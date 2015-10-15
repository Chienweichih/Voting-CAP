package real_time_pov.wei_chih.service.handler.twostep;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import message.Operation;
import service.handler.ConnectionHandler;
import real_time_pov.wei_chih.message.twostep.voting.*;
import real_time_pov.wei_chih.service.Config;
import real_time_pov.wei_chih.utility.*;

/**
 *
 * @author Chienweichih
 */
public class VotingHandler implements ConnectionHandler {
    private static final MerkleTree merkleTree;
    private static String digestBeforeUpdate;
    private static Operation lastOP;
    private static final ReentrantLock LOCK;
    
    private final Socket socket;
    private final KeyPair keyPair;
    
    static {
        merkleTree = new MerkleTree(new File(Config.DATA_DIR_PATH));
        digestBeforeUpdate = "";
        lastOP = null;
        LOCK = new ReentrantLock();
    }
    
    public VotingHandler(Socket socket, KeyPair keyPair) {
        this.socket = socket;
        this.keyPair = keyPair;
    }
    
    @Override
    public void run() {
        PublicKey clientPubKey = service.KeyPair.CLIENT.getKeypair().getPublic();
        
        try (DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {
            Request req = Request.parse(Utils.receive(in));
            
            LOCK.lock();
            
            if (!req.validate(clientPubKey)) {
                throw new SignatureException("REQ validation failure");
            }
            
            String result = merkleTree.getRootHash();
            
            Operation op = req.getOperation();
            
            File file = null;
            boolean sendFileAfterAck = false;
            boolean updateLastOP = false;
            
            switch (op.getType()) {
                case DOWNLOAD:
                    file = new File(Config.DATA_DIR_PATH + op.getPath());
                    
                    if (op.getMessage().equals(Config.EMPTY_STRING)) {
                       updateLastOP = true;
                       digestBeforeUpdate = merkleTree.getDigest(op.getPath());
                       break;
                    }
                    
                    sendFileAfterAck = op.getMessage().equals(result);
                    result = Utils.digest(file);
                    if (!sendFileAfterAck) {
                        result = Config.DOWNLOAD_FAIL;
                    }
                    
                    break;
                case UPLOAD:
                    file = new File(Config.DOWNLOADS_DIR_PATH + op.getPath());
                    
                    Utils.receive(in, file);

                    String digest = Utils.digest(file);

                    if (op.getMessage().equals(digest)) {
                        // write file
                        digestBeforeUpdate = merkleTree.getDigest(op.getPath());
                        merkleTree.update(op.getPath(), digest);
                        result = merkleTree.getRootHash();
                        updateLastOP = true;
                    } else {
                        result = Config.UPLOAD_FAIL;
                    }
                    
                    break;
                case AUDIT:
                    file = new File(Config.ATTESTATION_DIR_PATH + File.separator + "service-provider" + File.separator + "voting");
                    
                    if (lastOP == null) {
                        result = Config.AUDIT_FAIL;
                        break;
                    }
                                        
                    MerkleTree prevMerkleTree = new MerkleTree(merkleTree);
                    prevMerkleTree.update(lastOP.getPath(), digestBeforeUpdate);
                    
                    switch (lastOP.getType()) {
                        case DOWNLOAD:
                            Utils.write(file, prevMerkleTree.getRootHash());
                            break;
                        case UPLOAD:
                            Utils.Serialize(file, prevMerkleTree);
                            break;
                        default:
                            System.err.println(Config.AUDIT_FAIL);
                    }
                    
                    result = Utils.digest(file);
                    
                    sendFileAfterAck = true;
                    
                    break;
                default:
                    result = Config.OP_TYPE_MISMATCH;
            }
            
            Acknowledgement ack = new Acknowledgement(result, req);
            
            ack.sign(keyPair);
            
            Utils.send(out, ack.toString());
            
            if (sendFileAfterAck) {
                Utils.send(out, file);
            }
            
            if (updateLastOP) {
                lastOP = req.getOperation();
            }
            
            socket.close();
        } catch (IOException | SignatureException ex) {
            Logger.getLogger(VotingHandler.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (LOCK != null) {
                LOCK.unlock();
            }
        }
    }
}
