package voting_pov.service.handler.twostep;

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
import voting_pov.utility.Utils;
import voting_pov.message.twostep.voting.Acknowledgement;
import voting_pov.message.twostep.voting.Request;
import voting_pov.service.Config;


/**
 *
 * @author Chienweichih
 */
public class VotingHandler implements ConnectionHandler {
    private static final ReentrantLock LOCK;
    
    private final Socket socket;
    private final KeyPair keyPair;
    
    static {
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
            
            String result = "failed";
            
            Operation op = req.getOperation();
            
            File file = new File("");
            
            switch (op.getType()) {
                case UPLOAD:
                    //merkleTreeOld = new MerkleTree(merkleTreeNew);
                    
                    file = new File(Config.DOWNLOADS_DIR_PATH + '/' + op.getPath());
                    
                    Utils.receive(in, file);

                    String digest = Utils.digest(file);

                    if (!op.getMessage().equals(digest)) {
                        break;
                    }
                    
                    //merkleTreeNew.update(file, op.getPath());
                    //result = merkleTreeNew.getRootHash();
                    break;
                case AUDIT:
                    result = ""; //merkleTreeNew.getRootHash()             
                    break;
                case DOWNLOAD:
                    //server do nothing
                    if (op.getPath().equals("")) {
                        socket.close();
                        return;
                    }
                    if (op.getMessage().equals("") || op.getMessage().equals(result)) {
                        file = new File(Config.DATA_DIR_PATH + '/' + op.getPath());
                        result = Utils.digest(file);
                    }
                    break;
                default:
                    result = "operation type mismatch";
            }
            
            Acknowledgement ack = new Acknowledgement(result, req, " ");
            
            ack.sign(keyPair);
            
            Utils.send(out, ack.toString());
                     
            switch (op.getType()) {
                case AUDIT:
                    //Utils.send(out, merkleTreeOld);
                break;
                case DOWNLOAD:
                    if (op.getMessage().equals(result)) {
                        Utils.send(out, file);
                    }
                break;
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
