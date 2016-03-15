package wei_chih.service.handler.twostep;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import message.Operation;
import message.OperationType;
import service.handler.ConnectionHandler;
import wei_chih.message.twostep.voting.*;
import wei_chih.service.Config;
import wei_chih.service.SocketServer;
import wei_chih.utility.*;

/**
 *
 * @author Chienweichih
 */
public class VotingHandler implements ConnectionHandler {
    private static final ReentrantLock LOCK;
    
    private static final MerkleTree merkleTree;
    private static String digestBeforeUpdate;
    private static Operation lastOP;
    
    private final Socket socket;
    private final KeyPair keyPair;
    
    static {
        merkleTree = new MerkleTree(new File(SocketServer.dataDirPath));
        digestBeforeUpdate = "";
        lastOP = new Operation(OperationType.DOWNLOAD, "", merkleTree.getRootHash());
        LOCK = new ReentrantLock();
    }
    
    public VotingHandler(Socket socket, KeyPair keyPair) {
        this.socket = socket;
        this.keyPair = keyPair;
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
            
            Operation op = req.getOperation();
            
            if (op.getType() == OperationType.UPLOAD) {
                digestBeforeUpdate = merkleTree.getDigest(op.getPath());
                merkleTree.update(op.getPath(), op.getMessage());
            }
            
            File file = new File(SocketServer.dataDirPath + op.getPath());
            
            String rootHash = merkleTree.getRootHash();
            String fileHash = null;
            if (file.exists()) {
                fileHash = Utils.digest(file);
            }
            
            Acknowledgement ack = new Acknowledgement(rootHash, fileHash, req);
            ack.sign(keyPair);
            Utils.send(out, ack.toString());
                        
            switch (op.getType()) {
                case DOWNLOAD:
                    lastOP = op;
                    
                    if (socket.getPort() == Config.SERVICE_PORT[0] ||
                        socket.getLocalPort() == Config.SERVICE_PORT[0]) {
                        Utils.send(out, file);
                    }
                    break;
                case UPLOAD:
                    lastOP = op;
                    
                    file = new File(Config.DOWNLOADS_DIR_PATH + op.getPath());
                    Utils.receive(in, file);
                    String digest = Utils.digest(file);

                    if (op.getMessage().equals(digest) == false) {
                        throw new java.io.IOException();
                    }
                    break;
                case AUDIT:
                    file = new File(Config.ATTESTATION_DIR_PATH + "/service-provider/voting");
                    
                    switch (lastOP.getType()) {
                        case DOWNLOAD:
                            Utils.write(file, rootHash);
                            break;
                        case UPLOAD:
                            MerkleTree prevMerkleTree = new MerkleTree(merkleTree);
                            prevMerkleTree.update(lastOP.getPath(), digestBeforeUpdate);
                            Utils.Serialize(file, prevMerkleTree);
                            break;
                        default:
                            throw new java.lang.Error();
                    }
                    
                    Utils.send(out, file);                    
                    break;
                default:
            }
            socket.close();
        } catch (IOException | SignatureException ex) {
            Logger.getLogger(VotingHandler.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (LOCK != null) {
                LOCK.unlock();
            }
        }
    }
}
