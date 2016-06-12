package wei_chih.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyPair;
import java.security.SignatureException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.TimeUnit;

import client.Client;
import message.Operation;
import wei_chih.message.noncap.Acknowledgement;
import wei_chih.message.noncap.Request;
import wei_chih.service.Config;
import wei_chih.utility.Utils;

/**
 *
 * @author chienweichih
 */
public class NonCAPClient extends Client {

    public NonCAPClient(KeyPair keyPair, KeyPair spKeyPair) {
        super(Config.SERVICE_HOSTNAME,
                Config.SERVICE_PORT[Config.SERVICE_NUM + 1],
                keyPair,
                spKeyPair,
                true);
    }

    @Override
    public void run(final List<Operation> operations, int runTimes) {
        System.out.println("Running (" + runTimes + " times):");

        double[] results = new double[runTimes];

        // for best result
        for (int i = 1; i <= runTimes; i++) {
            final int x = i;
            pool.execute(() -> {
                execute(operations.get(x % operations.size()));
            });
        }

        for (int i = 1; i <= runTimes; i++) {
            final int x = i;
            pool.execute(() -> {
                long time = System.nanoTime();
                execute(operations.get(x % operations.size()));
                results[x - 1] = (System.nanoTime() - time) / 1e9;
            });
        }

        pool.shutdown();
        try {
            pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException ex) {
            Logger.getLogger(NonCAPClient.class.getName()).log(Level.SEVERE, null, ex);
        }

        Utils.printExperimentResult(results);

        System.out.println("Audit not supported.");
    }

    @Override
    protected void handle(Operation op, Socket socket, DataOutputStream out, DataInputStream in)
            throws SignatureException {
        Request req = new Request(op);
        req.sign(keyPair);
        Utils.send(out, req.toString());

        Acknowledgement ackTemp = Acknowledgement.parse(Utils.receive(in));
        if (!ackTemp.validate(spKeyPair.getPublic())) {
            throw new SignatureException("ACK validation failure");
        }

        switch (op.getType()) {
            case UPLOAD:
                Utils.send(out, new File(Experiment.dataDirPath + op.getPath()));
                break;
            case DOWNLOAD:
                File file = new File(Config.DOWNLOADS_DIR_PATH + op.getPath());
                Utils.receive(in, file);
                if (0 != ackTemp.getResult().compareTo(Utils.digest(file, Config.DIGEST_ALGORITHM))) {
                    try {
                        throw new java.io.IOException();
                    } catch (IOException ex) {
                        Logger.getLogger(NonCAPClient.class.getName()).log(Level.SEVERE, null, ex);
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
