package client;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
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
import service.handler.twostep.CSNHandler;
import utility.Utils;

/**
 *
 * @author Scott
 */
public class CSNClient {
    private static final File ATTESTATION;
    
    static {
        ATTESTATION = new File("attestation/client/csn");
    }
    
    private final String hostname;
    private final int port;
    private final KeyPair keyPair;
    private final KeyPair spKeyPair;
    private int csn;
    
    public CSNClient(KeyPair keyPair, KeyPair spKeyPair) {
        this(Config.SERVICE_HOSTNAME, Config.CSN_SERVICE_PORT, keyPair, spKeyPair);
    }
    
    public CSNClient(String hostname, int port, KeyPair keyPair, KeyPair spKeyPair) {
        this.hostname = hostname;
        this.port = port;
        this.keyPair = keyPair;
        this.spKeyPair = spKeyPair;
        this.csn = 1;
    }
    
    public void run(Operation op) {
        try (Socket socket = new Socket(hostname, port);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {
            Request req = new Request(op, csn);
            
            req.sign(keyPair);
            
            Utils.send(out, req.toString());
            
            Acknowledgement ack = Acknowledgement.parse(Utils.receive(in));
            
            if (!ack.validate(spKeyPair.getPublic())) {
                throw new SignatureException("ACK validation failure");
            }
            
            String result = ack.getResult();
            
            if (result.compareTo("CSN mismatch") == 0) {
                throw new IllegalAccessException(result);
            }
            
            csn += 1;
            
            switch (op.getType()) {
                case AUDIT:
                case DOWNLOAD:
                    String fname = Config.DOWNLOADS_DIR_PATH + '/' + op.getPath();
                    
                    File file = new File(fname);

                    Utils.receive(in, file);

                    String digest = Utils.digest(file);

                    if (result.compareTo(digest) == 0) {
                        result = "download success";
                    } else {
                        result = "download file digest mismatch";
                    }
                    
                    break;
            }
            
            Utils.append(ATTESTATION, ack.toString() + '\n');
            
            socket.close();
        } catch (IOException | IllegalAccessException | SignatureException ex) {
            Logger.getLogger(CSNClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public boolean audit(File cliFile, PublicKey cliKey, File spFile, PublicKey spKey) {
        boolean success = true;
        
        try (FileReader cliFr = new FileReader(cliFile);
             BufferedReader cliBr = new BufferedReader(cliFr);
             FileReader spFr = new FileReader(spFile);
             BufferedReader spBr = new BufferedReader(spFr)) {
            while (success) {
                String s1 = cliBr.readLine();
                String s2 = spBr.readLine();
                
                if (s1 == null || s2 == null) {
                    break;
                } else if (s1.compareTo(s2) != 0) {
                    success = false;
                } else {
                    Acknowledgement ack1 = Acknowledgement.parse(s1);
                    Request req1 = ack1.getRequest();
                    
                    Acknowledgement ack2 = Acknowledgement.parse(s2);
                    Request req2 = ack2.getRequest();
                    
                    if (req1.getConsecutiveSequenceNumber().compareTo(req2.getConsecutiveSequenceNumber()) != 0) {
                        success = false;
                    }
                    
                    success &= ack1.validate(spKey) & req1.validate(cliKey);
                    success &= ack2.validate(spKey) & req2.validate(cliKey);
                }
            }
        } catch (IOException ex) {
            success = false;
            
            Logger.getLogger(CSNClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return success;
    }
    
    public static void main(String[] args) {
        KeyPair keypair = Utils.readKeyPair("client.key");
        KeyPair spKeypair = Utils.readKeyPair("service_provider.key");
        CSNClient client = new CSNClient(keypair, spKeypair);
        Operation op = new Operation(OperationType.DOWNLOAD, Config.FNAME, "");

        System.out.println("Running:");
        
        long time = System.currentTimeMillis();
        for (int i = 1; i <= Config.NUM_RUNS; i++) {
            client.run(op);
        }
        time = System.currentTimeMillis() - time;
        
        System.out.println(Config.NUM_RUNS + " times cost " + time + "ms");
        
        System.out.println("Auditing:");
        
        op = new Operation(OperationType.AUDIT, CSNHandler.ATTESTATION.getPath(), "");
        
        client.run(op);
        
        File auditFile = new File(Config.DOWNLOADS_DIR_PATH + '/' + CSNHandler.ATTESTATION.getPath());
        
        // to prevent ClassLoader's init overhead
        client.audit(ATTESTATION, keypair.getPublic(), auditFile, spKeypair.getPublic());
        
        time = System.currentTimeMillis();
        boolean audit = client.audit(ATTESTATION, keypair.getPublic(),
                                     auditFile, spKeypair.getPublic());
        time = System.currentTimeMillis() - time;
        
        System.out.println("Audit: " + audit + ", cost " + time + "ms");
    }
}
