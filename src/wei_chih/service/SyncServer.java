package wei_chih.service;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import message.OperationType;
import service.handler.ConnectionHandler;
import wei_chih.message.twostep.voting.*;
import wei_chih.utility.*;

/**
 *
 * @author chienweichih
 */
public class SyncServer implements ConnectionHandler {
    private static final ReentrantLock LOCK;
    
    protected static final int[] SERVER_PORTS;
    protected static final int SYNC_PORT;
    
    private static String rootHash;
    private static final Map<Integer, Acknowledgement> lastAcks;
    
    private final Socket socket;
    
    static {
        LOCK = new ReentrantLock();
        
        SERVER_PORTS = new int[Config.SERVICE_NUM];
        for (int i = 0; i < Config.SERVICE_NUM; ++i) {
            SERVER_PORTS[i] = Config.SERVICE_PORT[i];
        }
        
        SYNC_PORT = Config.SERVICE_PORT[Config.SERVICE_NUM];
        
        rootHash = new MerkleTree(new File(SocketServer.dataDirPath)).getRootHash();
        
        lastAcks = new HashMap<>();
        for (int port : SERVER_PORTS) {
            lastAcks.put(port, null);
        }
    }
    
    public SyncServer(Socket socket, KeyPair keyPair) {
        this.socket = socket;
    }
    
    @Override
    public void run() {
        PublicKey clientPubKey = service.KeyPair.CLIENT.getKeypair().getPublic();
        
        try (DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {
            Request req = Request.parse(Utils.receive(in));
            
            LOCK.lock();
            
            File syncAck = new File(Config.DOWNLOADS_DIR_PATH + "/syncLast");
            Map<Integer, String> syncAckStrs = new HashMap<>();
            
            if (!req.validate(clientPubKey)) {
                throw new SignatureException("REQ validation failure");
            }
            
            if (req.getOperation().getType() != OperationType.DOWNLOAD) {
                return;
            }
            
            if (!req.getOperation().getMessage().equals(Config.EMPTY_STRING)) {
                for (int port : SERVER_PORTS) {
                    String lastStr = (lastAcks.get(port) == null) ? null : lastAcks.get(port).toString();
                    syncAckStrs.put(port, lastStr);
                }

                Utils.Serialize(syncAck, syncAckStrs);

                Utils.send(out, syncAck);
                Utils.send(out, rootHash);
            }
            
            // wait until client finish
            
            req = Request.parse(Utils.receive(in));
            
            if (!req.validate(clientPubKey)) {
                throw new SignatureException("REQ validation failure");
            }
            
            if (req.getOperation().getType() != OperationType.UPLOAD) {
                return;
            }
            
            if (!req.getOperation().getMessage().equals(Config.EMPTY_STRING)) {
                Utils.receive(in, syncAck);
                rootHash = Utils.receive(in);
                
                syncAckStrs = Utils.Deserialize(syncAck.getAbsolutePath());

                for (int port : SERVER_PORTS) {
                    if (syncAckStrs.get(port) != null) {
                        lastAcks.replace(port, Acknowledgement.parse(syncAckStrs.get(port)));
                    }
                }
            }
            
            socket.close();
        } catch (IOException | SignatureException ex) {
            Logger.getLogger(SyncServer.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (LOCK != null) {
                LOCK.unlock();
            }
        }
    }
}
