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
import message.fourstep.chainhash_lsn.*;
import service.Config;
import utility.Utils;

/**
 *
 * @author Scott
 */
public class ChainHashAndLSNClient {
    private final String hostname;
    private final int port;
    private final String id;
    private final KeyPair keyPair;
    private int lsn;
    
    public ChainHashAndLSNClient(String id, KeyPair keyPair) {
        this(Config.SERVICE_HOSTNAME, Config.CHAINHASH_LSN_SERVICE_PORT, id, keyPair);
    }
    
    public ChainHashAndLSNClient(String hostname, int port, String id, KeyPair keyPair) {
        this.hostname = hostname;
        this.port = port;
        this.id = id;
        this.keyPair = keyPair;
        this.lsn = 1;
    }
    
    public void run(Operation op) {
        PublicKey spPubKey = Utils.readKeyPair("service_provider.key").getPublic();
        
        try (Socket socket = new Socket(hostname, port);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {
            Request req = new Request(op, id, lsn);
            
            req.sign(keyPair);
            
            Utils.send(out, req.toString());
            
            Response res = Response.parse(Utils.receive(in));
            
            if (!res.validate(spPubKey)) {
                throw new SignatureException("RES validation failure");
            }
            
            ReplyResponse rr = new ReplyResponse(res);
            
            rr.sign(keyPair);
            
            Utils.send(out, rr.toString());
            
            Acknowledgement ack = Acknowledgement.parse(Utils.receive(in));
            
            if (!ack.validate(spPubKey)) {
                throw new SignatureException("ACK validation failure");
            }
            
            String result = ack.getResult();
            
            lsn += 1;
            
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
            
            File attestation = new File("attestation/client/chainhash-lsn");
            
            try (FileWriter fw = new FileWriter(attestation, true)) {
                fw.append(ack.toString() + '\n');
            }
            
            socket.close();
        } catch (IOException | SignatureException ex) {
            Logger.getLogger(CSNClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static void main(String[] args) {
        String id = "client";
        KeyPair keypair = Utils.readKeyPair(id + ".key");
        ChainHashAndLSNClient client = new ChainHashAndLSNClient(id, keypair);
        Operation op = new Operation(OperationType.DOWNLOAD, "data/1M.txt", "");
        
        for (int i = 1; i <= Config.NUM_RUNS; i++) {
            client.run(op);
        }
    }
}
