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
import message.fourstep.doublechainhash.*;
import service.Config;
import service.handler.fourstep.DoubleChainHashHandler;
import service.handler.fourstep.DoubleHashingChainTable;
import utility.Utils;

/**
 *
 * @author Scott
 */
public class DoubleChainHashClient extends Client {
    private static final File ATTESTATION;
    
    static {
        ATTESTATION = new File(Config.ATTESTATION_DIR_PATH + "/client/doublechainhash");
    }
    
    private final String id;
    private String lastChainHash;
    
    public DoubleChainHashClient(String id, KeyPair keyPair, KeyPair spKeyPair) {
        this(Config.SERVICE_HOSTNAME, Config.DOUBLECHAINHASH_SERVICE_PORT, id, keyPair, spKeyPair);
    }
    
    public DoubleChainHashClient(String hostname,
                                 int port,
                                 String id,
                                 KeyPair keyPair,
                                 KeyPair spKeyPair) {
        super(hostname, port, keyPair, spKeyPair);
        
        this.id = id;
        this.lastChainHash = Config.DEFAULT_CHAINHASH;
    }
    
    public String getLastChainHash() {
        return lastChainHash;
    }
    
    @Override
    protected void hook(Operation op, Socket socket, DataOutputStream out, DataInputStream in)
            throws SignatureException, IllegalAccessException {
        Request req = new Request(op, id);

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

        switch (op.getType()) {
            case AUDIT:
            case DOWNLOAD:
                String fname = Config.DOWNLOADS_DIR_PATH + '/' + op.getPath();

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

        long start = System.currentTimeMillis();
        Utils.write(ATTESTATION, ack.toString());
        this.attestationCollectTime += System.currentTimeMillis() - start;
    }
    
    public boolean audit(File attestation, PublicKey cliKey, PublicKey spKey) {
        boolean success = true;
        
        DoubleHashingChainTable hashingChainTab = new DoubleHashingChainTable();
        
        try (FileReader fr = new FileReader(attestation);
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
                
                if (hashingChainTab.getLastChainHashOfAll().compareTo(res.getUserLastChainHash()) == 0) {
                    hashingChainTab.chain(Utils.digest(res.toString()));
                } else {
                    success = false;
                }
                
                if (hashingChainTab.getLastChainHash(clientID).compareTo(res.getClientDeviceLastChainHash()) == 0) {
                    hashingChainTab.chain(clientID, Utils.digest(ack.toString()));
                } else {
                    success = false;
                }
                
                success &= ack.validate(spKey) & rr.validate(cliKey);
                success &= res.validate(spKey) & req.validate(cliKey);
            } while (success);
        } catch (IOException ex) {
            success = false;
            
            Logger.getLogger(DoubleChainHashClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return success;
    }
    
    public static void main(String[] args) {
        KeyPair keypair = Config.KeyPair.CLIENT.getKeypair();
        KeyPair spKeypair = Config.KeyPair.SERVICE_PROVIDER.getKeypair();
        DoubleChainHashClient client = new DoubleChainHashClient("client", keypair, spKeypair);
        Operation op = new Operation(OperationType.DOWNLOAD, Config.FILE.getName(), "");
//        Operation op = new Operation(OperationType.UPLOAD, Config.FILE.getName(), Utils.readDigest(Config.FILE.getPath()));
        
        System.out.println("Running:");
        
        long time = System.currentTimeMillis();
        for (int i = 1; i <= Config.NUM_RUNS; i++) {
            client.run(op);
        }
        time = System.currentTimeMillis() - time;
        
        System.out.println(Config.NUM_RUNS + " times cost " + (time - client.attestationCollectTime) + "ms (without collect attestations)");
        System.out.println("Collect attestations cost " + client.attestationCollectTime + "ms");
        
        System.out.println("Auditing:");
        
        op = new Operation(OperationType.AUDIT, DoubleChainHashHandler.ATTESTATION.getPath(), "");
        
        client.run(op);
        
        File auditFile = new File(Config.DOWNLOADS_DIR_PATH + '/' + DoubleChainHashHandler.ATTESTATION.getPath());
        
        // to prevent ClassLoader's init overhead
        client.audit(auditFile, keypair.getPublic(), spKeypair.getPublic());
        
        time = System.currentTimeMillis();
        boolean audit = client.audit(auditFile,
                                     keypair.getPublic(), spKeypair.getPublic());
        time = System.currentTimeMillis() - time;
        
        System.out.println("Audit: " + audit + ", cost " + time + "ms");
    }
}
