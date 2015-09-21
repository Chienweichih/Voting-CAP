package wei_shian_pov.client;

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

import client.Client;
import message.Operation;
import message.OperationType;
import utility.Utils;
import wei_shian_pov.message.twostep.voting.*;
import wei_shian_pov.service.Config;
import wei_shian_pov.service.handler.twostep.WeiShianHandler;

/**
 *
 * @author Chienweichih
 * //! is what need to change
 */
public class WeiShianClient extends Client {
    private static final File ATTESTATION;
    private static final Logger LOGGER;
    
    static {
        ATTESTATION = new File(service.Config.ATTESTATION_DIR_PATH + "/client/WeiShian");
        LOGGER = Logger.getLogger(WeiShianClient.class.getName());
    }
    
    private String lastChainHash;
    
    public WeiShianClient(KeyPair keyPair, KeyPair spKeyPair) {
        super(Config.SERVICE_HOSTNAME,
              Config.WEI_SHIAN_SERVICE_PORT,
              keyPair,
              spKeyPair,
              Config.NUM_PROCESSORS);
        
        this.lastChainHash = Config.DEFAULT_CHAINHASH;
    }
    
    public String getLastChainHash() {
        return lastChainHash;
    }
    
    @Override
    protected void hook(Operation op, Socket socket, DataOutputStream out, DataInputStream in) 
            throws SignatureException, IllegalAccessException {
        //! update roothash and last attestation from sync. server
        
        Request req = new Request(op);

        req.sign(keyPair);

        Utils.send(out, req.toString());

        if (op.getType() == OperationType.UPLOAD) {
            Utils.send(out, new File(Config.DATA_DIR_PATH + File.separator + op.getPath()));
        }

        Acknowledgement ack = Acknowledgement.parse(Utils.receive(in));

        if (!ack.validate(spKeyPair.getPublic())) {
            throw new SignatureException("ACK validation failure");
        }

        String result = ack.getResult();
        String chainHash = ack.getChainHash();
        String fname = "";

        if (!chainHash.equals(lastChainHash)) {
            throw new IllegalAccessException("Chain hash mismatch");
        }

        if (op.getType() != OperationType.AUDIT) { // dirty fix
            lastChainHash = Utils.digest(ack.toString());
        }
        
        switch (op.getType()) {
            case UPLOAD:
                //! update merkletree
                //! check roothash
                break;
            case DOWNLOAD:
                //! check roothash
                //! check file hash
                fname = "-" + System.currentTimeMillis();
            case AUDIT:
                fname = String.format("%s/%s%s",
                            service.Config.DOWNLOADS_DIR_PATH,
                            op.getPath(),
                            fname);

                File file = new File(fname);

                Utils.receive(in, file);

                String digest = Utils.digest(file);

                if (result.equals(digest)) {
                    result = "download success";
                } else {
                    result = "download file digest mismatch";
                }

                break;
        }

        long start = System.currentTimeMillis();
        //! update sync. to last attestation and roothash
        Utils.write(ATTESTATION, ack.toString());
        this.attestationCollectTime += System.currentTimeMillis() - start;
    }

    @Override
    public String getHandlerAttestationPath() {
        return WeiShianHandler.ATTESTATION.getPath();
    }

    @Override
    public boolean audit(File spFile) {
        boolean success = true;
        PublicKey spKey = spKeyPair.getPublic();
        PublicKey cliKey = keyPair.getPublic();
        
        //! need to change, wei shian's different.
        try (FileReader fr = new FileReader(spFile);
             BufferedReader br = new BufferedReader(fr)) {
            String chainhash = service.Config.DEFAULT_CHAINHASH;
            
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
