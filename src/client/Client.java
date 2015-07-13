package client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyPair;
import java.security.SignatureException;
import java.util.logging.Level;
import java.util.logging.Logger;
import message.Operation;

/**
 *
 * @author Kitty
 */
public abstract class Client {
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
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            }
    }
}
