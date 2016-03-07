package wei_shian.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.security.PublicKey;
import java.security.KeyPair;
import java.security.SignatureException;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import client.Client;
import message.Operation;
import message.OperationType;
import wei_chih.utility.MerkleTree;
import wei_chih.utility.Utils;
import wei_shian.message.twostep.voting.*;
import wei_shian.service.Config;

/**
 *
 * @author Chienweichih
 */
public class WeiShianClient extends Client {
    private static final Logger LOGGER;
    
    private static final Operation DOWNLOAD;
    private static final Operation UPLOAD;
    
    static {
        LOGGER = Logger.getLogger(WeiShianClient.class.getName());
        
        DOWNLOAD = new Operation(OperationType.DOWNLOAD, Config.EMPTY_STRING, Config.EMPTY_STRING);
        UPLOAD = new Operation(OperationType.UPLOAD, Config.EMPTY_STRING, Config.EMPTY_STRING);
    }
    
    private String lastChainHash;
    private final MerkleTree merkleTree;
    
    public WeiShianClient(KeyPair keyPair, KeyPair spKeyPair) {
        super(Config.SERVICE_HOSTNAME,
              Config.WEI_SHIAN_SERVICE_PORT,
              keyPair,
              spKeyPair,
              Config.NUM_PROCESSORS);
        
        lastChainHash = Utils.digest(Config.DEFAULT_CHAINHASH);
        merkleTree = new MerkleTree(new File(Experiment.dataDirPath));
    }
    
    @Override
    public void run(final List<Operation> operations, int runTimes) {
        System.out.println("Running:");
        
        List<Double> results = new ArrayList<>(); 
        
        for (int i = 1; i <= runTimes; i++) {
            final int x = i;
            pool.execute(() -> {
                long time = System.currentTimeMillis();
                try (Socket syncSocket = new Socket(Config.SYNC_HOSTNAME, Config.WEI_SHIAN_SYNC_PORT);
                     DataOutputStream syncOut = new DataOutputStream(syncSocket.getOutputStream());
                     DataInputStream SyncIn = new DataInputStream(syncSocket.getInputStream())) {

                    boolean syncSuccess = syncAtts(DOWNLOAD, syncOut, SyncIn);
                    if (!syncSuccess) {
                        System.err.println("Sync Error");
                    }

                    ///////////////////////////////////////////////////////////////////////////////////////////////////
                    
                    execute(operations.get(x % operations.size()));
                    
                    ///////////////////////////////////////////////////////////////////////////////////////////////////
                    
                    syncSuccess = syncAtts(UPLOAD, syncOut, SyncIn);
                    if (!syncSuccess) {
                        System.err.println("Sync Error");
                    }
                    
                    syncSocket.close();
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                } finally {
                    results.add((System.currentTimeMillis() - time) / 1000.0);
                }
            });
        }
        
        pool.shutdown();
        try {
            pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        
        Collections.sort(results);
        
        if (runTimes < 10) {
            for (double time : results) {
                System.out.printf("%.5f s\n", time);
            }
        } else {
            for (int i = 0; i < 5; ++i) {
                System.out.printf("%.5f s\n", results.get(i));
            }
            
            System.out.println(".");
            System.out.println(".");
            System.out.println(".");
            
            for (int i = 5; i > 0; --i) {
                System.out.printf("%.5f s\n", results.get(results.size() - i));
            }
        }
        
        System.out.println("Auditing:");
        
        long time = System.currentTimeMillis();
        boolean audit = audit();
        System.out.println("Audit: " + audit + ", cost " + ((System.currentTimeMillis() - time) / 1000.0) + " s");
    }
    
    @Override
    protected void hook(Operation op, Socket socket, DataOutputStream out, DataInputStream in) 
            throws SignatureException, IllegalAccessException {
        Request req = new Request(op);
        req.sign(keyPair);
        Utils.send(out, req.toString());

        if (op.getType() == OperationType.UPLOAD) {
            Utils.send(out, new File(Experiment.dataDirPath + op.getPath()));
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
                File file = new File(Config.DOWNLOADS_DIR_PATH + op.getPath());

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
    public String getHandlerAttestationPath() {
        return "WeiShianUpdate";
    }

    @Override
    public boolean audit(File spFile) {
        return audit();
    }
    
    public boolean audit() {
        boolean success = true;
        try (Socket syncSocket = new Socket(Config.SYNC_HOSTNAME, Config.WEI_SHIAN_SYNC_PORT);
             DataOutputStream syncOut = new DataOutputStream(syncSocket.getOutputStream());
             DataInputStream SyncIn = new DataInputStream(syncSocket.getInputStream())) {
            
            lastChainHash = Utils.digest(Config.DEFAULT_CHAINHASH);
            success &= syncAtts(DOWNLOAD, syncOut, SyncIn);
            success &= syncAtts(UPLOAD, syncOut, SyncIn);
            
            syncSocket.close();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        return success;
    }
    
    private boolean syncAtts(Operation op, DataOutputStream out, DataInputStream in) {
        boolean success = true;

        Request req = new Request(op);
        req.sign(keyPair);
        Utils.send(out, req.toString());
        
        switch (op.getType()){
            case DOWNLOAD:
                String roothash = Utils.receive(in);
                String lastCH = Utils.receive(in);
                
                if (!lastCH.equals(this.lastChainHash)) {
                    File spFile = new File(Config.DOWNLOADS_DIR_PATH + "/" + getHandlerAttestationPath());

                    downloadAtts(spFile);

                    success &= updateAtts(spFile);
                }

                if (!roothash.equals(this.merkleTree.getRootHash())) {
                    System.err.println("Sync Error");
                }
                break;
            case UPLOAD:
                Utils.send(out, this.merkleTree.getRootHash());
                Utils.send(out, this.lastChainHash);
                break;
            default:
                return false;
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
        
        LinkedList<String> tempCH = Utils.Deserialize(spFile.getAbsolutePath());

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
                break;
            }

            // update merkleTree if op is upload
            if (op.getType() == OperationType.UPLOAD) {
                this.merkleTree.update(op.getPath(), op.getMessage());
            }

            success &= ack.validate(spKey) & req.validate(cliKey);
        }
        
        return success;
    }
}
