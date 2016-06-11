package client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyPair;
import java.security.SignatureException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import message.Operation;
import message.OperationType;
import service.Config;

/**
 * A base Client class for all CAPs.
 * 
 * @author Scott
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
    
    protected ExecutorService pool;
    
    public Client(String hostname,
                  int port,
                  KeyPair keyPair,
                  KeyPair spKeyPair,
                  boolean supportConcurrency) {
        this.hostname = hostname;
        this.port = port;
        this.keyPair = keyPair;
        this.spKeyPair = spKeyPair;
        
        if (Config.ENABLE_MULTITHREAD_EXECUTING && supportConcurrency) {
            this.pool = Executors.newFixedThreadPool(Config.NUM_PROCESSORS);
        } else {
            this.pool = Executors.newSingleThreadExecutor();
        }
    }
    
    protected abstract void handle(Operation op,
                                   Socket socket,
                                   DataOutputStream out,
                                   DataInputStream in)
        throws SignatureException, IllegalAccessException;
    
    public final void execute(Operation op) {
        try (Socket socket = new Socket(hostname, port);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {
            handle(op, socket, out, in);

            socket.close();
        } catch (IOException | SignatureException | IllegalAccessException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }
    
    public abstract String getHandlerAttestationPath();
    
    public abstract boolean audit(File spFile);
    
    public void run(final List<Operation> operations, final int runTimes) {
        System.out.println("Running:");
        
        final HashMap<String, ReentrantLock> lockTable = new HashMap<>();
        
        for (Operation op : operations) {
            String id = op.getClientID();
            
            if (!lockTable.containsKey(id)) {
                lockTable.put(id, new ReentrantLock());
            }
        }
        
        long time = System.currentTimeMillis();
        for (int i = 1; i <= runTimes; i++) {
            final int x = i;
            pool.execute(() -> {
                Operation op = operations.get(x % operations.size());
                ReentrantLock lock = lockTable.get(op.getClientID());
                
                lock.lock();
                try {
                    execute(op);
                } finally {
                    lock.unlock();
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
        
        System.out.println("Auditing:");
        
        String handlerAttestationPath = getHandlerAttestationPath();
        
        execute(new Operation(OperationType.AUDIT, handlerAttestationPath, ""));
        
        File auditFile = new File(Config.DOWNLOADS_DIR_PATH + '/' + handlerAttestationPath);
        
        time = System.currentTimeMillis();
        boolean audit = audit(auditFile);
        time = System.currentTimeMillis() - time;
        
        System.out.println("Audit: " + audit + ", cost " + time + "ms");
    }
}
