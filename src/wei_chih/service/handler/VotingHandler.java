package wei_chih.service.handler;

import wei_chih.message.voting.Request;
import wei_chih.message.voting.Acknowledgement;
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
import service.Key;
import service.KeyManager;
import service.handler.ConnectionHandler;
import wei_chih.service.Config;
import wei_chih.service.SocketServer;
import wei_chih.utility.*;

/**
 *
 * @author Chienweichih
 */
public class VotingHandler extends ConnectionHandler {
    private static final ReentrantLock LOCK;
    
    private static final MerkleTree[] merkleTree;
    private static final String[] digestBeforeUpdate;
    private static final Operation[] lastOP;
    private static final Integer[] sequenceNumbers;

    static {
        merkleTree = new MerkleTree[Config.SERVICE_NUM];
        digestBeforeUpdate = new String[Config.SERVICE_NUM];
        lastOP = new Operation[Config.SERVICE_NUM];
        sequenceNumbers = new Integer[Config.SERVICE_NUM];
        
        for (int i = 0; i < Config.SERVICE_NUM; ++i) {
            merkleTree[i] = new MerkleTree(new File(SocketServer.dataDirPath));
            digestBeforeUpdate[i] = "";
            lastOP[i] = new Operation(OperationType.DOWNLOAD, "", merkleTree[i].getRootHash());
            sequenceNumbers[i] = 0;
        }

        LOCK = new ReentrantLock();
    }
    
    public VotingHandler(Socket socket, KeyPair keyPair) {
        super(socket, keyPair);
    }

    @Override
    protected void handle(DataOutputStream out, DataInputStream in) {
        PublicKey clientPubKey = KeyManager.getInstance().getPublicKey(Key.CLIENT);
        
        int portIndex = 0;
        if (Math.abs(socket.getPort() - Config.SERVICE_PORT[0]) < 10) {
            portIndex = socket.getPort() - Config.SERVICE_PORT[0];
        } else if (Math.abs(socket.getLocalPort() - Config.SERVICE_PORT[0]) < 10) {
            portIndex = socket.getLocalPort() - Config.SERVICE_PORT[0];
        }
        
        try {
            Request req = Request.parse(Utils.receive(in));
            
            LOCK.lock();
            
            if (!req.validate(clientPubKey)) {
                throw new SignatureException("REQ validation failure");
            }
            
            Operation op = req.getOperation();
            
            switch(op.getType()) {
                case UPLOAD:
                    digestBeforeUpdate[portIndex] = merkleTree[portIndex].getDigest(op.getPath());
                    merkleTree[portIndex].update(op.getPath(), op.getMessage());
                case DOWNLOAD:
                    // both upload and download, so no break
                    if (0 != op.getClientID().compareTo(String.valueOf(sequenceNumbers[portIndex]))) {
                        throw new java.security.InvalidParameterException();
                    }
                    sequenceNumbers[portIndex]++;
                default:
            }
            
            File file = new File(SocketServer.dataDirPath + op.getPath());
            
            String rootHash = merkleTree[portIndex].getRootHash();
            String fileHash = null;
            if (file.exists()) {
                fileHash = Utils.digest(file, Config.DIGEST_ALGORITHM);
            }
            
            Acknowledgement ack = new Acknowledgement(rootHash, fileHash, req);
            ack.sign(keyPair);
            Utils.send(out, ack.toString());
            
            switch (op.getType()) {
                case DOWNLOAD:
                    lastOP[portIndex] = op;
                    
                    if (portIndex + Config.SERVICE_PORT[0] == Config.SERVICE_PORT[0]) {
                        Utils.send(out, file);
                    }
                    break;
                case UPLOAD:
                    lastOP[portIndex] = op;
                    
                    if (portIndex + Config.SERVICE_PORT[0] == Config.SERVICE_PORT[0]) {
                        file = new File(Config.DOWNLOADS_DIR_PATH + op.getPath());
                        Utils.receive(in, file);
                        String digest = Utils.digest(file, Config.DIGEST_ALGORITHM);

                        if (0 != op.getMessage().compareTo(digest)) {
                            throw new java.io.IOException();
                        }
                    }
                    break;
                case AUDIT:
                    file = new File(Config.ATTESTATION_DIR_PATH + "/service-provider/voting");
                    
                    switch (lastOP[portIndex].getType()) {
                        case DOWNLOAD:
                            Utils.write(file, rootHash);
                            break;
                        case UPLOAD:
                            MerkleTree prevMerkleTree = new MerkleTree(merkleTree[portIndex]);
                            prevMerkleTree.update(lastOP[portIndex].getPath(), digestBeforeUpdate[portIndex]);
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
