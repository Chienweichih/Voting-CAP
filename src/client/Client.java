package client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyPair;
import java.security.SignatureException;
import java.util.logging.Level;
import java.util.logging.Logger;
import message.Operation;
import message.OperationType;
import service.Config;

/**
 *
 * @author Kitty
 */
public abstract class Client {
    private static final Logger LOGGER;
    
    static {
        LOGGER = Logger.getLogger(Client.class.getName());
    }
    
    protected final String hostname;
    protected final int port;
    protected final KeyPair keyPair;
    protected final KeyPair spKeyPair;
    protected long attestationCollectTime;
    
    public Client(String hostname, int port, KeyPair keyPair, KeyPair spKeyPair) {
        this.hostname = hostname;
        this.port = port;
        this.keyPair = keyPair;
        this.spKeyPair = spKeyPair;
        this.attestationCollectTime = 0;
    }
    
    protected abstract void hook(Operation op,
                                 Socket socket,
                                 DataOutputStream out,
                                 DataInputStream in) throws SignatureException, IllegalAccessException;
    
    public final void run(Operation op) {
        try (Socket socket = new Socket(hostname, port);
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                DataInputStream in = new DataInputStream(socket.getInputStream())) {
            hook(op, socket, out, in);

            socket.close();
        } catch (IOException | SignatureException | IllegalAccessException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }
    
    public abstract String getHandlerAttestationPath();
    
    public abstract boolean audit(File spFile);
    
    public void run(Operation op, int runTimes) {
        System.out.println("Running:");
        
        long time = System.currentTimeMillis();
        for (int i = 1; i <= runTimes; i++) {
            run(op);
        }
        time = System.currentTimeMillis() - time;
        
        System.out.println(runTimes + " times cost " + time + "ms");
        
        System.out.println("Auditing:");
        
        String handlerAttestationPath = getHandlerAttestationPath();
        
        run(new Operation(OperationType.AUDIT, handlerAttestationPath, ""));
        
        File auditFile = new File(Config.DOWNLOADS_DIR_PATH + '/' + handlerAttestationPath);
        
        time = System.currentTimeMillis();
        boolean audit = audit(auditFile);
        time = System.currentTimeMillis() - time;
        
        System.out.println("Audit: " + audit + ", cost " + time + "ms");
    }
}
