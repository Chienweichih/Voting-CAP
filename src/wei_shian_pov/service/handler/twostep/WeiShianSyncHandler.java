/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wei_shian_pov.service.handler.twostep;

import java.net.Socket;
import java.security.KeyPair;
import service.handler.ConnectionHandler;

/**
 *
 * @author chienweichih
 */
public class WeiShianSyncHandler implements ConnectionHandler {

    private final Socket socket;
    private final KeyPair keyPair;
    
    public WeiShianSyncHandler(Socket socket, KeyPair keyPair) {
        this.socket = socket;
        this.keyPair = keyPair;
    }
    
    @Override
    public void run() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
