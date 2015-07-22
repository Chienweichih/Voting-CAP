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
public class CSNClient extends Client {
    private static final File ATTESTATION;
    private static final Logger LOGGER;
    
    static {
        ATTESTATION = new File(Config.ATTESTATION_DIR_PATH + "/client/csn");
        LOGGER = Logger.getLogger(CSNClient.class.getName());
    }
    
    private int csn;
    
    public CSNClient(KeyPair keyPair, KeyPair spKeyPair) {
        super(Config.SERVICE_HOSTNAME,
              Config.CSN_SERVICE_PORT,
              keyPair,
              spKeyPair,
              1);
        
        this.csn = 1;
    }
    
    @Override
    protected void hook(Operation op, Socket socket, DataOutputStream out, DataInputStream in)
            throws SignatureException, IllegalAccessException {
        Request req = new Request(op, csn);
        
        req.sign(keyPair);
        
        Utils.send(out, req.toString());
        
        if (op.getType() == OperationType.UPLOAD) {
            Utils.send(out, new File(Config.DATA_DIR_PATH + '/' + op.getPath()));
        }

        Acknowledgement ack = Acknowledgement.parse(Utils.receive(in));
        
        if (!ack.validate(spKeyPair.getPublic())) {
            throw new SignatureException("ACK validation failure");
        }

        String result = ack.getResult();
        String fname = "";

        if (result.compareTo("CSN mismatch") == 0) {
            throw new IllegalAccessException(result);
        }

        csn += 1;

        switch (op.getType()) {
            case DOWNLOAD:
                fname = "-" + System.currentTimeMillis();
            case AUDIT:
                fname = String.format("%s/%s%s",
                            Config.DOWNLOADS_DIR_PATH,
                            op.getPath(),
                            fname);

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

        long start = System.currentTimeMillis();
        Utils.append(ATTESTATION, ack.toString() + '\n');
        super.attestationCollectTime += System.currentTimeMillis() - start;
    }

    @Override
    public String getHandlerAttestationPath() {
        return CSNHandler.ATTESTATION.getPath();
    }

    @Override
    public boolean audit(File spFile) {
        boolean success = true;
        PublicKey spKey = spKeyPair.getPublic();
        PublicKey cliKey = keyPair.getPublic();
        
        try (FileReader cliFr = new FileReader(ATTESTATION);
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
                    
                    if (req1.getConsecutiveSequenceNumber().compareTo(
                        req2.getConsecutiveSequenceNumber()) != 0) {
                        success = false;
                    }
                    
                    success &= ack1.validate(spKey) & req1.validate(cliKey);
                    success &= ack2.validate(spKey) & req2.validate(cliKey);
                }
            }
        } catch (IOException ex) {
            success = false;
            
            LOGGER.log(Level.SEVERE, null, ex);
        }
        
        return success;
    }
}
