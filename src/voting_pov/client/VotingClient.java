package voting_pov.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyPair;
import java.security.SignatureException;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import client.Client;
import message.Operation;
import message.OperationType;
import voting_pov.message.twostep.voting.*;
import voting_pov.service.Config;
import voting_pov.utility.MerkleTree;
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
    
    private class Roothash {
        public String voting;
        public String auditing;
        
        public Roothash(String v, String a) {
            voting = v;
            auditing = a;
        }
    }
    private final Map<Integer, Roothash> roothash;
    private final int[] ports;
    private String result;
    private long serverProcessTime;
    
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
        
        serverProcessTime = 0;
        
        String attestationPath = getHandlerAttestationPath();
        try {
            MerkleTree.create(Config.DATA_DIR_PATH,
                              attestationPath,
                              true);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        
        String roothashValue = Utils.readDigest(attestationPath);
        roothash = new HashMap<>();
        for (int p : ports) {
            roothash.put(p, new Roothash(roothashValue, roothashValue));
        }
    }
        
    @Override
    protected void hook(Operation op, Socket socket, DataOutputStream out, DataInputStream in) 
           throws SignatureException {
        Request req = new Request(op);

        req.sign(keyPair);

        long start = System.currentTimeMillis();
        Utils.send(out, req.toString());
        
        if (op.getType() == OperationType.UPLOAD) {
            Utils.send(out, new File(Config.DATA_DIR_PATH + File.separator + op.getPath()));
        }
        
        Acknowledgement ack = Acknowledgement.parse(Utils.receive(in));
        serverProcessTime += System.currentTimeMillis() - start;

        if (!ack.validate(spKeyPair.getPublic())) {
            throw new SignatureException("ACK validation failure");
        }

        result = ack.getResult();
        if (result.equals(Config.AUDIT_FAIL) || 
            result.equals(Config.DOWNLOAD_FAIL) ||
            result.equals(Config.UPLOAD_FAIL)) {
            System.err.println(result);
        }
        
        String fname = null;
        if (op.getType() == OperationType.DOWNLOAD &&
            !op.getMessage().equals(Config.EMPTY_STRING)) {
            fname = Config.DOWNLOADS_DIR_PATH + File.separator + op.getPath();
        } else if (op.getType() == OperationType.AUDIT) {
            fname = Config.ATTESTATION_DIR_PATH + File.separator + "client" + File.separator + "voting";
        }
        
        if (fname != null) {
            File file = new File(fname);

            Utils.receive(in, file);

            String digest = Utils.digest(file);

            if (result.equals(digest)) {
                result = "download success";
            } else {
                result = "download file digest mismatch";
            }
        }
        
        if (op.getType() == OperationType.AUDIT) {
            String dest = Config.ATTESTATION_DIR_PATH + File.separator + "client";
            Utils.clearDirectory(new File(dest + File.separator + "data_HASH"));
            Utils.unZip(dest, dest + File.separator + "voting");
        }
    }
    
    public final void execute(Operation op, String hostname, int port) {
        try (Socket socket = new Socket(hostname, port);
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                DataInputStream in = new DataInputStream(socket.getInputStream())) {
            hook(op, socket, out, in);
            
            Roothash temp = roothash.get(port);
            if (op.getType() == OperationType.DOWNLOAD &&
                op.getMessage().equals(Config.EMPTY_STRING)) {
                temp.voting = result;
            } else if (op.getType() == OperationType.UPLOAD) {
                temp.auditing = temp.voting;
                temp.voting = result;
            }
            roothash.put(port, temp);

            socket.close();
        } catch (IOException | SignatureException ex) {
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
                int diffPort = voting();
                if (diffPort != -1) {
                    String fname = op.getPath();
                    File auditFile = new File(Config.DOWNLOADS_DIR_PATH + File.separator + fname);
                    execute(new Operation(OperationType.AUDIT,
                                          Config.EMPTY_STRING,
                                          Config.EMPTY_STRING),
                            hostname,
                            diffPort);
                    boolean audit = audit(auditFile,
                                          getHandlerAttestationPath() + File.separator + fname + ".digest",
                                          roothash.get(diffPort).auditing);
                    System.out.println("Audit: " + audit);
                }
                if (op.getType() == OperationType.DOWNLOAD) {
                    // Download from one server
                    execute(new Operation(OperationType.DOWNLOAD,
                                          op.getPath(),
                                          roothash.get(ports[0]).voting),
                            hostname,
                            ports[0]);
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
        System.out.println("server average process cost " + serverProcessTime / runTimes + "ms");
        
        System.out.println("Auditing:");
        
        String fname = operations.get(runTimes % operations.size()).getPath();
        File auditFile = new File(Config.DOWNLOADS_DIR_PATH + File.separator + fname);
        
        time = System.currentTimeMillis();
        execute(new Operation(OperationType.AUDIT,
                              Config.EMPTY_STRING,
                              Config.EMPTY_STRING),
                hostname,
                ports[0]);
        time = System.currentTimeMillis() - time;
        System.out.println("Download attestation, cost " + time + "ms");
        
        time = System.currentTimeMillis();
        boolean audit = audit(auditFile,
                              getHandlerAttestationPath() + File.separator + fname + ".digest",
                              roothash.get(ports[0]).auditing);
        time = System.currentTimeMillis() - time;
        System.out.println("Audit: " + audit + ", cost " + time + "ms");
    }

    @Override
    public String getHandlerAttestationPath() {
        return Config.ATTESTATION_DIR_PATH + File.separator + "client" + File.separator +"data_HASH";
    }

    @Override
    public boolean audit (File spFile) {
        String attestationPath = getHandlerAttestationPath();
        return audit(spFile,
                     attestationPath + ".digest",
                     Utils.readDigest(attestationPath));
    }
    
    public boolean audit (File spFile, String filePath, String lastResult) {
        String attestationPath = getHandlerAttestationPath();
        MerkleTree.update(attestationPath,
                          filePath,
                          Utils.digest(spFile));
        return Utils.readDigest(attestationPath).equals(lastResult);
    }
    
    private int voting() {
        if (roothash.size() == 1) {
            return -1;
        }
        
        Integer prev = null;
        
        for (Iterator<Integer> iter = roothash.keySet().iterator(); iter.hasNext(); ) {
            Integer element = iter.next();
            
            String elementHash = roothash.get(element).auditing;
            if (prev != null &&
                !elementHash.equals(roothash.get(prev).auditing)) {
                if (roothash.size() == 2) {
                    return element;
                }
                if (!elementHash.equals(roothash.get(iter.next()).auditing)) {
                    return prev;
                }
                return element;
            }
            
            prev = element;
        }
        return -1;
    }
}
