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
    
    static {
        ATTESTATION = new File(Config.ATTESTATION_DIR_PATH + "/client/chainhash");
    }
    
    private String lastChainHash;
    
    public ChainHashClient(KeyPair keyPair, KeyPair spKeyPair) {
        this(Config.SERVICE_HOSTNAME, Config.CHAINHASH_SERVICE_PORT, keyPair, spKeyPair);
    }
    
    public ChainHashClient(String hostname, int port, KeyPair keyPair, KeyPair spKeyPair) {
        super(hostname, port, keyPair, spKeyPair);
        
        this.lastChainHash = Config.DEFAULT_CHAINHASH;
    }
    
    public String getLastChainHash() {
        return lastChainHash;
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
        String chainHash = ack.getChainHash();

        if (chainHash.compareTo(lastChainHash) != 0) {
            throw new IllegalAccessException("Chain hash mismatch");
        }

        lastChainHash = Utils.digest(ack.toString());

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
    
    public boolean audit(String lastChainHash, File attestation, PublicKey cliKey, PublicKey spKey) {
        boolean success = true;
        
        try (FileReader fr = new FileReader(attestation);
             BufferedReader br = new BufferedReader(fr)) {
            String chainhash = Config.DEFAULT_CHAINHASH;
            
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
        } catch (NullPointerException ex) {
            success = false;
        } catch (IOException ex) {
            success = false;
            
            Logger.getLogger(ChainHashClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return success;
    }
    
    public static void main(String[] args) {
        KeyPair keypair = Config.KeyPair.CLIENT.getKeypair();
        KeyPair spKeypair = Config.KeyPair.SERVICE_PROVIDER.getKeypair();
        ChainHashClient client = new ChainHashClient(keypair, spKeypair);
        Operation op = new Operation(OperationType.DOWNLOAD, Config.FILE.getName(), "");
        
        System.out.println("Running:");
        
        long time = System.currentTimeMillis();
        for (int i = 1; i <= Config.NUM_RUNS; i++) {
            client.run(op);
        }
        String chainhash = client.getLastChainHash();
        
        time = System.currentTimeMillis() - time;
        
        System.out.println(Config.NUM_RUNS + " times cost " + (time - client.attestationCollectTime) + "ms (without collect attestations)");
        System.out.println("Collect attestations cost " + client.attestationCollectTime + "ms");
        
        System.out.println("Auditing:");
        
        client.run(new Operation(OperationType.AUDIT,
                                 ChainHashHandler.ATTESTATION.getPath(),
                                 ""));
        
        File auditFile = new File(Config.DOWNLOADS_DIR_PATH + '/' + ChainHashHandler.ATTESTATION.getPath());
        
        // to prevent ClassLoader's init overhead
        client.audit(chainhash, auditFile, keypair.getPublic(), spKeypair.getPublic());
        
        time = System.currentTimeMillis();
        boolean audit = client.audit(chainhash,
                                     auditFile,
                                     keypair.getPublic(),
                                     spKeypair.getPublic());
        time = System.currentTimeMillis() - time;
        
        System.out.println("Audit: " + audit + ", cost " + time + "ms");
    }
}
