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
import message.twostep.chainhash.Acknowledgement;
import message.twostep.chainhash.Request;
import service.Config;
import utility.Utils;

/**
 *
 * @author Scott
 */
public class ChainHashClient {
    private final String hostname;
    private final int port;
    private final KeyPair keyPair;
    private String lastChainHash;
    
    public ChainHashClient(KeyPair keyPair, String lastChainHash) {
        this(Config.SERVICE_HOSTNAME, Config.CHAINHASH_SERVICE_PORT, keyPair, lastChainHash);
    }
    
    public ChainHashClient(String hostname, int port, KeyPair keyPair, String lastChainHash) {
        this.hostname = hostname;
        this.port = port;
        this.keyPair = keyPair;
        this.lastChainHash = lastChainHash;
    }
    
    public String getLastChainHash() {
        return lastChainHash;
    }
    
    public void run(Operation op) {
        PublicKey spPubKey = Utils.readKeyPair("service_provider.key").getPublic();
        
        try (Socket socket = new Socket(hostname, port);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {
            Request req = new Request(op);
            
            req.sign(keyPair);
            
            Utils.send(out, req.toString());
            
            Acknowledgement ack = Acknowledgement.parse(Utils.receive(in));
            
            if (!ack.validate(spPubKey)) {
                throw new SignatureException("ACK validation failure");
            }
            
            String result = ack.getResult();
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
                
                if (result.compareTo(digest) == 0) {
                    result = "download success";
                } else {
                    result = "download file digest mismatch";
                }
            }
            
            File attestation = new File("attestation/client/chainhash");
            
            try (FileWriter fw = new FileWriter(attestation)) {
                fw.append(ack.toString() + '\n');
            }
            
            socket.close();
        } catch (IOException | IllegalAccessException | SignatureException ex) {
            Logger.getLogger(CSNClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static void main(String[] args) {
        ChainHashClient client = new ChainHashClient(Utils.readKeyPair("client.key"), Config.DEFAULT_CHAINHASH);
        Operation op = new Operation(OperationType.DOWNLOAD, "data/1M.txt", "");
        
        for (int time = 1; time <= 1000; time++) {
            client.run(op);
        }
    }
}
