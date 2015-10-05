package wei_shian_pov.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyPair;
import java.security.SignatureException;
import java.util.logging.Level;
import java.util.logging.Logger;

import client.Client;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.security.PublicKey;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.TimeUnit;
import message.Operation;
import message.OperationType;
import utility.Utils;
import voting_pov.utility.MerkleTree;
import wei_shian_pov.message.twostep.voting.*;
import wei_shian_pov.service.Config;

/**
 *
 * @author Chienweichih
 */
public class WeiShianClient extends Client {
    private static final Logger LOGGER;
    private static final Operation DOWNLOAD;
    private static final Operation UPLOAD;
    
    private String lastChainHash;
    private final MerkleTree merkleTree;
    
    static {
        LOGGER = Logger.getLogger(WeiShianClient.class.getName());
        DOWNLOAD = new Operation(OperationType.DOWNLOAD, Config.EMPTY_STRING, Config.EMPTY_STRING);
        UPLOAD = new Operation(OperationType.UPLOAD, Config.EMPTY_STRING, Config.EMPTY_STRING);
    }
    
    public WeiShianClient(KeyPair keyPair, KeyPair spKeyPair) {
        super(Config.SERVICE_HOSTNAME,
              Config.WEI_SHIAN_SERVICE_PORT,
              keyPair,
              spKeyPair,
              Config.NUM_PROCESSORS);
        
        lastChainHash = Utils.digest(Config.DEFAULT_CHAINHASH);
        merkleTree = new MerkleTree(new File(Config.DATA_DIR_PATH));
    }
        
    private boolean syncAtts(Operation op, DataOutputStream out, DataInputStream in) {
        boolean success = true;

        Request req = new Request(op);

        req.sign(keyPair);

        Utils.send(out, req.toString());
            
        if (op == UPLOAD) {
            Utils.send(out, this.merkleTree.getRootHash());
            Utils.send(out, this.lastChainHash);
        }
        
        String roothash = Utils.receive(in);
        
        switch (roothash) {
            case Config.EMPTY_STRING:
                return true;
            case Config.OP_TYPE_MISMATCH:
                return false;
        }
        
        String lastCH = Utils.receive(in);
        
        if (!lastCH.equals(this.lastChainHash)) {
            File spFile = new File(Config.DOWNLOADS_DIR_PATH + File.separator + getHandlerAttestationPath());
            
            downloadAtts(spFile);
            
            success &= updateAtts(spFile);
        }
        
        if (!roothash.equals(this.merkleTree.getRootHash())) {
            System.err.println("Sync Error");
        }
       
        return success;
    }
    
