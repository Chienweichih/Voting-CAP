package voting_pov.client;

import client.Client;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyPair;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import message.Operation;
import message.OperationType;
import voting_pov.message.twostep.voting.Acknowledgement;
import voting_pov.message.twostep.voting.Request;
import voting_pov.service.Config;
import voting_pov.utility.Utils;

/**
 *
 * @author Chienweichih
 */
public class VotingClient extends Client {
    private static final Logger LOGGER;
    
    static {
        LOGGER = Logger.getLogger(VotingClient.class.getName());
    }
    
    private final int[] ports;
    private String resultTemp;
    private String[] results;
        
    public VotingClient(KeyPair keyPair, KeyPair spKeyPair) {
        super(Config.SERVICE_HOSTNAME,
              Config.VOTING_SERVICE_PORT_1,
              keyPair,
              spKeyPair,
              Config.NUM_PROCESSORS);
        ports = new int[]{Config.VOTING_SERVICE_PORT_1,
                          Config.VOTING_SERVICE_PORT_2,
                          Config.VOTING_SERVICE_PORT_3,
                          Config.VOTING_SERVICE_PORT_4,
                          Config.VOTING_SERVICE_PORT_5};
        resultTemp = "Ctor";
        results = new String[ports.length];
    }
        
    @Override
    protected void hook(Operation op, Socket socket, DataOutputStream out, DataInputStream in) 
            throws SignatureException, IllegalAccessException {
        Request req = new Request(op);

        req.sign(keyPair);

        Utils.send(out, req.toString());

        switch (op.getType()) {
            case UPLOAD:
                Utils.send(out, new File(Config.DATA_DIR_PATH + '/' + op.getPath()));
            break;
            case DOWNLOAD:
                //server do nothing
                if (op.getPath().equals("")) {
                    return;
                }
            break;
        }

        Acknowledgement ack = Acknowledgement.parse(Utils.receive(in));

        if (!ack.validate(spKeyPair.getPublic())) {
            throw new SignatureException("ACK validation failure");
        }

        resultTemp = ack.getResult();

        switch (op.getType()) {
            case AUDIT:
                long start = System.currentTimeMillis();
                //Utils.receive(in, merkleTree);
                this.attestationCollectTime += System.currentTimeMillis() - start;
            break;
            case DOWNLOAD:
                if (op.getMessage().equals(resultTemp)) {
                    File file = new File(Config.DOWNLOADS_DIR_PATH + '/' + op.getPath());

                    Utils.receive(in, file);

                    String digest = Utils.digest(file);

                    if (resultTemp.compareTo(digest) != 0) {
                        resultTemp = "download success";
                    } else {
                        resultTemp = "download file digest mismatch";
                    }
                }
            break;
        }
    }
    
    public final void execute(Operation op, String hostname, int port) {
        try (Socket socket = new Socket(hostname, port);
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                DataInputStream in = new DataInputStream(socket.getInputStream())) {
            hook(op, socket, out, in);
            for (int i = 0;i < ports.length;++i) {
                if (ports[i] == port) {
                    results[i] = resultTemp;
                    break;
                }
            }

            socket.close();
        } catch (IOException | SignatureException | IllegalAccessException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }
    
    @Override
    public void run(final List<Operation> operations, int runTimes) {
        System.out.println("Running:");
        
        long time = System.currentTimeMillis();
        
        for (int i = 1; i <= runTimes; i++) {
            final int x = i; 
            pool.execute(() -> {
                Operation op = operations.get(x % operations.size());
                for (int port_i : ports) {
                    execute(op, hostname, port_i);
                }
                boolean success = voting();
                if (!success) {
                    execute(new Operation(OperationType.AUDIT, "", ""), hostname, ports[0]);
                    boolean audit = audit(null);
                }
                if (op.getType() == OperationType.DOWNLOAD) {
                    // Download from one server
                    execute(new Operation(OperationType.DOWNLOAD, op.getPath(), results[0]), hostname, ports[0]);
                    // Other Server do nothing
                    for (int port_i : Arrays.copyOfRange(ports, 1, ports.length)) {
                        execute(new Operation(OperationType.DOWNLOAD, "", ""), hostname, port_i);
                    }
                }
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
        
        System.out.println("Auditing:");
                
        execute(new Operation(OperationType.AUDIT, "", ""), hostname, ports[0]);
                        
        time = System.currentTimeMillis();
        boolean audit = audit(null);
        time = System.currentTimeMillis() - time;
        
        System.out.println("Audit: " + audit + ", cost " + time + "ms");
    }

    @Override
    public String getHandlerAttestationPath() {
        return "No used in this Client";
    }

    @Override
    public boolean audit(File spFile) {
        boolean success = true;
        //success = merkleTree.update( Utils.digest(spFile) );
        //success &= result.equals( merkleTree.getRootHash );
        
        return success;
    }
    
    private boolean voting() {
        boolean success = true;
        for (int i = 1; i < results.length; ++i) {
            success &= results[i].equals(results[i-1]);
        }
        return success;
    }
}
