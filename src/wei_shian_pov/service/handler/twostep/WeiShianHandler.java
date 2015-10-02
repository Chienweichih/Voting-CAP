package wei_shian_pov.service.handler.twostep;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
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
import voting_pov.utility.MerkleTree;
import voting_pov.utility.Utils;
import wei_shian_pov.message.twostep.voting.*;
import wei_shian_pov.service.Config;

/**
 *
 * @author Chienweichih
 */
public class WeiShianHandler implements ConnectionHandler {
    
    private static final MerkleTree merkleTree;
    private static final LinkedList<String> HashingChain;
    private static final ReentrantLock LOCK;
    
    private final Socket socket;
    private final KeyPair keyPair;
    
    static {
        merkleTree = new MerkleTree(new File(Config.DATA_DIR_PATH));
        HashingChain = new LinkedList<>();
        HashingChain.add(Config.DEFAULT_CHAINHASH);
        
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
            
            String result, digest;
            
            Operation op = req.getOperation();
            
            File file = null;
            boolean sendFileAfterAck = false;
            
            switch (op.getType()) {
                case DOWNLOAD:
                    file = new File(Config.DATA_DIR_PATH + op.getPath());
                    
                    result = merkleTree.getRootHash();
                    digest = Utils.digest(file);
                    
                    sendFileAfterAck = true;

                    break;
                case UPLOAD:
                    file = new File(Config.DOWNLOADS_DIR_PATH + op.getPath());
                    
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
                    file = new File(Config.ATTESTATION_DIR_PATH + File.separator + "service-provider" + File.separator + "WeiShian");
                    
                    String attP = op.getMessage();
                    
                    ListIterator li = HashingChain.listIterator(HashingChain.size());
                    while (li.hasPrevious()) {
                        if (attP.equals(li.previous())) {
                            break;
                        }
                    }
                    
                    LinkedList<String> tempCH = new LinkedList<>();
                    while (li.hasNext()) {
                        tempCH.add((String) li.next());
                    }
                    
                    try (FileOutputStream fout = new FileOutputStream(file);
                         ObjectOutputStream oos = new ObjectOutputStream(fout)) {   
                        oos.writeObject(tempCH);
                        oos.close();
                    } catch (IOException ex) {
                        Logger.getLogger(MerkleTree.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    
                    result = merkleTree.getRootHash();
                    digest = Utils.digest(file);
                    
                    sendFileAfterAck = true;
                    
                    break;
                default:
                    result = Config.OP_TYPE_MISMATCH;
                    digest = Config.OP_TYPE_MISMATCH;
            }
            
            Acknowledgement ack = new Acknowledgement(result, digest, req, Utils.digest(HashingChain.getLast()));
            
            ack.sign(keyPair);
            
            Utils.send(out, ack.toString());
            
            HashingChain.add(ack.toString());
            
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
