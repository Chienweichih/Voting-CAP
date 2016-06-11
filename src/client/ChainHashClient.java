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
import message.twostep.chainhash.Acknowledgement;
import message.twostep.chainhash.Request;
import service.Config;
import service.handler.twostep.ChainHashHandler;
import utility.Utils;

/**
 *
 * @author Scott
 */
public class ChainHashClient extends Client {
    private static final File ATTESTATION;
    private static final Logger LOGGER;
    
    static {
        ATTESTATION = new File(Config.ATTESTATION_DIR_PATH + "/client/chainhash");
        LOGGER = Logger.getLogger(ChainHashClient.class.getName());
    }
    
    private String lastChainHash;
    
    public ChainHashClient(KeyPair keyPair, KeyPair spKeyPair) {
        super(Config.SERVICE_HOSTNAME,
              Config.CHAINHASH_SERVICE_PORT,
              keyPair,
              spKeyPair,
              false);
        
        this.lastChainHash = Config.INITIAL_HASH;
    }
    
    public String getLastChainHash() {
        return lastChainHash;
    }
    
    @Override
    protected void handle(Operation op, Socket socket, DataOutputStream out, DataInputStream in) 
            throws SignatureException, IllegalAccessException {
        Request req = new Request(op);

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
        String chainHash = ack.getChainHash();
        String fname = "";

        if (chainHash.compareTo(lastChainHash) != 0) {
            throw new IllegalAccessException("Chain hash mismatch");
        }

        if (op.getType() != OperationType.AUDIT) { // dirty fix
            lastChainHash = Utils.digest(ack.toString());
        }
        
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

        Utils.write(ATTESTATION, ack.toString());
    }

    @Override
    public String getHandlerAttestationPath() {
        return ChainHashHandler.ATTESTATION.getPath();
    }

    @Override
    public boolean audit(File spFile) {
        boolean success = true;
        PublicKey spKey = spKeyPair.getPublic();
        PublicKey cliKey = keyPair.getPublic();
        
        try (FileReader fr = new FileReader(spFile);
             BufferedReader br = new BufferedReader(fr)) {
            String chainhash = Config.INITIAL_HASH;
            
            do {
                String s = br.readLine();
                
                Acknowledgement ack = Acknowledgement.parse(s);
                Request req = ack.getRequest();
                
                if (chainhash.compareTo(ack.getChainHash()) == 0) {
                    chainhash = Utils.digest(ack.toString());
                } else {
                    success = false;
                }
                
                success &= ack.validate(spKey) & req.validate(cliKey);
            } while (success && chainhash.compareTo(lastChainHash) != 0);
        } catch (NullPointerException | IOException ex) {
            success = false;
            
            LOGGER.log(Level.SEVERE, null, ex);
        }
        
        return success;
    }
}
