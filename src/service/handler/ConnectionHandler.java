package service.handler;

import client.Client;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyPair;
import java.security.SignatureException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Scott
 */
public abstract class ConnectionHandler implements Runnable {
    private static final Logger LOGGER;
    
    static {
        LOGGER = Logger.getLogger(Client.class.getName());
    }
    
    protected final Socket socket;
    protected final KeyPair keyPair;
    
    public ConnectionHandler(Socket socket, KeyPair keyPair) {
        this.socket = socket;
        this.keyPair = keyPair;
    }
    
    protected abstract void handle(DataOutputStream out, DataInputStream in)
            throws SignatureException, IllegalAccessException;
    
    @Override
    public void run() {
        try (DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {
            handle(out, in);
            
            socket.close();
        } catch (IOException | SignatureException | IllegalAccessException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }
}
