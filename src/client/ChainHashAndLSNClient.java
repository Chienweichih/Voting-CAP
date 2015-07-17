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
import message.fourstep.chainhash_lsn.*;
import service.Config;
import service.handler.fourstep.ChainHashAndLSNHandler;
import service.handler.fourstep.HashingChainTable;
import service.handler.fourstep.LSNTable;
import utility.Utils;

/**
 *
 * @author Scott
 */
public class ChainHashAndLSNClient extends Client {
    private static final File ATTESTATION;
    private static final Logger LOGGER;
    
    static {
        ATTESTATION = new File(Config.ATTESTATION_DIR_PATH + "/client/chainhash-lsn");
        LOGGER = Logger.getLogger(ChainHashAndLSNClient.class.getName());
    }
    
    private final String id;
    private int lsn;
    
    public ChainHashAndLSNClient(String id, KeyPair keyPair, KeyPair spKeyPair) {
        super(Config.SERVICE_HOSTNAME,
              Config.CHAINHASH_LSN_SERVICE_PORT,
              keyPair,
              spKeyPair);
        
        this.id = id;
        this.lsn = 1;
    }
    
    @Override
    protected void hook(Operation op, Socket socket, DataOutputStream out, DataInputStream in)
            throws SignatureException, IllegalAccessException {
        Request req = new Request(op, id, lsn);

        req.sign(keyPair);

        Utils.send(out, req.toString());

        Response res = Response.parse(Utils.receive(in));

        if (!res.validate(spKeyPair.getPublic())) {
            throw new SignatureException("RES validation failure");
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

        String result = ack.getResult();

        lsn += 1;

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

        long start = System.currentTimeMillis();
        Utils.write(ATTESTATION, ack.toString());
        this.attestationCollectTime += System.currentTimeMillis() - start;
    }

    @Override
    public String getHandlerAttestationPath() {
        return ChainHashAndLSNHandler.ATTESTATION.getPath();
    }

    @Override
    public boolean audit(File spFile) {
        boolean success = true;
        PublicKey spKey = spKeyPair.getPublic();
        PublicKey cliKey = keyPair.getPublic();
        
        LSNTable lsnTab = new LSNTable();
        HashingChainTable hashingChainTab = new HashingChainTable();
        
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
                
                if (lsnTab.isMatched(clientID, req.getLocalSequenceNumber())) {
                    lsnTab.increment(clientID);
                } else {
                    success = false;
                }
                
                if (hashingChainTab.getLastChainHash(clientID).compareTo(
                    res.getChainHash()) == 0) {
                    hashingChainTab.chain(clientID, Utils.digest(ack.toString()));
                } else {
                    success = false;
                }
                
                success &= ack.validate(spKey) & rr.validate(cliKey);
                success &= res.validate(spKey) & req.validate(cliKey);
            } while (success);
        } catch (IOException ex) {
            success = false;
            
            LOGGER.log(Level.SEVERE, null, ex);
        }
        
        return success;
    }
}
