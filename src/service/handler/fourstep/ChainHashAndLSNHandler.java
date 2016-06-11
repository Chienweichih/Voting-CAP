package service.handler.fourstep;

import service.HashingChainTable;
import service.LSNTable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.net.Socket;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import message.Operation;
import message.fourstep.chainhash_lsn.*;
import service.Config;
import service.Key;
import service.KeyManager;
import service.handler.ConnectionHandler;
import utility.Utils;

/**
 *
 * @author Scott
 */
public class ChainHashAndLSNHandler extends ConnectionHandler {
    public static final File ATTESTATION;
    
    private static final HashingChainTable HASHING_CHAIN_TABLE;
    private static final LSNTable LSN_TABLE;
    private static final ReentrantLock LOCK;
    
    static {
        ATTESTATION = new File(Config.ATTESTATION_DIR_PATH + "/service-provider/chainhash-lsn");
        
        HASHING_CHAIN_TABLE = new HashingChainTable();
        LSN_TABLE = new LSNTable();
        LOCK = new ReentrantLock();
    }
    
    public ChainHashAndLSNHandler(Socket socket, KeyPair keyPair) {
        super(socket, keyPair);
    }

    @Override
    protected void handle(DataOutputStream out, DataInputStream in)
            throws SignatureException, IllegalAccessException {
        PublicKey clientPubKey = KeyManager.getInstance().getPublicKey(Key.CLIENT);
        Lock lock = null;
        
        try {
            Request req = Request.parse(Utils.receive(in));
            String result, clientID;
            
            LOCK.lock();
            try {
                if (!req.validate(clientPubKey)) {
                    throw new SignatureException("REQ validation failure");
                }

                clientID = req.getClientID();
                Integer lsn = req.getLocalSequenceNumber();

                if (!LSN_TABLE.isMatched(clientID, lsn)) {
                    result = "LSN mismatch";
                } else {
                    result = Utils.digest("result");
                }

                String lastChainHash = HASHING_CHAIN_TABLE.getLastChainHash(clientID);

                Response res = new Response(req, result, lastChainHash);

                res.sign(keyPair);

                Utils.send(out, res.toString());
                
                LSN_TABLE.increment(req.getClientID());
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
            
            HASHING_CHAIN_TABLE.chain(req.getClientID(), Utils.digest(ackStr));
            
            Utils.appendAndDigest(ATTESTATION, ackStr + '\n');
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }
}
