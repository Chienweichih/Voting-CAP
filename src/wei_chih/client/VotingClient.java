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
    
    private final Map<Integer, Acknowledgement> syncAcks;
    private final Map<Integer, Acknowledgement> acks;
    
    public VotingClient(KeyPair keyPair, KeyPair spKeyPair) {
        super(Config.SERVICE_HOSTNAME,
              Experiment.SYNC_PORT,
              keyPair,
              spKeyPair,
              Config.NUM_PROCESSORS);
        
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
        
        double[] results = new double[runTimes];
        
        // for best result
        for (int i = 1; i <= runTimes; i++) {
            final int x = i; 
            pool.execute(() -> {
                
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
                        boolean audit = audit(diffPort);
                        System.out.println("Audit: " + audit);
                    }
                    
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
                }            
            });
        }
        
        for (int i = 1; i <= runTimes; i++) {
            final int x = i; 
            pool.execute(() -> {
                long time = System.nanoTime();
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
                        boolean audit = audit(diffPort);
                        System.out.println("Audit: " + audit);
                    }
                    
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
                }            
            results[x-1] = (System.nanoTime() - time) / 1e9;
            });
        }
        
        pool.shutdown();
        try {
            pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        
        Utils.printExperimentResult(results);
        
        runAudit();
    }
    
    private void runAudit() {
        System.out.println("Auditing:");
        
        long time = System.nanoTime();
        execute(new Operation(OperationType.AUDIT,
                              "/ATT_FOR_AUDIT",
                              Config.EMPTY_STRING),
                Experiment.SERVER_PORTS[0]);
        System.out.println("Download attestation, cost " + (System.nanoTime() - time)/1e9 + " s");
        
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
            
            time = System.nanoTime();
            int testPort = Experiment.SERVER_PORTS[0];
            boolean audit = audit(testPort);
            System.out.println("Audit: " + audit + ", cost " + (System.nanoTime() - time)/1e9 + " s");
            
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
                hook(op, socket, out, in);

                acks.replace(p, acknowledgement);
                switch (op.getType()) {
                    case DOWNLOAD:
                        results.put(p, acknowledgement.getFileHash());
                    case UPLOAD:
                        results.put(p, acknowledgement.getRoothash());
                }
                
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
        
        acknowledgement = Acknowledgement.parse(Utils.receive(in));

        if (!acknowledgement.validate(spKeyPair.getPublic())) {
            throw new SignatureException("ACK validation failure");
        }
                
        File file = new File(Config.DOWNLOADS_DIR_PATH + op.getPath());
        switch (op.getType()) {
            case DOWNLOAD:
                if (socket.getPort() == Config.SERVICE_PORT[0] ||
                    socket.getLocalPort() == Config.SERVICE_PORT[0]) {
                    Utils.receive(in, file);
                }
                break;
            case UPLOAD:
                Utils.send(out, new File(Experiment.dataDirPath + op.getPath()));
                break;
            case AUDIT:
                Utils.receive(in, file);                
            break;
            default:
        }
    }

    @Override
    public boolean audit(File spFile) {
        return audit(-1);
    }
    
    public boolean audit(int port) {
        if (port == -1) {
            return false;
        }
        
        Acknowledgement lastAck = syncAcks.get(port);
        String lastRootHash = lastAck.getRoothash();
        
        Acknowledgement thisack = acks.get(port);
        String thisRootHash = thisack.getRoothash();
        
        String serverGaveMeRoothash = null;
        String meCalculateRoothash = null;
        
        String attFileName = Config.DOWNLOADS_DIR_PATH + "/ATT_FOR_AUDIT";
        Operation thisOp = thisack.getRequest().getOperation();
        
        switch (thisOp.getType()) {
            case DOWNLOAD:
                serverGaveMeRoothash = Utils.read(attFileName);
                meCalculateRoothash = serverGaveMeRoothash;
                break;
            case UPLOAD:
                MerkleTree merkleTree = Utils.Deserialize(attFileName);
                serverGaveMeRoothash = merkleTree.getRootHash();
                
                merkleTree.update(thisOp.getPath(), thisOp.getMessage());
                meCalculateRoothash = merkleTree.getRootHash();
                
                break;
            default:
        }
        
        return lastRootHash.equals(serverGaveMeRoothash) && thisRootHash.equals(meCalculateRoothash);
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
                
                syncAckStrs = Utils.Deserialize(syncAck.getAbsolutePath());

                for (int p : Experiment.SERVER_PORTS) {
                    if (syncAckStrs.get(p) != null) {
                        syncAcks.replace(p, Acknowledgement.parse(syncAckStrs.get(p)));
                    }
                }
                break;
            case UPLOAD:
                for (int p : Experiment.SERVER_PORTS) {
                    String lastStr = (acks.get(p) == null) ? null : acks.get(p).toString();
                    syncAckStrs.put(p, lastStr);
                }

                Utils.Serialize(syncAck, syncAckStrs);

                Utils.send(out, syncAck);
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
