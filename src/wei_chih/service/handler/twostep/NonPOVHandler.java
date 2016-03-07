package wei_chih.service.handler.twostep;

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
import message.OperationType;
import service.handler.ConnectionHandler;
import wei_chih.message.twostep.voting.Acknowledgement;
import wei_chih.message.twostep.voting.Request;
import wei_chih.service.Config;
import wei_chih.service.SocketServer;
import wei_chih.utility.Utils;

/**
 *
 * @author chienweichih
 */
public class NonPOVHandler implements ConnectionHandler {
    private static final ReentrantLock LOCK;
    
    private final Socket socket;
    private final KeyPair keyPair;
    
    static {
        LOCK = new ReentrantLock();
    }
    
    public NonPOVHandler(Socket socket, KeyPair keyPair) {
        this.socket = socket;
        this.keyPair = keyPair;
    }
    
    @Override
    public void run() {
        PublicKey clientPubKey = service.KeyPair.CLIENT.getKeypair().getPublic();
        
        try (DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {
            Request req = Request.parse(Utils.receive(in));
            Operation op = req.getOperation();
            
            LOCK.lock();
            
            if (!req.validate(clientPubKey)) {
                throw new SignatureException("REQ validation failure");
            }
            
            String result = Utils.digest(new File(SocketServer.dataDirPath + op.getPath()));
            
            if (op.getType() == OperationType.UPLOAD) {
                File file = new File(Config.DOWNLOADS_DIR_PATH + op.getPath());
                Utils.receive(in, file);
                result = Utils.digest(file);
            }
            
            Acknowledgement ack = new Acknowledgement(result, req);
            ack.sign(keyPair);
            Utils.send(out, ack.toString());
            
            if (op.getType() == OperationType.DOWNLOAD) {
                Utils.send(out, new File(SocketServer.dataDirPath + op.getPath()));
            }
            
            socket.close();
        } catch (IOException | SignatureException ex) {
            Logger.getLogger(NonPOVHandler.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (LOCK != null) {
                LOCK.unlock();
            }
        }
    }
}
