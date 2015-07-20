package service.handler.fourstep;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import message.Operation;
import message.fourstep.doublechainhash.*;
import service.Config;
import service.handler.ConnectionHandler;
import utility.Utils;

/**
 *
 * @author Scott
 */
public class DoubleChainHashHandler implements ConnectionHandler {
    public static final File ATTESTATION;
    
    private static final DoubleHashingChainTable HASHING_CHAIN_TABLE;
    private static final ReentrantLock LOCK;
    
    private final Socket socket;
    private final KeyPair keyPair;
    
    static {
        ATTESTATION = new File(Config.ATTESTATION_DIR_PATH + "/service-provider/doublechainhash");
        
        HASHING_CHAIN_TABLE = new DoubleHashingChainTable();
        LOCK = new ReentrantLock();
    }
    
    public DoubleChainHashHandler(Socket socket, KeyPair keyPair) {
        this.socket = socket;
        this.keyPair = keyPair;
    }
    
    @Override
    public void run() {
        PublicKey clientPubKey = service.KeyPair.CLIENT.getKeypair().getPublic();
        Lock lock = null;
        
        try (DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {
            Request req = Request.parse(Utils.receive(in));
            String result, clientID;
            
            LOCK.lock();
            try {
                if (!req.validate(clientPubKey)) {
                    throw new SignatureException("REQ validation failure");
                }

                clientID = req.getClientID();

                String lastChainHash = HASHING_CHAIN_TABLE.getLastChainHash(clientID);
                String lastChainHashOfAll = HASHING_CHAIN_TABLE.getLastChainHashOfAll();

                Response res = new Response(req, lastChainHash, lastChainHashOfAll);

                res.sign(keyPair);

                Utils.send(out, res.toString());

                HASHING_CHAIN_TABLE.chain(Utils.digest(res.toString()));
            } finally {
                LOCK.unlock();
            }
            
            ReplyResponse rr = ReplyResponse.parse(Utils.receive(in));
            
            if (!rr.validate(clientPubKey)) {
                throw new SignatureException("RR validation failure");
            }
            
            Operation op = req.getOperation();

            File file = new File(Config.DATA_DIR_PATH + '/' + op.getPath());
            ReentrantReadWriteLock rwl = service.File.parse(op.getPath()).getLock();
            
            switch (op.getType()) {
                case UPLOAD:
                case AUDIT:
                    lock = rwl.writeLock();
                    lock.lock();
                    
                    break;
                case DOWNLOAD:
                    lock = rwl.readLock();
                    lock.lock();
                    
                    break;
            }
            
            boolean sendFileAfterAck = false;
            
            switch (op.getType()) {
                case UPLOAD:
                    file = new File(Config.DOWNLOADS_DIR_PATH + '/' + op.getPath());
                    
                    Utils.receive(in, file);

                    String digest = Utils.digest(file);

                    if (op.getMessage().compareTo(digest) == 0) {
                        result = "ok";
                    } else {
                        result = "upload fail";
                    }
                    
                    Utils.writeDigest(file.getPath(), digest);

                    break;
                case AUDIT:
                    file = new File(op.getPath());
                    
                    result = Utils.readDigest(file.getPath());
                    
                    sendFileAfterAck = true;
                    
                    break;
                case DOWNLOAD:
                    result = Utils.readDigest(file.getPath());
                    
                    sendFileAfterAck = true;

                    break;
                default:
                    result = "operation type mismatch";
            }
            
            Acknowledgement ack = new Acknowledgement(result, rr);
            
            ack.sign(keyPair);
            
            String ackStr = ack.toString();
            
            Utils.send(out, ackStr);
            
            if (sendFileAfterAck) {
                Utils.send(out, file);
            }
            
            HASHING_CHAIN_TABLE.chain(clientID, Utils.digest(ackStr));
            
            Utils.appendAndDigest(ATTESTATION, ackStr + '\n');
            
            socket.close();
        } catch (IOException | SignatureException ex) {
            Logger.getLogger(DoubleChainHashHandler.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }
}
