package client;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
import service.handler.NonPOVHandler;
import utility.Utils;

/**
 *
 * @author Scott
 */
public class NonPOVClient extends Client {
    private static final File REQ_ATTESTATION;
    private static final File ACK_ATTESTATION;
    
    static {
        REQ_ATTESTATION = new File(Config.ATTESTATION_DIR_PATH + "/client/nonpov.req");
        ACK_ATTESTATION = new File(Config.ATTESTATION_DIR_PATH + "/client/nonpov.ack");
    }
    
    public NonPOVClient(KeyPair keyPair, KeyPair spKeyPair) {
        super(Config.SERVICE_HOSTNAME,
             Config.NONPOV_SERVICE_PORT,
             keyPair,
             spKeyPair);
    }
    
    @Override
    protected void hook(Operation op, Socket socket, DataOutputStream out, DataInputStream in)
            throws SignatureException, IllegalAccessException {
        Request req = new Request(op);

        req.sign(keyPair);

        Utils.send(out, req.toString());

        Acknowledgement ack = Acknowledgement.parse(Utils.receive(in));

        if (!ack.validate(spKeyPair.getPublic())) {
            throw new SignatureException("ACK validation failure");
        }

        String result = ack.getResult();
        String digest = "";

        switch (op.getType()) {
            case AUDIT:
                File tmp_req_attestation = new File(Config.DOWNLOADS_DIR_PATH + '/' + NonPOVHandler.REQ_ATTESTATION.getPath() + ".audit");
                File tmp_ack_attestation = new File(Config.DOWNLOADS_DIR_PATH + '/' + NonPOVHandler.ACK_ATTESTATION.getPath() + ".audit");

                Utils.receive(in, tmp_req_attestation);
                Utils.receive(in, tmp_ack_attestation);

                digest = Utils.digest(tmp_req_attestation) + Utils.digest(tmp_ack_attestation);

                break;
            case DOWNLOAD:
                String fname = Config.DOWNLOADS_DIR_PATH + '/' + op.getPath();

                File file = new File(fname);

                Utils.receive(in, file);

                digest = Utils.digest(file, Config.DIGEST_ALGORITHM);

                break;
        }

        if (result.compareTo(digest) == 0) {
            result = "download success";
        } else {
            result = "download file digest mismatch";
        }

        long start = System.currentTimeMillis();
        Utils.append(REQ_ATTESTATION, req.toString() + '\n');
        Utils.append(ACK_ATTESTATION, ack.toString() + '\n');
        this.attestationCollectTime += System.currentTimeMillis() - start;
    }
    
    public static boolean Audit(Class c, File cliFile, File spFile, PublicKey key) {
        boolean success = true;
        
        try (FileReader fr = new FileReader(cliFile);
             BufferedReader br = new BufferedReader(fr);
             FileReader frAudit = new FileReader(spFile);
             BufferedReader brAudit = new BufferedReader(frAudit)) {
            String s1, s2;
            
            Method parse = c.getMethod("parse", String.class);
            Method validate = c.getMethod("validate", PublicKey.class);
            
            while (success) {
                s1 = br.readLine();
                s2 = brAudit.readLine();
                
                // client side will have one more record about audit operation
                if (s1 == null || s2 == null) {
                    break;
                } else if (s1.compareTo(s2) != 0) {
                    success = false;
                } else {                
                    Object o1 = parse.invoke(null, s1);
                    Object o2 = parse.invoke(null, s2);

                    success &= (boolean) validate.invoke(o1, key);
                    success &= (boolean) validate.invoke(o2, key);
                }
            }
        } catch (IOException | NoSuchMethodException | SecurityException
               | IllegalAccessException | IllegalArgumentException
               | InvocationTargetException ex) {
            success = false;
            
            Logger.getLogger(NonPOVClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return success;
    }
    
    public static void main(String[] args) {
        KeyPair keypair = Config.KeyPair.CLIENT.getKeypair();
        KeyPair spKeypair = Config.KeyPair.SERVICE_PROVIDER.getKeypair();
        NonPOVClient client = new NonPOVClient(keypair, spKeypair);
        Operation op = new Operation(OperationType.DOWNLOAD, Config.FILE.getName(), "");
        
        System.out.println("Running:");
        
        long time = System.currentTimeMillis();
        for (int i = 1; i <= Config.NUM_RUNS; i++) {
            client.run(op);
        }
        time = System.currentTimeMillis() - time;
        
        System.out.println(Config.NUM_RUNS + " times cost " + time + "ms");
        
        System.out.println("Auditing:");
        
        client.run(new Operation(OperationType.AUDIT, "", ""));
        
        File reqAuditFile = new File(Config.DOWNLOADS_DIR_PATH + '/' + NonPOVHandler.REQ_ATTESTATION.getPath() + ".audit");
        File ackAuditFile = new File(Config.DOWNLOADS_DIR_PATH + '/' + NonPOVHandler.ACK_ATTESTATION.getPath() + ".audit");
        
        // to prevent ClassLoader's init overhead
        Audit(Request.class, REQ_ATTESTATION, reqAuditFile, keypair.getPublic());
        Audit(Acknowledgement.class, ACK_ATTESTATION, ackAuditFile, spKeypair.getPublic());
        
        time = System.currentTimeMillis();
        boolean reqAudit = Audit(Request.class,
                                 REQ_ATTESTATION,
                                 reqAuditFile,
                                 keypair.getPublic());
        time = System.currentTimeMillis() - time;
        
        System.out.println("Request: " + reqAudit + ", cost " + time + "ms");
        
        time = System.currentTimeMillis();
        boolean ackAudit = Audit(Acknowledgement.class,
                                 ACK_ATTESTATION,
                                 ackAuditFile,
                                 spKeypair.getPublic());
        time = System.currentTimeMillis() - time;
        
        System.out.println("Ack: " + ackAudit + ", cost " + time + "ms");
        
    }
}
