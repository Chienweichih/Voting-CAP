package wei_shian.service;

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
import wei_chih.utility.Utils;
import wei_chih.utility.MerkleTree;
import wei_shian.message.twostep.voting.Request;

/**
 *
 * @author chienweichih
 */
public class SyncServer implements ConnectionHandler {
    private static final ReentrantLock LOCK;
    
    private static String roothash;
    private static String lastAck;
        
    private final Socket socket;
    
    static {
        LOCK = new ReentrantLock();
        
        roothash = new MerkleTree(new File(SocketServer.dataDirPath)).getRootHash();
        lastAck = Utils.digest(Config.DEFAULT_CHAINHASH);
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
                return;
            }
            
            Utils.send(out, roothash);
            Utils.send(out, lastAck);
            
            // wait until client finish
            
            req = Request.parse(Utils.receive(in));
            
            if (!req.validate(clientPubKey)) {
                throw new SignatureException("REQ validation failure");
            }
            
            if (req.getOperation().getType() != OperationType.UPLOAD) {
                return;
            }
            
            roothash = Utils.receive(in);
            lastAck = Utils.receive(in);
            
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
