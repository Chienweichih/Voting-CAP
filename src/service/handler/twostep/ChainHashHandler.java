package service.handler.twostep;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import message.Operation;
import message.twostep.chainhash.*;
import service.Config;
import service.handler.ConnectionHandler;
import utility.Utils;

/**
 *
 * @author Scott
 */
public class ChainHashHandler implements ConnectionHandler {
    public static final File ATTESTATION;
    
    private static final LinkedList<String> HashingChain;
    private final Socket socket;
    private final KeyPair keyPair;
    
    static {
        ATTESTATION = new File("attestation/service-provider/chainhash");
        
        HashingChain = new LinkedList<>();
        HashingChain.add(Config.DEFAULT_CHAINHASH);
    }
    
    public ChainHashHandler(Socket socket, KeyPair keyPair) {
        this.socket = socket;
        this.keyPair = keyPair;
    }
    
    @Override
    public void run() {
        PublicKey clientPubKey = Utils.readKeyPair("client.key").getPublic();
        
        try (DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {
            Request req = Request.parse(Utils.receive(in));
            
            if (!req.validate(clientPubKey)) {
                throw new SignatureException("REQ validation failure");
            }
            
            String result;
            
            Operation op = req.getOperation();
            
            File file = new File(Config.DATA_DIR_PATH + '/' + op.getPath());
            boolean sendFileAfterAck = false;
            
            switch (op.getType()) {
                case UPLOAD:
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
            
            socket.close();
        } catch (IOException | SignatureException ex) {
            Logger.getLogger(CSNHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
