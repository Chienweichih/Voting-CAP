package service.handler.twostep;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import message.Operation;

import message.twostep.csn.*;
import service.Config;
import service.handler.ConnectionHandler;
import utility.Utils;

/**
 *
 * @author Scott
 */
public class CSNHandler implements ConnectionHandler {
    private static final AtomicInteger CSN;
    private final Socket socket;
    private final KeyPair keyPair;
    
    static {
        CSN = new AtomicInteger(0);
    }
    
    public CSNHandler(Socket socket, KeyPair keyPair) {
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
            
            File file = null;
            boolean sendFileAfterAck = false;
            
            if (req.getConsecutiveSequenceNumber() == CSN.get() + 1) {
                CSN.incrementAndGet();
                
                Operation op = req.getOperation();
                
                file = new File(op.getPath());
                
                String fname = Config.DATA_DIR_PATH + "/" + file.getName() + ".digest";
                
                String digest;
                
                switch (op.getType()) {
                    case UPLOAD:
                        Utils.receive(in, file);
                        
                        digest = Utils.digest(file);
                        
                        if (op.getMessage().compareTo(digest) == 0) {
                            result = "ok";
                        } else {
                            result = "upload fail";
                        }
                        
                        try (FileWriter fw = new FileWriter(fname)) {
                            fw.write(digest);
                        }
                        
                        break;
                    case DOWNLOAD:
                        try (FileReader fr = new FileReader(fname);
                             BufferedReader br = new BufferedReader(fr)) {
                            digest = br.readLine();
                        }
                        
                        result = digest;
                        sendFileAfterAck = true;
                        
                        break;
                    default:
                        result = "operation type mismatch";
                }
            } else {
                result = "CSN mismatch";
            }
            
            Acknowledgement ack = new Acknowledgement(result, req);
            
            ack.sign(keyPair);
            
            Utils.send(out, ack.toString());
            
            if (sendFileAfterAck) {
                Utils.send(out, file);
            }
            
            File attestation = new File("attestation/service-provider/csn");
            
            try (FileWriter fw = new FileWriter(attestation, true)) {
                fw.append(ack.toString() + '\n');
            }
            
            socket.close();
        } catch (IOException ex) {
            Logger.getLogger(CSNHandler.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SignatureException ex) {
            Logger.getLogger(CSNHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