    private void downloadAtts(File spFile) {
        try (Socket socket = new Socket(hostname, port);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {
            Request req = new Request(new Operation(OperationType.AUDIT, Config.EMPTY_STRING, this.lastChainHash));
            
            req.sign(keyPair);
        
            Utils.send(out, req.toString());

            Acknowledgement ack = Acknowledgement.parse(Utils.receive(in));

            if (!ack.validate(spKeyPair.getPublic())) {
                throw new SignatureException("ACK validation failure");
            }
            
            Utils.receive(in, spFile);

            String digest = Utils.digest(spFile);

            if (!ack.getFileHash().equals(digest)) {
                System.err.println(Config.DOWNLOAD_FAIL);
            }

            socket.close();
        } catch (IOException | SignatureException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }
    
    private boolean updateAtts(File spFile) {
        boolean success = true;
        PublicKey spKey = spKeyPair.getPublic();
        PublicKey cliKey = keyPair.getPublic();
        
        try (FileInputStream fin = new FileInputStream(spFile);
             ObjectInputStream ois = new ObjectInputStream(fin)) {
            LinkedList<String> tempCH = (LinkedList<String>) ois.readObject();
            ois.close();
            
            ListIterator li = tempCH.listIterator();
            while (success && li.hasNext()) {
                Acknowledgement ack = Acknowledgement.parse((String) li.next());
                Request req = ack.getRequest();
                Operation op = req.getOperation();
                
                // check if chain hash linked
                if (lastChainHash.equals(ack.getChainHash())) {
                    lastChainHash = Utils.digest(ack.toString());
                } else {
                    success = false;
                }

                // update merkleTree if op is upload
                if (op.getType() == OperationType.UPLOAD) {
                    this.merkleTree.update(op.getPath(), op.getMessage());
                }
                
                success &= ack.validate(spKey) & req.validate(cliKey);
            }
        } catch (IOException | ClassNotFoundException ex) {
            success = false;
            
            LOGGER.log(Level.SEVERE, null, ex);
        }

        return success;
    }
    
    @Override
    protected void hook(Operation op, Socket socket, DataOutputStream out, DataInputStream in) 
            throws SignatureException, IllegalAccessException {
            Request req = new Request(op);

            req.sign(keyPair);

            Utils.send(out, req.toString());

            if (op.getType() == OperationType.UPLOAD) {
                Utils.send(out, new File(Config.DATA_DIR_PATH + File.separator + op.getPath()));
            }

            Acknowledgement ack = Acknowledgement.parse(Utils.receive(in));

            if (!ack.validate(spKeyPair.getPublic())) {
                throw new SignatureException("ACK validation failure");
            }

            String roothash = ack.getRoothash();
            String fileHash = ack.getFileHash();
            String chainHash = ack.getChainHash();
            
            if (!chainHash.equals(lastChainHash)) {
                System.out.println(chainHash);
                System.out.println(lastChainHash);
                throw new IllegalAccessException("Chain hash mismatch");
            }
            
            lastChainHash = Utils.digest(ack.toString());
            
            switch (op.getType()) {
                case UPLOAD:
                    merkleTree.update(op.getPath(), fileHash);
                    if (!roothash.equals(merkleTree.getRootHash())) {
                        System.err.println(Config.UPLOAD_FAIL);
                    }
                    
                    break;
                case DOWNLOAD:
                    String fname = Config.DOWNLOADS_DIR_PATH + File.separator + op.getPath();
                    
                    File file = new File(fname);

                    Utils.receive(in, file);

                    String digest = Utils.digest(file);

                    if (!fileHash.equals(digest)) {
                        System.err.println(Config.DOWNLOAD_FAIL);
                    }
                    
                    break;
                default:
                    System.err.println(Config.OP_TYPE_MISMATCH);
            }
    }
    
    @Override
    public void run(final List<Operation> operations, int runTimes) {
        System.out.println("Running:");
        
        long time = System.currentTimeMillis();
        for (int i = 1; i <= runTimes; i++) {
            final int x = i;
            pool.execute(() -> {
                try (Socket syncSocket = new Socket(Config.SYNC_HOSTNAME, Config.WEI_SHIAN_SYNC_PORT);
                     DataOutputStream syncOut = new DataOutputStream(syncSocket.getOutputStream());
                     DataInputStream SyncIn = new DataInputStream(syncSocket.getInputStream())) {

                    boolean success = syncAtts(DOWNLOAD, syncOut, SyncIn);
                    if (!success) {
                        System.err.println("Sync Error");
                    }

                    execute(operations.get(x % operations.size()));
                    
                    long start = System.currentTimeMillis();
                    success = syncAtts(UPLOAD, syncOut, SyncIn);
                    if (!success) {
                        System.err.println("Sync Error");
                    }
                    this.attestationCollectTime += System.currentTimeMillis() - start;

                    syncSocket.close();
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                }
            });
        }
        
        pool.shutdown();
        try {
            pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        time = System.currentTimeMillis() - time;
        
        System.out.println(runTimes + " times cost " + time + "ms");
        
        System.out.println("Auditing:");
        
        File auditFile = new File(Config.DOWNLOADS_DIR_PATH + File.separator + getHandlerAttestationPath());
        
        time = System.currentTimeMillis();
        boolean audit = audit(auditFile);
        time = System.currentTimeMillis() - time;
        
        System.out.println("Audit: " + audit + ", cost " + time + "ms");
    }

    @Override
    public String getHandlerAttestationPath() {
        return "WeiShianUpdate";
    }

    @Override
    public boolean audit(File spFile) {
        boolean success = true;
        try (Socket syncSocket = new Socket(Config.SYNC_HOSTNAME, Config.WEI_SHIAN_SYNC_PORT);
             DataOutputStream syncOut = new DataOutputStream(syncSocket.getOutputStream());
             DataInputStream SyncIn = new DataInputStream(syncSocket.getInputStream())) {
            
            //lastChainHash = Utils.digest(Config.DEFAULT_CHAINHASH);
            success &= syncAtts(DOWNLOAD, syncOut, SyncIn);
            success &= syncAtts(UPLOAD, syncOut, SyncIn);
            
            syncSocket.close();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        return success;
    }
}
