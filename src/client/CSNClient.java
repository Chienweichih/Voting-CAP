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
              false);
        
        this.csn = 1;
    }
    
    @Override
    protected void handle(Operation op, Socket socket, DataOutputStream out, DataInputStream in)
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
        
        Utils.write(ATTESTATION, ack.toString() + '\n');
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
        int csn = 1;
        
        try (FileReader cliFr = new FileReader(ATTESTATION);
             BufferedReader cliBr = new BufferedReader(cliFr);
             FileReader spFr = new FileReader(spFile);
             BufferedReader spBr = new BufferedReader(spFr)) {
            while (success) {
                String s = spBr.readLine();
                
                if (s == null) {
                    break;
                } else {
                    Acknowledgement ack = Acknowledgement.parse(s);
                    Request req = ack.getRequest();
                    
                    if (req.getConsecutiveSequenceNumber() != csn) {
                        success = false;
                    } else {
                        csn += 1;
                    }
                    
                    success &= ack.validate(spKey) & req.validate(cliKey);
                }
            }
            
            Request req = Acknowledgement.parse(cliBr.readLine()).getRequest();
            success = (csn == req.getConsecutiveSequenceNumber());
        } catch (IOException ex) {
            success = false;
            
            LOGGER.log(Level.SEVERE, null, ex);
        }
        
        return success;
    }
}
