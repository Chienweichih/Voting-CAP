package wei_shian.service.handler.twostep;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import message.Operation;
import service.handler.ConnectionHandler;
import wei_chih.utility.MerkleTree;
import wei_chih.utility.Utils;
import wei_shian.message.twostep.voting.Acknowledgement;
import wei_shian.message.twostep.voting.Request;
import wei_shian.service.Config;
import wei_shian.service.SocketServer;

/**
 *
 * @author Chienweichih
 */
public class WeiShianHandler implements ConnectionHandler {
    private static final ReentrantLock LOCK;
    
    private static final MerkleTree merkleTree;
    private static final LinkedList<String> ACKChain;
    
    private final Socket socket;
    private final KeyPair keyPair;
    
    static {
        merkleTree = new MerkleTree(new File(SocketServer.dataDirPath));
        ACKChain = new LinkedList<>();
        ACKChain.add(Config.DEFAULT_CHAINHASH);
        
        LOCK = new ReentrantLock();
    }
    
    public WeiShianHandler(Socket socket, KeyPair keyPair) {
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
            
            File file = null;
            String result, digest;
            boolean addAckToChian = false;
            boolean sendFileAfterAck = false;
            
            switch (op.getType()) {
                case DOWNLOAD:
                    addAckToChian = true;
                    sendFileAfterAck = true;
                    
                    file = new File(SocketServer.dataDirPath + Utils.subPath(op.getPath()));
                    digest = Utils.digest(file);
                    
                    result = merkleTree.getRootHash();
                    
                    break;
                case UPLOAD:
                    addAckToChian = true;
                    
                    file = new File(Config.DOWNLOADS_DIR_PATH + Utils.subPath(op.getPath()));
                    
                    Utils.receive(in, file);

                    digest = Utils.digest(file);

                    if (op.getMessage().equals(digest)) {
                        // write file
                        merkleTree.update(op.getPath(), digest);
                        result = merkleTree.getRootHash();
                    } else {
                        result = Config.UPLOAD_FAIL;
                    }
                    
                    break;
                case AUDIT:
                    String clientLastAttHash = op.getMessage();
                    
                    ListIterator li = ACKChain.listIterator(ACKChain.size());
                    // find client's last attestation
                    while (li.hasPrevious()) {
                        String prevHash = Utils.digest((String) li.previous());
                        if (clientLastAttHash.equals(prevHash)) {
                            li.next();
                            break;
                        }
                    }
                    
                    LinkedList<String> clientWanted = new LinkedList<>();
                    while (li.hasNext()) {
                        clientWanted.add((String) li.next());
                    }
                    
                    sendFileAfterAck = true;
                    
                    file = new File(Config.ATTESTATION_DIR_PATH + "/service-provider/WeiShian");
                    Utils.Serialize(file, clientWanted);
                    digest = Utils.digest(file);
                    
                    result = merkleTree.getRootHash();
                    
                    break;
                default:
                    result = Config.OP_TYPE_MISMATCH;
                    digest = Config.OP_TYPE_MISMATCH;
            }
            
            Acknowledgement ack = new Acknowledgement(result, digest, req, Utils.digest(ACKChain.getLast()));
            ack.sign(keyPair);
            Utils.send(out, ack.toString());
            
            if (addAckToChian) {
                ACKChain.add(ack.toString());
            }
            
            if (sendFileAfterAck) {
                Utils.send(out, file);
            }
             
            socket.close();
        } catch (IOException | SignatureException ex) {
            Logger.getLogger(WeiShianHandler.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (LOCK != null) {
                LOCK.unlock();
            }
        }
    }
}
