package client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.logging.Level;
import java.util.logging.Logger;

import message.Operation;
import message.OperationType;
import message.nonpov.*;
import service.Config;
import utility.Utils;

/**
 *
 * @author Scott
 */
public class NonPOVClient {
    private final String hostname;
    private final int port;
    private final KeyPair keyPair;
    
    public NonPOVClient(KeyPair keyPair) {
        this(Config.SERVICE_HOSTNAME, Config.NONPOV_SERVICE_PORT, keyPair);
    }
    
    public NonPOVClient(String hostname, int port, KeyPair keyPair) {
        this.hostname = hostname;
        this.port = port;
        this.keyPair = keyPair;
    }
    
    public void run(Operation op) {
        try (Socket socket = new Socket(hostname, port);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {
            Request req = new Request(op);
            
            req.sign(keyPair);
            
            Utils.send(out, req.toString());
            
            Acknowledgement ack = Acknowledgement.parse(Utils.receive(in));
            
            PublicKey spPubKey = Utils.readKeyPair("service_provider.key").getPublic();
            
            if (!ack.validate(spPubKey)) {
                throw new SignatureException("ACK validation failure");
            }
            
            String result = ack.getResult();
            
            if (op.getType() == OperationType.DOWNLOAD) {
                String fname = op.getPath();
                
                File file = new File(fname);
                
                Utils.receive(in, file);
                
                String digest = Utils.digest(file, Config.DIGEST_ALGORITHM);
                
                if (ack.getResult().compareTo(digest) == 0) {
                    result = "download success";
                } else {
                    result = "download file digest mismatch";
                }
            }
            
            File ack_attestation = new File("attestation/client/nonpov.ack");
            
            try (FileWriter fw = new FileWriter(ack_attestation, true)) {
                fw.append(ack.toString() + '\n');
            }
            
            File req_attestation = new File("attestation/client/nonpov.req");
            
            try (FileWriter fw = new FileWriter(req_attestation, true)) {
                fw.append(req.toString() + '\n');
            }
            
            socket.close();
        } catch (IOException | SignatureException ex) {
            Logger.getLogger(NonPOVClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static void main(String[] args) {
        for (int time = 1; time <= 3; time++) {
            NonPOVClient client = new NonPOVClient(Utils.readKeyPair("client.key"));

            Operation op = new Operation(OperationType.DOWNLOAD, "data/1M.txt", "");

            client.run(op);
        }
    }
}
