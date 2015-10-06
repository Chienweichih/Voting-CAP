package wei_shian_pov.service;

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

import message.OperationType;
import service.handler.ConnectionHandler;
import utility.Utils;
import voting_pov.utility.MerkleTree;
import wei_shian_pov.message.twostep.voting.*;

/**
 *
 * @author chienweichih
 */
public class SyncServer implements ConnectionHandler {
    private static String roothash;
    private static String lastChainHash;
    private static final ReentrantLock LOCK;
        
    private final Socket socket;
    
    static {
        roothash = new MerkleTree(new File(Config.DATA_DIR_PATH)).getRootHash();
        lastChainHash = Utils.digest(Config.DEFAULT_CHAINHASH);
        LOCK = new ReentrantLock();
    }
    
    public SyncServer(Socket socket, KeyPair keyPair) {
        this.socket = socket;
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
            
            if (req.getOperation().getType() != OperationType.DOWNLOAD) {
                Utils.send(out, Config.OP_TYPE_MISMATCH);
                return;
            }
            
            Utils.send(out, roothash);
            Utils.send(out, lastChainHash);
            
            req = Request.parse(Utils.receive(in));
            
            if (!req.validate(clientPubKey)) {
                throw new SignatureException("REQ validation failure");
            }
            
            if (req.getOperation().getType() != OperationType.UPLOAD) {
                Utils.send(out, Config.OP_TYPE_MISMATCH);
                return;
            }
            
            roothash = Utils.receive(in);
            lastChainHash = Utils.receive(in);
            Utils.send(out, Config.EMPTY_STRING);
            
            socket.close();
        } catch (IOException | SignatureException ex) {
            Logger.getLogger(SyncServer.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (LOCK != null) {
                LOCK.unlock();
            }
        }
    }
}
