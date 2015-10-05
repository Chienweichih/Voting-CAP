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
import java.util.ListIterator;
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
    
    private static String lastChainHash;
    private static final MerkleTree merkleTree;
    
    static {
        LOGGER = Logger.getLogger(WeiShianClient.class.getName());
        DOWNLOAD = new Operation(OperationType.DOWNLOAD, Config.EMPTY_STRING, Config.EMPTY_STRING);
        UPLOAD = new Operation(OperationType.UPLOAD, Config.EMPTY_STRING, Config.EMPTY_STRING);
        
        lastChainHash = Utils.digest(Config.DEFAULT_CHAINHASH);
        merkleTree = new MerkleTree(new File(Config.DATA_DIR_PATH));
    }
    
    public WeiShianClient(KeyPair keyPair, KeyPair spKeyPair) {
        super(Config.SERVICE_HOSTNAME,
              Config.WEI_SHIAN_SERVICE_PORT,
              keyPair,
              spKeyPair,
              Config.NUM_PROCESSORS);
    }
        
    private boolean syncAtts(Operation op) {
        boolean success = true;
        try (Socket socket = new Socket(Config.SYNC_HOSTNAME, Config.WEI_SHIAN_SYNC_PORT);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {
            
            Request req = new Request(op);

            req.sign(keyPair);

            Utils.send(out, req.toString());
            
            if (op.getType() == OperationType.UPLOAD) {
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
                Operation auditOP = new Operation(OperationType.AUDIT, Config.EMPTY_STRING, Config.EMPTY_STRING);
                execute(auditOP);
                
                File updateFile = new File(Config.DOWNLOADS_DIR_PATH + File.separator + getHandlerAttestationPath());
                success &= updateAtts(updateFile);
            }
            
            if (!roothash.equals(this.merkleTree.getRootHash())) {
                System.err.println("Sync Error");
            }

            socket.close();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        return success;
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
                
                if (lastChainHash.equals(ack.getChainHash())) {
                    lastChainHash = Utils.digest(ack.toString());
                } else {
                    success = false;
                }

                if (req.getOperation().getType() == OperationType.UPLOAD) {
                    Operation op = req.getOperation();
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
//        if (op.getType() == OperationType.AUDIT) {
//            op = new Operation(op.getType(), op.getPath(), this.lastChainHash);
//        } else {
//            boolean success = syncAtts(DOWNLOAD);
//            if (!success) {
//                System.err.println("Sync Error");
//            }
//        }
        
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
        String fname = "";

        if (!chainHash.equals(lastChainHash)) {
            System.out.println(chainHash);
            System.out.println(lastChainHash);
            throw new IllegalAccessException("Chain hash mismatch");
        }

        if (op.getType() != OperationType.AUDIT) { // dirty fix
            lastChainHash = Utils.digest(ack.toString());
        }
        
        switch (op.getType()) {
            case UPLOAD:
                merkleTree.update(op.getPath(), fileHash);
                if (!roothash.equals(merkleTree.getRootHash())) {
                    System.err.println(Config.UPLOAD_FAIL);
                }
                break;
            case DOWNLOAD:
                fname = "-" + System.currentTimeMillis();
            case AUDIT:
                fname = String.format("%s%s%s%s",
                            Config.DOWNLOADS_DIR_PATH,
                            File.separator,
                            op.getPath(),
                            fname);

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

        long start = System.currentTimeMillis();
//        if (op.getType() != OperationType.AUDIT) {
//            boolean success = syncAtts(UPLOAD);
//            if (!success) {
//                System.err.println("Sync Error");
//            }
//        }
        this.attestationCollectTime += System.currentTimeMillis() - start;
    }

    @Override
    public String getHandlerAttestationPath() {
        return "WeiShianUpdate";
    }

    @Override
    public boolean audit(File spFile) {
        boolean success = true;
//        success &= syncAtts(DOWNLOAD);
//        success &= syncAtts(UPLOAD);
        return success;
    }
}
