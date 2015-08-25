package voting_pov.service.handler.twostep;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
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
import voting_pov.utility.Utils;
import voting_pov.message.twostep.voting.*;
import voting_pov.service.Config;
import voting_pov.utility.MerkleTree_file;

/**
 *
 * @author Chienweichih
 */
public class VotingHandler implements ConnectionHandler {
    public static final String OLD_HASH_PATH;
    public static final String NEW_HASH_PATH;
    private static final String attestationPath;
    private static final ReentrantLock LOCK;
    private static Acknowledgement lastAck;
    private static Acknowledgement thisAck;
    
    private final Socket socket;
    private final KeyPair keyPair;
    
    static {
        attestationPath = Config.ATTESTATION_DIR_PATH + File.separator + "voting" +
                                                        File.separator + "service-provider";
        OLD_HASH_PATH = attestationPath + File.separator +"old" +
                                          File.separator + "data_HASH";
        NEW_HASH_PATH = attestationPath + File.separator +"new" +
                                          File.separator + "data_HASH";
        LOCK = new ReentrantLock();
        lastAck = null;
        thisAck = null;
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
            
            String result;
            
            Operation op = req.getOperation();
            
            File file = null;
            boolean sendFileAfterAck = false;
            boolean updateLastAck = false;
            
            switch (op.getType()) {
                case UPLOAD:
                    file = new File(Config.DOWNLOADS_DIR_PATH + op.getPath());
                    
                    Utils.receive(in, file);

                    String digest = Utils.digest(file);

                    if (op.getMessage().equals(digest)) {
                        // write file
                        MerkleTree_file.copy(NEW_HASH_PATH, OLD_HASH_PATH);
                        MerkleTree_file.update(NEW_HASH_PATH,
                                          NEW_HASH_PATH + op.getPath() + ".digest",
                                          digest);
                        result = Utils.readDigest(NEW_HASH_PATH);
                        updateLastAck = true;
                    } else {
                        result = Config.UPLOAD_FAIL;
                    }
                    
                    break;
                case AUDIT:
                    if (lastAck == null) {
                        result = Config.AUDIT_FAIL;
                        break;
                    }
                    
                    Request lastReq = lastAck.getRequest();
                    if (op.getPath().equals(Config.EMPTY_STRING)) {
                        result = Utils.readDigest(NEW_HASH_PATH);
                        req = lastReq;
                        break;
                    }
                    
                    switch (lastReq.getOperation().getType()) {
                        case DOWNLOAD:
                            file = new File(OLD_HASH_PATH + ".digest");
                            break;
                        case UPLOAD:
                            file = new File(attestationPath + File.separator + "voting");
                            File oldHash = new File(OLD_HASH_PATH);
                            if (!oldHash.exists()) {
                                throw new FileNotFoundException("Old Hash Not Found!"); 
                            }
                            Utils.zipDir(oldHash.getParentFile(), file);
                            break;
                        default:
                            System.err.println(Config.AUDIT_FAIL);
                    }
                    
                    result = Utils.digest(file);
                    
                    sendFileAfterAck = true;
                    
                    break;
                case DOWNLOAD:
                    file = new File(Config.DATA_DIR_PATH + op.getPath());
                    result = Utils.readDigest(NEW_HASH_PATH + op.getPath());
                    
                    if (!op.getMessage().equals(Config.EMPTY_STRING)) {
                        sendFileAfterAck = op.getMessage().equals(result);
                        if (!sendFileAfterAck) {
                            result = Config.DOWNLOAD_FAIL;
                        }
                    } else {
                        updateLastAck = true;
                    }
                    
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
            
            if (updateLastAck) {
                lastAck = thisAck;
                thisAck = ack;
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
