package wei_chih.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyPair;
import java.security.SignatureException;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import client.Client;
import message.Operation;
import message.OperationType;
import wei_chih.message.twostep.voting.*;
import wei_chih.service.Config;
import wei_chih.utility.*;

/**
 *
 * @author Chienweichih
 */
public class VotingClient extends Client {
    private static final Logger LOGGER;
    
    static {
        LOGGER = Logger.getLogger(VotingClient.class.getName());
    }
    
    private Acknowledgement acknowledgement;
    
    private String syncRootHash;
    private final Map<Integer, Acknowledgement> syncAcks;
    private final Map<Integer, Acknowledgement> acks;
    
    public VotingClient(KeyPair keyPair, KeyPair spKeyPair) {
        super(Config.SERVICE_HOSTNAME,
              Experiment.SYNC_PORT,
              keyPair,
              spKeyPair,
              Config.NUM_PROCESSORS);
        
        syncRootHash = null;
        syncAcks = new HashMap<>();
        acks = new HashMap<>();
        
        for (int p : Experiment.SERVER_PORTS) {
            syncAcks.put(p, null);
            acks.put(p, null);
        }
    }
    
    @Override
    public void run(final List<Operation> operations, int runTimes) {
        System.out.println("Running (" + runTimes + " times):");
        
        List<Double> results = new ArrayList<>(); 
        
        for (int i = 1; i <= runTimes; i++) {
            final int x = i; 
            pool.execute(() -> {
                long time = System.currentTimeMillis();
                try (Socket syncSocket = new Socket(Config.SYNC_HOSTNAME, Experiment.SYNC_PORT);
                     DataOutputStream syncOut = new DataOutputStream(syncSocket.getOutputStream());
                     DataInputStream SyncIn = new DataInputStream(syncSocket.getInputStream())) {
                    Operation op = operations.get(x % operations.size());
                    
                    boolean syncSuccess = syncAtts(new Operation(OperationType.DOWNLOAD,
                                                                 Config.EMPTY_STRING,
                                                                 (op.getType() == OperationType.UPLOAD)? Config.EMPTY_STRING: "Download Please"), 
                                                   syncOut, 
                                                   SyncIn);
                    if (!syncSuccess) {
                        System.err.println("Sync Error");
                    }
                    
                    ///////////////////////////////////////////////////////////////////////////////////////////////////
                    
                    int diffPort = execute(op, Experiment.SERVER_PORTS[0]);
                    if (diffPort != -1) {
                        execute(new Operation(OperationType.AUDIT,
                                              "/ATT_FOR_AUDIT",
                                              Config.EMPTY_STRING),
                                diffPort);
                        boolean audit = audit(diffPort, op, acks.get(diffPort).getResult());
                        System.out.println("Audit: " + audit);
                    }
                    
                    syncRootHash = acks.get(Experiment.SERVER_PORTS[0]).getResult();
                    
                    ///////////////////////////////////////////////////////////////////////////////////////////////////
                    
                    syncSuccess = syncAtts(new Operation(OperationType.UPLOAD,
                                                         Config.EMPTY_STRING,
                                                         (op.getType() == OperationType.UPLOAD)? "Upload Please" : Config.EMPTY_STRING), 
                                           syncOut, 
                                           SyncIn);
                    if (!syncSuccess) {
                        System.err.println("Sync Error");
                    }

                    syncSocket.close();
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                } finally {
                    results.add((System.currentTimeMillis() - time) / 1000.0);
                }                
            });
        }
        
        pool.shutdown();
        try {
            pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
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
        
        runAudit();
    }
    
    private void runAudit() {
        System.out.println("Auditing:");
        
        long time = System.currentTimeMillis();
        execute(new Operation(OperationType.AUDIT,
                              "/ATT_FOR_AUDIT",
                              Config.EMPTY_STRING),
                Experiment.SERVER_PORTS[0]);
        System.out.println("Download attestation, cost " + (System.currentTimeMillis() - time)/1000.0 + " s");
        
        try (Socket syncSocket = new Socket(Config.SYNC_HOSTNAME, Experiment.SYNC_PORT);
             DataOutputStream syncOut = new DataOutputStream(syncSocket.getOutputStream());
             DataInputStream SyncIn = new DataInputStream(syncSocket.getInputStream())) {
            
            boolean syncSuccess = syncAtts(new Operation(OperationType.DOWNLOAD,
                                                         Config.EMPTY_STRING,
                                                         "Download Please"), 
                                           syncOut, 
                                           SyncIn);
            if (!syncSuccess) {
                System.err.println("Sync Error");
            }

            ///////////////////////////////////////////////////////////////////////////////////////////////////
            
            time = System.currentTimeMillis();
            int testPort = Experiment.SERVER_PORTS[0];
            boolean audit = audit(testPort, acks.get(testPort).getRequest().getOperation(), acks.get(testPort).getResult());
            System.out.println("Audit: " + audit + ", cost " + (System.currentTimeMillis() - time)/1000.0 + " s");
            
            ///////////////////////////////////////////////////////////////////////////////////////////////////
            
            syncSuccess = syncAtts(new Operation(OperationType.UPLOAD,
                                                 Config.EMPTY_STRING,
                                                 Config.EMPTY_STRING), 
                                   syncOut, 
                                   SyncIn);
            if (!syncSuccess) {
                System.err.println("Sync Error");
            }

            syncSocket.close();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }
    
    public final int execute(Operation op, int auditPort) {
        if (op.getType() == OperationType.AUDIT) {
            try (Socket socket = new Socket(hostname, auditPort);
                 DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                 DataInputStream in = new DataInputStream(socket.getInputStream())) {
                hook(op, socket, out, in);
                socket.close();
            } catch (IOException | SignatureException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
            return -1;
        }
        
        Map<Integer, String> results = new HashMap<>();
                
        for (int p : Experiment.SERVER_PORTS) {
            try (Socket socket = new Socket(hostname, p);
                 DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                 DataInputStream in = new DataInputStream(socket.getInputStream())) {
                if (op.getType() == OperationType.DOWNLOAD &&
                    p == Experiment.SERVER_PORTS[0]) {
                    // Download from one server
                    op = new Operation(OperationType.DOWNLOAD,
                                       op.getPath(),
                                       syncRootHash);
                }
                hook(op, socket, out, in);

                acks.replace(p, acknowledgement);
                results.put(p, acknowledgement.getResult());
                
                socket.close();
            } catch (IOException | SignatureException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
        
        return voting(results);
    }
    
    @Override
    protected void hook(Operation op, Socket socket, DataOutputStream out, DataInputStream in) 
            throws SignatureException {
        Request req = new Request(op);
        req.sign(keyPair);
        Utils.send(out, req.toString());
        
        if (op.getType() == OperationType.UPLOAD) {
            Utils.send(out, new File(Experiment.dataDirPath + op.getPath()));
        }
        
        Acknowledgement ackTemp = Acknowledgement.parse(Utils.receive(in));

        if (!ackTemp.validate(spKeyPair.getPublic())) {
            throw new SignatureException("ACK validation failure");
        }

        String result = ackTemp.getResult();
        if (result.equals(Config.AUDIT_FAIL) || 
            result.equals(Config.DOWNLOAD_FAIL) ||
            result.equals(Config.UPLOAD_FAIL)) {
            System.err.println(result);
        }
        
        acknowledgement = ackTemp;
        
        switch (op.getType()) {
            case DOWNLOAD:
                if (op.getMessage().equals(Config.EMPTY_STRING)) {
                    break;
                }
            case AUDIT:
                File file = new File(Config.DOWNLOADS_DIR_PATH + op.getPath());
                Utils.receive(in, file);
            break;
            default:
        }
    }

    @Override
    public boolean audit(File spFile) {
        return audit(0, null, null);
    }
    
    public boolean audit(int port, Operation op, String roothash) {
        if (op == null || roothash == null) {
            return false;
        }
        
        String calResult;
        String attFileName = Config.DOWNLOADS_DIR_PATH + "/ATT_FOR_AUDIT";
        
        switch (op.getType()) {
            case DOWNLOAD:
                calResult = Utils.read(attFileName);
                break;
            case UPLOAD:
                MerkleTree merkleTree = Utils.Deserialize(attFileName);
                
                if (!syncRootHash.equals(merkleTree.getRootHash())) {
                    return false;
                }
                
                merkleTree.update(op.getPath(), op.getMessage());
                calResult = merkleTree.getRootHash();
                break;
            default:
                calResult = Config.WRONG_OP;
        }
        
        return calResult.equals(roothash);
    }
    
    private int voting(Map<Integer, String> inputs) {
        Map<String, Integer> occurrenceCount = new HashMap<>();
        String currentMaxElement = (String) inputs.get(Experiment.SERVER_PORTS[0]);

        for (String element : inputs.values()) {
            Integer elementCount = occurrenceCount.get(element);
            if (elementCount != null) {
                occurrenceCount.put(element, elementCount + 1);
                if (elementCount >= occurrenceCount.get(currentMaxElement)) {
                    currentMaxElement = element;
                }
            } else {
                occurrenceCount.put(element, 1);
            }
        }
        
        for (Integer port_i : inputs.keySet()) {
            if (!currentMaxElement.equals(inputs.get(port_i))) {
                return port_i;
            }
        }
        
        return -1;
    }
    
    private boolean syncAtts(Operation op, DataOutputStream out, DataInputStream in) {
        File syncAck = new File(Config.DOWNLOADS_DIR_PATH + "/syncAck");
        Map<Integer, String> syncAckStrs = new HashMap<>();
        
        Request req = new Request(op);
        req.sign(keyPair);
        Utils.send(out, req.toString());
        
        if (op.getMessage().equals(Config.EMPTY_STRING)) {
            return true;
        }
        
        switch (op.getType()){
            case DOWNLOAD:
                Utils.receive(in, syncAck);
                syncRootHash = Utils.receive(in);
                
                syncAckStrs = Utils.Deserialize(syncAck.getAbsolutePath());

                for (int p : Experiment.SERVER_PORTS) {
                    if (syncAckStrs.get(p) != null) {
                        syncAcks.replace(p, Acknowledgement.parse(syncAckStrs.get(p)));
                    }
                }
                break;
            case UPLOAD:
                for (int p : Experiment.SERVER_PORTS) {
                    String lastStr = (syncAcks.get(p) == null) ? null : syncAcks.get(p).toString();
                    syncAckStrs.put(p, lastStr);
                }

                Utils.Serialize(syncAck, syncAckStrs);

                Utils.send(out, syncAck);
                Utils.send(out, syncRootHash);
                break;
            default:
                return false;
        }
        
        return true;
    }
    
    @Override
    public String getHandlerAttestationPath() {
        throw new java.lang.UnsupportedOperationException();
    }
}
