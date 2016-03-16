package wei_chih.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyPair;
import java.security.SignatureException;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import client.Client;
import message.Operation;
import wei_chih.message.twostep.nonpov.Acknowledgement;
import wei_chih.message.twostep.nonpov.Request;
import wei_chih.service.Config;
import wei_chih.utility.Utils;

/**
 *
 * @author chienweichih
 */
public class NonPOVClient extends Client {
    public NonPOVClient(KeyPair keyPair, KeyPair spKeyPair) {
        super(Config.SERVICE_HOSTNAME,
              Config.SERVICE_PORT[Config.SERVICE_NUM + 1],
              keyPair,
              spKeyPair,
              Config.NUM_PROCESSORS);
    }
    
    @Override
    public void run(final List<Operation> operations, int runTimes) {
        System.out.println("Running (" + runTimes + " times):");
        
        List<Double> results = new ArrayList<>(); 
        
        for (int i = 1; i <= runTimes; i++) {
            final int x = i; 
            pool.execute(() -> {
                long time = System.currentTimeMillis();
                execute(operations.get(x % operations.size()));
                results.add((System.currentTimeMillis() - time) / 1000.0);
            });
        }
        
        pool.shutdown();
        try {
            pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException ex) {
            Logger.getLogger(NonPOVClient.class.getName()).log(Level.SEVERE, null, ex);
        }

        Collections.sort(results);
        
        if (runTimes < 10) {
            for (double time : results) {
                System.out.printf("%.5f s\n", time);
            }
        } else {
            for (int i = 0; i < 5; ++i) {
                System.out.printf("%.5f s\n", results.get(i));
            }
            
            System.out.println(".");
            System.out.println(".");
            System.out.println(".");
            
            for (int i = 5; i > 0; --i) {
                System.out.printf("%.5f s\n", results.get(results.size() - i));
            }
        }
                
        System.out.println("Audit not supported.");
    }

    @Override
    protected void hook(Operation op, Socket socket, DataOutputStream out, DataInputStream in)
            throws SignatureException {
        Request req = new Request(op);
        req.sign(keyPair);
        Utils.send(out, req.toString());
        
        Acknowledgement ackTemp = Acknowledgement.parse(Utils.receive(in));
        if (!ackTemp.validate(spKeyPair.getPublic())) {
            throw new SignatureException("ACK validation failure");
        }
        
        File file;
        switch (op.getType()) {
                case UPLOAD:
                    file = new File(Experiment.dataDirPath + op.getPath());
                    Utils.send(out, file);
                    break;
                case DOWNLOAD:
                    file = new File(Config.DOWNLOADS_DIR_PATH + op.getPath());
                    Utils.receive(in, file);
                    if (ackTemp.getResult().equals(Utils.digest(file)) == false) {
                        try {
                            throw new java.io.IOException();
                        } catch (IOException ex) {
                            Logger.getLogger(NonPOVClient.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    break;
                default:
            }
    }
    
    @Override
    public String getHandlerAttestationPath() {
        throw new java.lang.UnsupportedOperationException();
    }

    @Override
    public boolean audit(File spFile) {
        throw new java.lang.UnsupportedOperationException();
    }
    
}
