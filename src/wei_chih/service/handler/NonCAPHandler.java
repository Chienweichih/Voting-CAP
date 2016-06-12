package wei_chih.service.handler;

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
import service.Key;
import service.KeyManager;
import service.handler.ConnectionHandler;
import wei_chih.message.noncap.Acknowledgement;
import wei_chih.message.noncap.Request;
import wei_chih.service.Config;
import wei_chih.service.SocketServer;
import wei_chih.utility.Utils;

/**
 *
 * @author chienweichih
 */
public class NonCAPHandler extends ConnectionHandler {

    private static final ReentrantLock LOCK;

    static {
        LOCK = new ReentrantLock();
    }

    public NonCAPHandler(Socket socket, KeyPair keyPair) {
        super(socket, keyPair);
    }

    @Override
    protected void handle(DataOutputStream out, DataInputStream in) {
        PublicKey clientPubKey = KeyManager.getInstance().getPublicKey(Key.CLIENT);

        try {
            Request req = Request.parse(Utils.receive(in));
            Operation op = req.getOperation();

            LOCK.lock();

            if (!req.validate(clientPubKey)) {
                throw new SignatureException("REQ validation failure");
            }

            String result = Utils.digest(new File(SocketServer.dataDirPath + op.getPath()), Config.DIGEST_ALGORITHM);

            Acknowledgement ack = new Acknowledgement(result);
            ack.sign(keyPair);
            Utils.send(out, ack.toString());

            File file;
            switch (op.getType()) {
                case UPLOAD:
                    file = new File(Config.DOWNLOADS_DIR_PATH + op.getPath());
                    Utils.receive(in, file);
                    String digest = Utils.digest(file, Config.DIGEST_ALGORITHM);
                    if (0 != op.getMessage().compareTo(digest)) {
                        throw new java.io.IOException();
                    }
                    break;
                case DOWNLOAD:
                    file = new File(SocketServer.dataDirPath + op.getPath());
                    Utils.send(out, file);
                    break;
                default:
            }

            socket.close();
        } catch (SignatureException | IOException ex) {
            Logger.getLogger(NonCAPHandler.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (LOCK != null) {
                LOCK.unlock();
            }
        }
    }
}
