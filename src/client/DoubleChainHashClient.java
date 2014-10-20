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
import message.fourstep.doublechainhash.*;
import service.Config;
import utility.Utils;

/**
 *
 * @author Scott
 */
public class DoubleChainHashClient {
    private final String hostname;
    private final int port;
    private final KeyPair keyPair;
    private String lastChainHash;
    
    public DoubleChainHashClient(KeyPair keyPair, String hash) {
        this(Config.SERVICE_HOSTNAME, Config.SERVICE_PORT, keyPair, hash);
    }
    
    public DoubleChainHashClient(String hostname, int port, KeyPair keyPair, String hash) {
        this.hostname = hostname;
        this.port = port;
        this.keyPair = keyPair;
        this.lastChainHash = hash;
    }
    
    public String getLastChainHash() {
        return lastChainHash;
    }
    
    public void run(Operation op, String id) {
        PublicKey spPubKey = Utils.readKeyPair("service_provider.key").getPublic();
        
        try (Socket socket = new Socket(hostname, port);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {
            Request req = new Request(op, id);
            
            req.sign(keyPair);
            
            Utils.send(out, req.toString());
            
            Response res = Response.parse(Utils.receive(in));
            
            if (!res.validate(spPubKey)) {
                throw new SignatureException("RES validation failure");
            }
            
            String result = null;
            
            if (lastChainHash.compareTo(res.getClientDeviceLastChainHash()) != 0) {
                result = "chain hash mismatch";
            }
            
            ReplyResponse rr = new ReplyResponse(res);
            
            rr.sign(keyPair);
            
            Utils.send(out, rr.toString());
            
            Acknowledgement ack = Acknowledgement.parse(Utils.receive(in));
            
            if (!ack.validate(spPubKey)) {
                throw new SignatureException("ACK validation failure");
            }
            
            if (result == null) {
                result = ack.getResult();
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
            
            lastChainHash = Utils.digest(ack.toString());
            
            File attestation = new File("attestation/client/doublechainhash");
            
            try (FileWriter fw = new FileWriter(attestation)) {
                fw.append(ack.toString() + '\n');
            }
            
            socket.close();
        } catch (IOException | SignatureException ex) {
            Logger.getLogger(TwoStepCSNClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static void main(String[] args) {
        KeyPair keyPair = Utils.readKeyPair("client.key");
        String chainhash = Config.DEFAULT_CHAINHASH;
        
        DoubleChainHashClient client = new DoubleChainHashClient(keyPair, chainhash);
        
        for (int i = 1; i <= 3; i++) {
            Operation op = new Operation(OperationType.DOWNLOAD, "data/1M.txt", "");

            client.run(op, "client01");
        }
    }
}
