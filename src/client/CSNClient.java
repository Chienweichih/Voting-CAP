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
import message.twostep.csn.Acknowledgement;
import message.twostep.csn.Request;
import service.Config;
import utility.Utils;

/**
 *
 * @author Scott
 */
public class CSNClient {
    private final String hostname;
    private final int port;
    private final KeyPair keyPair;
    private int csn;
    
    public CSNClient(KeyPair keyPair) {
        this(Config.SERVICE_HOSTNAME, Config.CSN_SERVICE_PORT, keyPair);
    }
    
    public CSNClient(String hostname, int port, KeyPair keyPair) {
        this.hostname = hostname;
        this.port = port;
        this.keyPair = keyPair;
        this.csn = 1;
    }
    
    public void run(Operation op) {
        PublicKey spPubKey = Utils.readKeyPair("service_provider.key").getPublic();
        
        try (Socket socket = new Socket(hostname, port);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {
            Request req = new Request(op, csn);
            
            req.sign(keyPair);
            
            Utils.send(out, req.toString());
            
            Acknowledgement ack = Acknowledgement.parse(Utils.receive(in));
            
            if (!ack.validate(spPubKey)) {
                throw new SignatureException("ACK validation failure");
            }
            
            String result = ack.getResult();
            
            if (result.compareTo("CSN mismatch") == 0) {
                throw new IllegalAccessException(result);
            }
            
            csn += 1;
            
            if (op.getType() == OperationType.DOWNLOAD) {
                String fname = op.getPath();
                
                File file = new File(fname);
                
                Utils.receive(in, file);
                
                String digest = Utils.digest(file, Config.DIGEST_ALGORITHM);
                
                if (result.compareTo(digest) == 0) {
                    result = "download success";
                } else {
                    result = "download file digest mismatch";
                }
            }
            
            File attestation = new File("attestation/client/csn");
            
            try (FileWriter fw = new FileWriter(attestation, true)) {
                fw.append(ack.toString() + '\n');
            }
            
            socket.close();
        } catch (IOException | IllegalAccessException | SignatureException ex) {
            Logger.getLogger(CSNClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static void main(String[] args) {
        KeyPair keypair = Utils.readKeyPair("client.key");
        CSNClient client = new CSNClient(keypair);
        Operation op = new Operation(OperationType.DOWNLOAD, "data/1M.txt", "");

        for (int csn = 1; csn <= Config.NUM_RUNS; csn++) {
            client.run(op);
        }
    }
}
