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
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import message.Operation;
import message.OperationType;
import message.fourstep.doublechainhash.*;
import service.Config;
import service.handler.fourstep.DoubleChainHashHandler;
import service.DoubleHashingChainTable;
import utility.Utils;

/**
 *
 * @author Scott
 */
public class DoubleChainHashClient extends Client {
    private static final File ATTESTATION;
    private static final Logger LOGGER;
    
    static {
        ATTESTATION = new File(Config.ATTESTATION_DIR_PATH + "/client/doublechainhash");
        LOGGER = Logger.getLogger(DoubleChainHashClient.class.getName());
    }
    
    private final String id;
    private String lastChainHash;
    
    public DoubleChainHashClient(String id, KeyPair keyPair, KeyPair spKeyPair) {
        super(Config.SERVICE_HOSTNAME,
              Config.DOUBLECHAINHASH_SERVICE_PORT,
              keyPair,
              spKeyPair,
              true);
        
        this.id = id;
        this.lastChainHash = Config.INITIAL_HASH;
    }
    
    public String getLastChainHash() {
        return lastChainHash;
    }
    
    @Override
    protected void handle(Operation op, Socket socket, DataOutputStream out, DataInputStream in)
            throws SignatureException, IllegalAccessException {
        Request req = new Request(op, op.getClientID());
        
        req.sign(keyPair);

        Utils.send(out, req.toString());

        Response res = Response.parse(Utils.receive(in));

        if (!res.validate(spKeyPair.getPublic())) {
            throw new SignatureException("RES validation failure");
        }

        String result = null;

        if (lastChainHash.compareTo(res.getClientDeviceLastChainHash()) != 0) {
            result = "chain hash mismatch";
        }

        ReplyResponse rr = new ReplyResponse(res);

        rr.sign(keyPair);

        Utils.send(out, rr.toString());

        if (op.getType() == OperationType.UPLOAD) {
            Utils.send(out, new File(Config.DATA_DIR_PATH + '/' + op.getPath()));
        }

        Acknowledgement ack = Acknowledgement.parse(Utils.receive(in));

        if (!ack.validate(spKeyPair.getPublic())) {
            throw new SignatureException("ACK validation failure");
        }

        if (result == null) {
            result = ack.getResult();
        }
        
        String fname = "";

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

                if (ack.getResult().compareTo(digest) == 0) {
                    result = "download success";
                } else {
                    result = "download file digest mismatch";
                }

                break;
        }

        lastChainHash = Utils.digest(ack.toString());

        synchronized (this) {
            Utils.write(ATTESTATION, ack.toString());
        }
    }
    
    @Override
    public String getHandlerAttestationPath() {
        return DoubleChainHashHandler.ATTESTATION.getPath();
    }

    @Override
    public boolean audit(File spFile) {
        boolean success = true;
        PublicKey spKey = spKeyPair.getPublic();
        PublicKey cliKey = keyPair.getPublic();
        
        DoubleHashingChainTable hashingChainTab = new DoubleHashingChainTable();
        Map<String, String> mainChainTab = new HashMap<>();
        
        try (FileReader fr = new FileReader(spFile);
             BufferedReader br = new BufferedReader(fr)) {
            do {
                String s = br.readLine();
                
                if (s == null) {
                    break;
                }
                
                Acknowledgement ack = Acknowledgement.parse(s);
                ReplyResponse rr = ack.getReplyResponse();
                Response res = rr.getResponse();
                Request req = res.getRequest();
                
                String clientID = req.getClientID();
                
                mainChainTab.put(res.getUserLastChainHash(), Utils.digest(res.toString()));
                
                if (hashingChainTab.getLastChainHash(clientID).compareTo(
                    res.getClientDeviceLastChainHash()) == 0) {
                    hashingChainTab.chain(clientID, Utils.digest(ack.toString()));
                } else {
                    success = false;
                }
                
                success &= ack.validate(spKey) & rr.validate(cliKey);
                success &= res.validate(spKey) & req.validate(cliKey);
            } while (success);
            
            String hash = Config.INITIAL_HASH;
            int numFound = 0;
            
            while (mainChainTab.containsKey(hash)) {
                numFound += 1;
                
                hash = mainChainTab.get(hash);
            }
            
            success &= numFound == mainChainTab.size();
        } catch (IOException ex) {
            success = false;
            
            LOGGER.log(Level.SEVERE, null, ex);
        }
        
        return success;
    }
}
