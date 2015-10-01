package wei_shian_pov.client;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.logging.Level;
import java.util.logging.Logger;

import client.Client;
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
    
    static {
        LOGGER = Logger.getLogger(WeiShianClient.class.getName());
    }
    
    private String lastChainHash;
    private final MerkleTree merkleTree;
    
    public WeiShianClient(KeyPair keyPair, KeyPair spKeyPair) {
        super(Config.SERVICE_HOSTNAME,
              Config.WEI_SHIAN_SERVICE_PORT,
              keyPair,
              spKeyPair,
              Config.NUM_PROCESSORS);
        
        this.lastChainHash = Config.DEFAULT_CHAINHASH;
        this.merkleTree = new MerkleTree(new File(Config.DATA_DIR_PATH));
    }
        
    private boolean syncAttestation(Operation op) {
        boolean success = true;
        try (Socket socket = new Socket(Config.SYNC_HOSTNAME, Config.WEI_SHIAN_SYNC_PORT);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {
            
            Request req = new Request(op);

            req.sign(keyPair);

            Utils.send(out, req.toString());
            
            String roothash = Utils.receive(in);
            
            if ( roothash.equals(Config.EMPTY_STRING) ) {
                return false;
            }
            
            
            
            
            
            String lastCH = Utils.receive(in);
            
            

            socket.close();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        return success;
    }
    
    @Override
    protected void hook(Operation op, Socket socket, DataOutputStream out, DataInputStream in) 
            throws SignatureException, IllegalAccessException {
        if (op.getType() == OperationType.AUDIT) {
            return;
        }
        
        syncAttestation(new Operation(OperationType.DOWNLOAD,"",""));
        
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
                String fname = service.Config.DOWNLOADS_DIR_PATH + File.separator + op.getPath() + "-" + System.currentTimeMillis();

                File file = new File(fname);

                Utils.receive(in, file);

                String digest = Utils.digest(file);

                if (!(roothash.equals(merkleTree.getRootHash()) && 
                      fileHash.equals(digest))) {
                    System.err.println(Config.DOWNLOAD_FAIL);
                }

                break;
            default:
                System.err.println(Config.OP_TYPE_MISMATCH);
        }

        long start = System.currentTimeMillis();
        syncAttestation(new Operation(OperationType.UPLOAD,"",""));
        this.attestationCollectTime += System.currentTimeMillis() - start;
    }

    @Override
    public String getHandlerAttestationPath() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public boolean audit(File spFile) {
        return syncAttestation(new Operation(OperationType.DOWNLOAD,"",""));
        
        
        boolean success = true;
        PublicKey spKey = spKeyPair.getPublic();
        PublicKey cliKey = keyPair.getPublic();
        
        //! need to change, wei shian's different.
        try (FileReader fr = new FileReader(spFile);
             BufferedReader br = new BufferedReader(fr)) {
            String chainhash = service.Config.DEFAULT_CHAINHASH;
            
            do {
                String s = br.readLine();
                
                Acknowledgement ack = Acknowledgement.parse(s);
                Request req = ack.getRequest();
                
                if (chainhash.compareTo(ack.getChainHash()) == 0) {
                    chainhash = Utils.digest(ack.toString());
                } else {
                    success = false;
                }
                
                success &= ack.validate(spKey) & req.validate(cliKey);
            } while (success && chainhash.compareTo(lastChainHash) != 0);
        } catch (NullPointerException | IOException ex) {
            success = false;
            
            LOGGER.log(Level.SEVERE, null, ex);
        }
        
        return success;
    }
}
