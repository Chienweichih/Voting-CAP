package voting_pov.service;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
import voting_pov.message.twostep.voting.*;
import voting_pov.utility.Utils;

/**
 *
 * @author chienweichih
 */
public class SyncServer implements ConnectionHandler {
    private static final ReentrantLock LOCK;
    public static final int[] PORTS;
    
    private final Map<Integer, Acknowledgement> lastAcks;
    private final Map<Integer, Acknowledgement> thisAcks;
    private final Socket socket;
    
    static {
        LOCK = new ReentrantLock();
        
        PORTS = new int[]{Config.VOTING_SERVICE_PORT_1,
                          Config.VOTING_SERVICE_PORT_2,
                          Config.VOTING_SERVICE_PORT_3,
                          Config.VOTING_SERVICE_PORT_4,
                          Config.VOTING_SERVICE_PORT_5};
    }
    
    public SyncServer(Socket socket, KeyPair keyPair) {
        lastAcks = new HashMap<>();
        thisAcks = new HashMap<>();
        
        for (int port : PORTS) {
            lastAcks.put(port, null);
            thisAcks.put(port, null);
        }
        
        this.socket = socket;
    }
    
    @Override
    public void run() {
        PublicKey clientPubKey = service.KeyPair.CLIENT.getKeypair().getPublic();
        
        try (DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {
            Request req = Request.parse(Utils.receive(in));
            
            LOCK.lock();
            
            if (!req.validate(clientPubKey)) {
                throw new SignatureException("REQ validation failure");
            }
            
            if (req.getOperation().getType() != OperationType.DOWNLOAD) {
                return;
            }
            
            File lastFile = new File(Config.DOWNLOADS_DIR_PATH + File.separator + "syncLast");
            File thisFile = new File(Config.DOWNLOADS_DIR_PATH + File.separator + "syncThis");
            
            Map<Integer, String> lastAcksStr = new HashMap<>();
            Map<Integer, String> thisAcksStr = new HashMap<>();
            
            for (int port : PORTS) {
                String lastStr = (lastAcks.get(port) == null) ? null : lastAcks.get(port).toString();
                String thisStr = (thisAcks.get(port) == null) ? null : thisAcks.get(port).toString();
                
                lastAcksStr.put(port, lastStr);
                thisAcksStr.put(port, thisStr);
            }
            
            Serialize(lastFile, lastAcksStr);
            Serialize(thisFile, thisAcksStr);
            
            Utils.send(out, lastFile);
            Utils.send(out, thisFile);
            
            req = Request.parse(Utils.receive(in));
            
            if (!req.validate(clientPubKey)) {
                throw new SignatureException("REQ validation failure");
            }
            
            if (req.getOperation().getType() != OperationType.UPLOAD) {
                return;
            }
            
            Utils.receive(in, lastFile);
            Utils.receive(in, thisFile);
            
            lastAcksStr = Deserialize(lastFile.getAbsolutePath());
            thisAcksStr = Deserialize(thisFile.getAbsolutePath());
            
            for (int port : PORTS) {
                if (lastAcksStr.get(port) != null) {
                    lastAcks.replace(port, Acknowledgement.parse(lastAcksStr.get(port)));
                }
                
                if (thisAcksStr.get(port) != null) {
                    thisAcks.replace(port, Acknowledgement.parse(thisAcksStr.get(port)));
                }
            }
            
            socket.close();
        } catch (IOException | SignatureException ex) {
            Logger.getLogger(SyncServer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(SyncServer.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (LOCK != null) {
                LOCK.unlock();
            }
        }
    }
    
    public static void Serialize(File dest, Map<Integer, String> ackStr) {
        try (FileOutputStream fout = new FileOutputStream(dest);
             ObjectOutputStream oos = new ObjectOutputStream(fout)) {   
            oos.writeObject(ackStr);
            oos.close();
        } catch (IOException ex) {
            Logger.getLogger(SyncServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static Map<Integer, String> Deserialize(String src) {
        Map<Integer, String> ackStr = null;
        try (FileInputStream fin = new FileInputStream(src);
             ObjectInputStream ois = new ObjectInputStream(fin)) {
            ackStr = (Map<Integer, String>) ois.readObject();
            ois.close();
        } catch (IOException | ClassNotFoundException ex) {
            Logger.getLogger(SyncServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ackStr;
    }
}
