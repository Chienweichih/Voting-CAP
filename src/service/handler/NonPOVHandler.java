package service.handler;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.logging.Level;
import java.util.logging.Logger;

import message.Operation;
import service.Config;
import message.nonpov.*;
import utility.Utils;

/**
 *
 * @author Scott
 */
public class NonPOVHandler implements ConnectionHandler {
    public static final File REQ_ATTESTATION;
    public static final File ACK_ATTESTATION;
    
    static {
        REQ_ATTESTATION = new File("attestation/service-provider/nonpov.req");
        ACK_ATTESTATION = new File("attestation/service-provider/nonpov.ack");
    }
    
    private final Socket socket;
    private final KeyPair keyPair;
    
    public NonPOVHandler(Socket socket, KeyPair keyPair) {
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
            
            File file;
            boolean sendFileAfterAck = false;
            
            Operation op = req.getOperation();

            file = new File(op.getPath());

            String fname = Config.DATA_DIR_PATH + "/" + file.getName();

            switch (op.getType()) {
                case UPLOAD:
                    Utils.receive(in, file);

                    String digest = Utils.digest(file, Config.DIGEST_ALGORITHM);

                    if (op.getMessage().compareTo(digest) == 0) {
                        result = "ok";
                    } else {
                        result = "upload fail";
                    }
                    
                    Utils.writeDigest(fname, digest);

                    break;
                case DOWNLOAD:
                    result = Utils.readDigest(fname);
                    
                    sendFileAfterAck = true;
                    
                    break;
                case AUDIT:
                    result = Utils.readDigest(REQ_ATTESTATION.getPath());
                    result += Utils.readDigest(ACK_ATTESTATION.getPath());
                    
                    sendFileAfterAck = true;
                    
                    break;
                default:
                    result = "operation type mismatch";
            }
            
            Acknowledgement ack = new Acknowledgement(result);
            
            ack.sign(keyPair);
            
            Utils.send(out, ack.toString());
            
            if (sendFileAfterAck) {
                switch (op.getType()) {
                    case DOWNLOAD:
                        Utils.send(out, file);
                        
                        break;
                    case AUDIT:
                        Utils.send(out, REQ_ATTESTATION);
                        Utils.send(out, ACK_ATTESTATION);
                        
                        break;
                }
            }
            
            Utils.appendAndDigest(REQ_ATTESTATION, req.toString() + '\n');
            Utils.appendAndDigest(ACK_ATTESTATION, ack.toString() + '\n');
            
            socket.close();
        } catch (IOException | SignatureException ex) {
            Logger.getLogger(NonPOVHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
