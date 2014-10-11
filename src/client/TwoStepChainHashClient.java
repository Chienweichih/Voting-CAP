package client;

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
import message.OperationType;
import message.twostep.chainhash.Acknowledgement;
import message.twostep.chainhash.Request;
import service.Config;
import utility.Utils;

/**
 *
 * @author Scott
 */
public class TwoStepChainHashClient {
    private final String hostname;
    private final int port;
    private final KeyPair keyPair;
    private String lastChainHash;
    
    public TwoStepChainHashClient(KeyPair keyPair, String lastChainHash) {
        this(Config.SERVICE_HOSTNAME, Config.SERVICE_PORT, keyPair, lastChainHash);
    }
    
    public TwoStepChainHashClient(String hostname, int port, KeyPair keyPair, String lastChainHash) {
        this.hostname = hostname;
        this.port = port;
        this.keyPair = keyPair;
        this.lastChainHash = lastChainHash;
    }
    
    public String getLastChainHash() {
        return lastChainHash;
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
            
            String result = null;
            String chainHash = ack.getChainHash();
            
            if (chainHash.compareTo(lastChainHash) != 0) {
                throw new IllegalAccessException("Chain hash mismatch");
            }
            
            lastChainHash = Utils.digest(ack.toString());
            
            if (op.getType() == OperationType.DOWNLOAD) {
                String fname = op.getPath();
                
                File file = new File(fname);
                
                Utils.receive(in, file);
                
                String digest = Utils.digest(file);
                
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
        } catch (SignatureException ex) {
            Logger.getLogger(TwoStepCSNClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static void main(String[] args) {
        String chainhash = Config.DEFAULT_CHAINHASH;
        
        for (int time = 1; time <= 3; time++) {
            TwoStepChainHashClient client = new TwoStepChainHashClient(Utils.readKeyPair("client.key"), chainhash);

            Operation op = new Operation(OperationType.DOWNLOAD, "data/1M.txt", "");

            client.run(op);
            
            chainhash = client.getLastChainHash();
        }
    }
}
