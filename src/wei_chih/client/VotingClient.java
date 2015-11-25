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
import wei_chih.service.SyncServer;
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
    
    private long serverProcessTime;
    private long excuteTime;
    
    private final Map<Integer, String> roothashs;
    private final Map<Integer, Acknowledgement> lastAcks;
    private final Map<Integer, Acknowledgement> thisAcks;
    private String result;
    private Acknowledgement acknowledgement;
    
    public VotingClient(KeyPair keyPair, KeyPair spKeyPair) {
        super(Config.SERVICE_HOSTNAME,
              Config.VOTING_SERVICE_PORT_1,
              keyPair,
              spKeyPair,
              Config.NUM_PROCESSORS);
        
        serverProcessTime = 0;
        excuteTime = 0;
        
        lastAcks = new HashMap<>();
        thisAcks = new HashMap<>();
        roothashs = new HashMap<>();
        
        for (int p : SyncServer.PORTS) {
            lastAcks.put(p, null);
            thisAcks.put(p, null);
            roothashs.put(p, null);
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
            Utils.send(out, new File(Config.DATA_DIR_PATH + op.getPath()));
        }
        
        Acknowledgement ack = Acknowledgement.parse(Utils.receive(in));
        serverProcessTime += System.currentTimeMillis() - start;
        ++excuteTime;

        if (!ack.validate(spKeyPair.getPublic())) {
            throw new SignatureException("ACK validation failure");
        }

        result = ack.getResult();
        if (result.equals(Config.AUDIT_FAIL) || 
            result.equals(Config.DOWNLOAD_FAIL) ||
            result.equals(Config.UPLOAD_FAIL)) {
            System.err.println(result);
        }
        
        acknowledgement = ack;
        
        switch (op.getType()) {
            case DOWNLOAD:
                if (op.getMessage().equals(Config.EMPTY_STRING)) {
                    break;
                }
            case AUDIT:
                acknowledgement = null;
                
                File file = new File(Config.DOWNLOADS_DIR_PATH + op.getPath());

                Utils.receive(in, file);

                String digest = Utils.digest(file);

                if (result.equals(digest)) {
                    result = "download success";
                } else {
                    result = "download file digest mismatch";
                    System.err.println(result);
                }
            break;
            default:
        }
    }
    
    public final void execute(Operation op, String hostname, int port) {
        try (Socket socket = new Socket(hostname, port);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {
            hook(op, socket, out, in);

            switch (op.getType()) {
                case DOWNLOAD:
                    if (!op.getMessage().equals(Config.EMPTY_STRING)) {
                        break;
                    }
                case UPLOAD:
                    roothashs.replace(port, result);
                    break;
                default:
            }

            if (acknowledgement != null) {
                lastAcks.replace(port, thisAcks.get(port));
                thisAcks.replace(port, acknowledgement);
            }
            
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
                try (Socket syncSocket = new Socket(Config.SYNC_HOSTNAME, Config.VOTING_SYNC_PORT);
                     DataOutputStream syncOut = new DataOutputStream(syncSocket.getOutputStream());
                     DataInputStream SyncIn = new DataInputStream(syncSocket.getInputStream())) {
                    Operation DOWNLOAD = new Operation(OperationType.DOWNLOAD, Config.EMPTY_STRING, Config.EMPTY_STRING);
                    Operation UPLOAD = new Operation(OperationType.UPLOAD, Config.EMPTY_STRING, Config.EMPTY_STRING);
                    boolean success = syncAtts(DOWNLOAD, syncOut, SyncIn);
                    if (!success) {
                        System.err.println("Sync Error");
                    }
                    
                    Operation op = operations.get(x % operations.size());
                    for (int port_i : SyncServer.PORTS) {
                        execute(op, hostname, port_i);
                    }
                    int diffPort = voting();
                    if (diffPort != -1) {
                        execute(new Operation(OperationType.AUDIT,
                                              File.separator + "ATT_FOR_AUDIT",
                                              Config.EMPTY_STRING),
                                hostname,
                                diffPort);
                        boolean audit = audit(lastAcks.get(diffPort), thisAcks.get(diffPort));
                        System.out.println("Audit: " + audit);
                    }
                    
                    if (op.getType() == OperationType.DOWNLOAD) {
                        // Download from one server
                        execute(new Operation(OperationType.DOWNLOAD,
                                              op.getPath(),
                                              roothashs.get(SyncServer.PORTS[0])),
                                hostname,
                                SyncServer.PORTS[0]);
                    }
                    
                    success = syncAtts(UPLOAD, syncOut, SyncIn);
                    if (!success) {
                        System.err.println("Sync Error");
                    }

                    syncSocket.close();
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
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
        System.out.println("server average process cost " + serverProcessTime / excuteTime + "ms");
        
        System.out.println("Auditing:");
        time = System.currentTimeMillis();
        
        execute(new Operation(OperationType.AUDIT,
                              File.separator + "ATT_FOR_AUDIT",
                              Config.EMPTY_STRING),
                hostname,
                SyncServer.PORTS[0]);
        
        time = System.currentTimeMillis() - time;
        System.out.println("Download attestation, cost " + time + "ms");
        
        try (Socket syncSocket = new Socket(Config.SYNC_HOSTNAME, Config.VOTING_SYNC_PORT);
             DataOutputStream syncOut = new DataOutputStream(syncSocket.getOutputStream());
             DataInputStream SyncIn = new DataInputStream(syncSocket.getInputStream())) {
            Operation DOWNLOAD = new Operation(OperationType.DOWNLOAD, Config.EMPTY_STRING, Config.EMPTY_STRING);
            Operation UPLOAD = new Operation(OperationType.UPLOAD, Config.EMPTY_STRING, Config.EMPTY_STRING);
            boolean success = syncAtts(DOWNLOAD, syncOut, SyncIn);
            if (!success) {
                System.err.println("Sync Error");
            }

            time = System.currentTimeMillis();
            boolean audit = audit(lastAcks.get(SyncServer.PORTS[0]), thisAcks.get(SyncServer.PORTS[0]));
            time = System.currentTimeMillis() - time;
            System.out.println("Audit: " + audit + ", cost " + time + "ms");

            success = syncAtts(UPLOAD, syncOut, SyncIn);
            if (!success) {
                System.err.println("Sync Error");
            }

            syncSocket.close();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public String getHandlerAttestationPath() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public boolean audit(File spFile) {
        return audit(null, null);
    }
    
    public boolean audit(Acknowledgement lastAck, Acknowledgement thisAck) {
        if (lastAck == null || thisAck == null) {
            System.err.println(Config.WRONG_OP);
            return false;
        }
        
        String calResult;
        Operation op = thisAck.getRequest().getOperation();
        
        String attFileName = Config.DOWNLOADS_DIR_PATH + File.separator + "ATT_FOR_AUDIT";
        switch (op.getType()) {
            case DOWNLOAD:
                calResult = Utils.read(attFileName);
                break;
            case UPLOAD:
                MerkleTree merkleTree = Utils.Deserialize(attFileName);
                
                if (!lastAck.getResult().equals(merkleTree.getRootHash())) {
                    return false;
                }
                
                merkleTree.update(op.getPath(), op.getMessage());
                calResult = merkleTree.getRootHash();
                break;
            default:
                calResult = Config.WRONG_OP;
        }
        
        return calResult.equals(thisAck.getResult());
    }
    
    private int voting() {
        HashMap<String, Integer> occurrenceCount = new HashMap<>();
        String currentMaxElement = (String) roothashs.get(SyncServer.PORTS[0]);

        for (String element : roothashs.values()) {
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
        
        for (Integer port_i : roothashs.keySet()) {
            if (!currentMaxElement.equals(roothashs.get(port_i))) {
                return port_i;
            }
        }
        
        return -1;
    }
    
    private boolean syncAtts(Operation op, DataOutputStream out, DataInputStream in) {
        File lastFile = new File(Config.DOWNLOADS_DIR_PATH + File.separator + "clientLast");
        File thisFile = new File(Config.DOWNLOADS_DIR_PATH + File.separator + "clientThis");
        Map<Integer, String> lastAcksStr = new HashMap<>();
        Map<Integer, String> thisAcksStr = new HashMap<>();
                
        Request req = new Request(op);

        req.sign(keyPair);

        Utils.send(out, req.toString());
        
        switch (op.getType()){
            case DOWNLOAD:
                Utils.receive(in, lastFile);
                Utils.receive(in, thisFile);

                lastAcksStr = Utils.Deserialize(lastFile.getAbsolutePath());
                thisAcksStr = Utils.Deserialize(thisFile.getAbsolutePath());

                for (int p : SyncServer.PORTS) {
                    if (lastAcksStr.get(p) != null) {
                        lastAcks.replace(p, Acknowledgement.parse(lastAcksStr.get(p)));
                    }
                    
                    if (thisAcksStr.get(p) != null) {
                        thisAcks.replace(p, Acknowledgement.parse(thisAcksStr.get(p)));
                    }
                }
                break;
            case UPLOAD:
                for (int p : SyncServer.PORTS) {
                    String lastStr = (lastAcks.get(p) == null) ? null : lastAcks.get(p).toString();
                    String thisStr = (thisAcks.get(p) == null) ? null : thisAcks.get(p).toString();

                    lastAcksStr.put(p, lastStr);
                    thisAcksStr.put(p, thisStr);
                }

                Utils.Serialize(lastFile, lastAcksStr);
                Utils.Serialize(thisFile, thisAcksStr);

                Utils.send(out, lastFile);
                Utils.send(out, thisFile);
                break;
            default:
                return false;
        }
        
        return true;
    }
}
