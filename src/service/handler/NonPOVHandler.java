package service.handler;

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

import service.Config;
import message.nonpov.*;
import utility.Utils;

/**
 *
 * @author Scott
 */
public class NonPOVHandler implements ConnectionHandler {
    private final Socket socket;
    private final KeyPair keyPair;
    
    public NonPOVHandler(Socket socket, KeyPair keyPair) {
        this.socket = socket;
        this.keyPair = keyPair;
    }
    
    @Override
    public void run() {
        try (DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {
            Request req = Request.parse(Utils.receive(in));
            
            PublicKey clientPubKey = Utils.readKeyPair("client.key").getPublic();
            
            if (!req.validate(clientPubKey)) {
                throw new SignatureException("REQ validation failure");
            }
            
            String result;
            
            File file = null;
            boolean sendFileAfterAck = false;
            
               
            Operation op = req.getOperation();

            file = new File(op.getPath());

            String fname = Config.DATA_DIR_PATH + "/" + file.getName() + ".digest";

            String digest;

            switch (op.getType()) {
                case UPLOAD:
                    Utils.receive(in, file);

                    digest = Utils.digest(file, Config.DIGEST_ALGORITHM);

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
            
            Acknowledgement ack = new Acknowledgement(result);
            
            ack.sign(keyPair);
            
            Utils.send(out, ack.toString());
            
            if (sendFileAfterAck) {
                Utils.send(out, file);
            }
            
            File ack_attestation = new File("attestation/service-provider/nonpov.ack");
            
            try (FileWriter fw = new FileWriter(ack_attestation, true)) {
                fw.append(ack.toString() + '\n');
            }
            
            File req_attestation = new File("attestation/service-provider/nonpov.req");
            
            try (FileWriter fw = new FileWriter(req_attestation, true)) {
                fw.append(req.toString() + '\n');
            }
            
            socket.close();
        } catch (IOException | SignatureException ex) {
            Logger.getLogger(NonPOVHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
