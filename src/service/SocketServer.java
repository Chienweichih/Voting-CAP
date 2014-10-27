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
import service.handler.NonPOVHandler;
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
    private int numThreads;
    private ServerSocket serverSocket;
    private ExecutorService pool;
    private Class<? extends ConnectionHandler> handlerClass;
    private Constructor handlerCtor;
    private KeyPair keyPair;
    
    public SocketServer(Class handler) {
        this(handler, 3000);
    }
    
    public SocketServer(Class<? extends ConnectionHandler> handler, int port) {
        this.port = port;
        this.numThreads = Runtime.getRuntime().availableProcessors();
        this.handlerClass = handler;
        
        try {
            this.handlerCtor = handler.getDeclaredConstructor(Socket.class, KeyPair.class);
        } catch (NoSuchMethodException | SecurityException ex) {
            Logger.getLogger(SocketServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        keyPair = Utils.readKeyPair("service_provider.key");
    }
    
    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            pool = Executors.newFixedThreadPool(numThreads);
            
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
        Utils.cleanAllAttestations();
        
        new SocketServer(NonPOVHandler.class, Config.NONPOV_SERVICE_PORT).start();
        new SocketServer(CSNHandler.class, Config.CSN_SERVICE_PORT).start();
        new SocketServer(ChainHashHandler.class, Config.CHAINHASH_SERVICE_PORT).start();
        new SocketServer(ChainHashAndLSNHandler.class, Config.CHAINHASH_LSN_SERVICE_PORT).start();
        new SocketServer(DoubleChainHashHandler.class, Config.DOUBLECHAINHASH_SERVICE_PORT).start();
    }
}
