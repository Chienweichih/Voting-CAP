package wei_chih.service.handler.wei_chih;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import message.OperationType;
import service.Key;
import service.KeyManager;
import service.handler.ConnectionHandler;
import wei_chih.service.Config;
import wei_chih.utility.Utils;
import wei_chih.message.wei_chih.Request;
import wei_chih.message.wei_chih.Acknowledgement;

/**
 *
 * @author chienweichih
 */
public class WeiChihSyncHandler extends ConnectionHandler {

    private static final ReentrantLock LOCK;

    public static final int[] SERVER_PORTS;
    public static final int SYNC_PORT;

    private static final Map<Integer, Integer> sequenceNumbers;
    private static final Map<Integer, Acknowledgement> lastAcks;

    static {
        LOCK = new ReentrantLock();

        SERVER_PORTS = new int[Config.SERVICE_NUM];
        System.arraycopy(Config.SERVICE_PORT, 0, SERVER_PORTS, 0, Config.SERVICE_NUM);

        SYNC_PORT = Config.SERVICE_PORT[Config.SERVICE_NUM];

        sequenceNumbers = new HashMap<>();
        lastAcks = new HashMap<>();

        for (int port : SERVER_PORTS) {
            sequenceNumbers.put(port, 0);
            lastAcks.put(port, null);
        }
    }

    public WeiChihSyncHandler(Socket socket, KeyPair keyPair) {
        super(socket, keyPair);
    }

    @Override
    protected void handle(DataOutputStream out, DataInputStream in) {
        PublicKey clientPubKey = KeyManager.getInstance().getPublicKey(Key.CLIENT);

        try {
            Request req = Request.parse(Utils.receive(in));

            LOCK.lock();

            File syncSN = new File(Config.DOWNLOADS_DIR_PATH + "/syncSNs");
            Map<Integer, Integer> syncSNReturn;

            File syncAck = new File(Config.DOWNLOADS_DIR_PATH + "/syncLast");
            Map<Integer, String> syncAckStrs = new HashMap<>();

            if (!req.validate(clientPubKey)) {
                throw new SignatureException("REQ validation failure");
            }

            if (req.getOperation().getType() != OperationType.DOWNLOAD) {
                return;
            }

            Utils.Serialize(syncSN, sequenceNumbers);
            Utils.send(out, syncSN);

            if (0 != req.getOperation().getMessage().compareTo(Config.EMPTY_STRING)) {
                for (int port : SERVER_PORTS) {
                    String lastStr = (lastAcks.get(port) == null) ? null : lastAcks.get(port).toString();
                    syncAckStrs.put(port, lastStr);
                }
                Utils.Serialize(syncAck, syncAckStrs);
                Utils.send(out, syncAck);
            }

            // wait until client finish
            req = Request.parse(Utils.receive(in));

            if (!req.validate(clientPubKey)) {
                throw new SignatureException("REQ validation failure");
            }

            if (req.getOperation().getType() != OperationType.UPLOAD) {
                return;
            }

            Utils.receive(in, syncSN);
            syncSNReturn = Utils.Deserialize(syncSN.getAbsolutePath());

            for (int port : SERVER_PORTS) {
                if (syncSNReturn.get(port) != null) {
                    sequenceNumbers.replace(port, syncSNReturn.get(port));
                }
            }

            if (0 != req.getOperation().getMessage().compareTo(Config.EMPTY_STRING)) {
                Utils.receive(in, syncAck);
                syncAckStrs = Utils.Deserialize(syncAck.getAbsolutePath());

                for (int port : SERVER_PORTS) {
                    if (syncAckStrs.get(port) != null) {
                        lastAcks.replace(port, Acknowledgement.parse(syncAckStrs.get(port)));
                    }
                }
            }

            socket.close();
        } catch (IOException | SignatureException ex) {
            Logger.getLogger(WeiChihSyncHandler.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (LOCK != null) {
                LOCK.unlock();
            }
        }
    }
}
