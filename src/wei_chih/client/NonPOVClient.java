package wei_chih.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.net.Socket;
import java.security.KeyPair;
import java.security.SignatureException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.TimeUnit;

import client.Client;
import message.Operation;
import message.OperationType;
import wei_chih.message.twostep.voting.Acknowledgement;
import wei_chih.message.twostep.voting.Request;
import wei_chih.service.Config;
import wei_chih.utility.Utils;

/**
 *
 * @author chienweichih
 */
public class NonPOVClient extends Client {
    private static final Logger LOGGER;
    
    static {
        LOGGER = Logger.getLogger(NonPOVClient.class.getName());
    }
    
    public NonPOVClient(KeyPair keyPair, KeyPair spKeyPair) {
        super(Config.SERVICE_HOSTNAME,
              Config.SERVICE_PORT[0],
              keyPair,
              spKeyPair,
              Config.NUM_PROCESSORS);
    }

    @Override
    protected void hook(Operation op, Socket socket, DataOutputStream out, DataInputStream in)
            throws SignatureException {
        Request req = new Request(op);
        req.sign(keyPair);
        Utils.send(out, req.toString());
        
        if (op.getType() == OperationType.UPLOAD) {
            Utils.send(out, new File(ExperimentNonPOV.dataDirPath + op.getPath()));
        }
        
        Acknowledgement ackTemp = Acknowledgement.parse(Utils.receive(in));

        if (!ackTemp.validate(spKeyPair.getPublic())) {
            throw new SignatureException("ACK validation failure");
        }

        if (op.getType() == OperationType.DOWNLOAD) {
            Utils.receive(in, new File(Config.DOWNLOADS_DIR_PATH + op.getPath()));
        }
    }

    @Override
    public void run(final List<Operation> operations, int runTimes) {
        System.out.println("Running:");
        
        long time = System.currentTimeMillis();
        for (int i = 1; i <= runTimes; i++) {
            final int x = i; 
            pool.execute(() -> {
                execute(operations.get(x % operations.size()));
            });
        }
        
        pool.shutdown();
        try {
            pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        time = System.currentTimeMillis() - time;
        
        System.out.println(runTimes + " times cost " + time + "ms");
        
        System.out.println("Audit not supported.");
    }

    @Override
    public String getHandlerAttestationPath() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public boolean audit(File spFile) {
        throw new UnsupportedOperationException("Not supported.");
    }
    
}
