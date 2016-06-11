package service.handler.twostep;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.net.Socket;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;

import message.Operation;
import message.twostep.chainhash.*;
import service.Config;
import service.Key;
import service.KeyManager;
import service.handler.ConnectionHandler;
import utility.Utils;

/**
 *
 * @author Scott
 */
public class ChainHashHandler extends ConnectionHandler {
    public static final File ATTESTATION;
    
    private static final LinkedList<String> HashingChain;
    private static final ReentrantLock LOCK;
    
    static {
        ATTESTATION = new File(Config.ATTESTATION_DIR_PATH + "/service-provider/chainhash");
        
        HashingChain = new LinkedList<>();
        HashingChain.add(Config.INITIAL_HASH);
        
        LOCK = new ReentrantLock();
    }
    
    public ChainHashHandler(Socket socket, KeyPair keyPair) {
        super(socket, keyPair);
    }
    
    @Override
    protected void handle(DataOutputStream out, DataInputStream in)
            throws SignatureException, IllegalAccessException {
        PublicKey clientPubKey = KeyManager.getInstance().getPublicKey(Key.CLIENT);
        
        try {
            Request req = Request.parse(Utils.receive(in));
            
            LOCK.lock();
            
            if (!req.validate(clientPubKey)) {
                throw new SignatureException("REQ validation failure");
            }
            
            String result;
            
            Operation op = req.getOperation();
            
            File file = new File(Config.DATA_DIR_PATH + '/' + op.getPath());
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
            
            Acknowledgement ack = new Acknowledgement(result, req, HashingChain.getLast());
            
            ack.sign(keyPair);
            
            Utils.send(out, ack.toString());
            
            HashingChain.add(Utils.digest(ack.toString()));
            
            if (sendFileAfterAck) {
                Utils.send(out, file);
            }
            
            Utils.appendAndDigest(ATTESTATION, ack.toString() + '\n');
        } finally {
            if (LOCK != null) {
                LOCK.unlock();
            }
        }
    }
}
