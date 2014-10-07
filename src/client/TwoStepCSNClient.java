package client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyPair;
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
public class TwoStepCSNClient {
    private final String hostname;
    private final int port;
    private final KeyPair keyPair;
    
    public TwoStepCSNClient(KeyPair keyPair) {
        this(Config.SERVICE_HOSTNAME, Config.SERVICE_PORT, keyPair);
    }
    
    public TwoStepCSNClient(String hostname, int port, KeyPair keyPair) {
        this.hostname = hostname;
        this.port = port;
        this.keyPair = keyPair;
    }
    
    public void run(Operation op, int csn) {
        try (Socket socket = new Socket(hostname, port);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {
            Request req = new Request(op, csn);
            
            Utils.send(out, req.toString());
            
            Acknowledgement ack = Acknowledgement.parse(Utils.receive(in));
            
            String result = ack.getResult();
            
            if (result.compareTo("CSN mismatch") == 0) {
                throw new IllegalAccessException(result);
            }
            
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
            
            System.out.println(result);
            
            socket.close();
        } catch (IOException ex) {
            Logger.getLogger(TwoStepCSNClient.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(TwoStepCSNClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static void main(String[] args) {
        for (int csn = 1; csn <= 3; csn++) {
            TwoStepCSNClient client = new TwoStepCSNClient(Utils.readKeyPair("client.key"));

            Operation op = new Operation(OperationType.DOWNLOAD, "data/1M.txt", "");

            client.run(op, csn);
        }
    }
}
