package service.handler.fourstep;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.logging.Level;
import java.util.logging.Logger;
import message.Operation;

import message.fourstep.*;
import service.Config;
import service.handler.ConnectionHandler;
import utility.Utils;

/**
 *
 * @author Scott
 */
public class ChainHashAndLSNHandler implements ConnectionHandler {
    private static final HashingChainTable HASHING_CHAIN_TABLE;
    private static final LSNTable LSN_TABLE;
    private final Socket socket;
    private final KeyPair keyPair;
    
    static {
        HASHING_CHAIN_TABLE = new HashingChainTable();
        LSN_TABLE = new LSNTable();
    }
    
    public ChainHashAndLSNHandler(Socket socket, KeyPair keyPair) {
        this.socket = socket;
        this.keyPair = keyPair;
    }
    
    @Override
    public void run() {
        PublicKey clientPubKey = Utils.readKeyPair("client.key").getPublic();
        
        try (DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {
            Request req = Request.parse(Utils.receive(in));
            
            if (!req.validate(clientPubKey)) {
                throw new SignatureException("REQ validation failure");
            }
            
            String clientID = req.getClientID();
            Integer lsn = req.getLocalSequenceNumber();
            
            String result;
            
            if (!LSN_TABLE.isMatched(clientID, lsn)) {
                result = "LSN mismatch";
            } else {
                result = Utils.digest("result");
            }
            
            String lastChainHash = HASHING_CHAIN_TABLE.getLastChainHash(result);
            
            Response res = new Response(req, result, lastChainHash);
            
            res.sign(keyPair);
            
            Utils.send(out, res.toString());
            
            ReplyResponse rr = ReplyResponse.parse(Utils.receive(in));
            
            if (!rr.validate(clientPubKey)) {
                throw new SignatureException("RR validation failure");
            }
            
            LSN_TABLE.increment(req.getClientID());
            
            File file;
            boolean sendFileAfterAck = false;
            
            Operation op = req.getOperation();

            file = new File(op.getPath());

            String fname = Config.DATA_DIR_PATH + "/" + file.getName() + ".digest";

            String digest;

            switch (op.getType()) {
                case UPLOAD:
                    Utils.receive(in, file);

                    digest = Utils.digest(file);

                    if (op.getMessage().compareTo(digest) == 0) {
                        result = "ok";
                    } else {
                        result = "upload fail";
                    }

                    try (FileWriter fw = new FileWriter(fname)) {
                        fw.write(digest);
                    }

                    break;
                case DOWNLOAD:
                    try (FileReader fr = new FileReader(fname);
                         BufferedReader br = new BufferedReader(fr)) {
                        digest = br.readLine();
                    }

                    result = digest;
                    sendFileAfterAck = true;

                    break;
                default:
                    result = "operation type mismatch";
            }
            
            Acknowledgement ack = new Acknowledgement(result, rr);
            
            ack.sign(keyPair);
            
            Utils.send(out, ack.toString());
            
            if (sendFileAfterAck) {
                Utils.send(out, file);
            }
            
            HASHING_CHAIN_TABLE.chain(req.getClientID(), Utils.digest(ack.toString()));
            
            File attestation = new File("attestation/service-provider/chainhash-lsn");
            
            try (FileWriter fw = new FileWriter(attestation, true)) {
                fw.append(ack.toString() + '\n');
            }
            
            socket.close();
        } catch (IOException | SignatureException ex) {
            Logger.getLogger(ChainHashAndLSNHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}