package service;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import service.handler.ConnectionHandler;
import service.handler.NonCAPHandler;
import service.handler.fourstep.ChainHashAndLSNHandler;
import service.handler.fourstep.DoubleChainHashHandler;
import service.handler.twostep.CSNHandler;
import service.handler.twostep.ChainHashHandler;
import utility.Utils;

/**
 *
 * @author Scott
 */
public class SocketServer extends Thread {
    private int port;
    private ServerSocket serverSocket;
    private ExecutorService pool;
    private Constructor handlerCtor;
    private KeyPair keyPair;
    
    public SocketServer(Class handler) {
        this(handler, 3000);
    }
    
    public SocketServer(Class<? extends ConnectionHandler> handler, int port) {
        this.port = port;
        
        try {
            this.handlerCtor = handler.getDeclaredConstructor(Socket.class, KeyPair.class);
        } catch (NoSuchMethodException | SecurityException ex) {
            Logger.getLogger(SocketServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        keyPair = KeyManager.getInstance().getKeyPair(Key.SERVICE_PROVIDER);
    }
    
    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            pool = Executors.newFixedThreadPool(Config.NUM_PROCESSORS);
            
            do {
                Socket socket = serverSocket.accept();
                
                pool.execute((Runnable) handlerCtor.newInstance(socket, keyPair));
            } while (true);
        } catch (IOException ex) {
            Logger.getLogger(SocketServer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            Logger.getLogger(SocketServer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(SocketServer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(SocketServer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvocationTargetException ex) {
            Logger.getLogger(SocketServer.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                serverSocket.close();
                pool.shutdown();
            } catch (IOException ex) {
                Logger.getLogger(SocketServer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public static void main(String[] args) {
        Utils.createRequiredFiles();
        Utils.cleanAllAttestations();
        
        new SocketServer(NonCAPHandler.class, Config.NONCAP_SERVICE_PORT).start();
        new SocketServer(CSNHandler.class, Config.CSN_SERVICE_PORT).start();
        new SocketServer(ChainHashHandler.class, Config.CHAINHASH_SERVICE_PORT).start();
        new SocketServer(ChainHashAndLSNHandler.class, Config.CHAINHASH_LSN_SERVICE_PORT).start();
        new SocketServer(DoubleChainHashHandler.class, Config.DOUBLECHAINHASH_SERVICE_PORT).start();
        
        System.out.println("Ready to go!");
    }
}
