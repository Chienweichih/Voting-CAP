package wei_chih.service.handler;

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
import service.Key;
import service.KeyManager;
import service.handler.ConnectionHandler;
import wei_chih.message.weishian.Request;
import wei_chih.service.Config;
import wei_chih.service.SocketServer;
import wei_chih.utility.Utils;
import wei_chih.utility.MerkleTree;

/**
 *
 * @author chienweichih
 */
public class WeiShianSyncHandler extends ConnectionHandler {
    private static final ReentrantLock LOCK;
    
    private static String roothash;
    private static String lastAck;

    static {
        LOCK = new ReentrantLock();
        
        roothash = new MerkleTree(new File(SocketServer.dataDirPath)).getRootHash();
        lastAck = Utils.digest(Config.INITIAL_HASH, Config.DIGEST_ALGORITHM);
    }
    
    public WeiShianSyncHandler(Socket socket, KeyPair keyPair) {
        super(socket, keyPair);
    }

    @Override
    protected void handle(DataOutputStream out, DataInputStream in) throws SignatureException, IllegalAccessException {
        PublicKey clientPubKey = KeyManager.getInstance().getPublicKey(Key.CLIENT);
        
        try {
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
            Logger.getLogger(WeiShianSyncHandler.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (LOCK != null) {
                LOCK.unlock();
            }
        }
    }
}
