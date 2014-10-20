package message.fourstep.doublechainhash;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPException;
import message.SOAPMessage;
import org.w3c.dom.NodeList;

/**
 *
 * @author Scott
 */
public class Response extends SOAPMessage {
    private static final long serialVersionUID = 20141013L;
    private final Request request;
    private final String clientDeviceLastChainHash; // ACKj
    private final String userLastChainHash; // Ri-1
    
    public Response(Request req, String hash1, String hash2) {
        super("response");
        
        this.request = req;
        this.clientDeviceLastChainHash = hash1;
        this.userLastChainHash = hash2;
        
        add2Body("request", request.toString());
        add2Body("result", clientDeviceLastChainHash);
        add2Body("chainhash", userLastChainHash);
    }
    
    private Response(javax.xml.soap.SOAPMessage message) {
        super(message);
        
        NodeList body = getBody();
        
        this.request = Request.parse(body.item(0).getTextContent());
        this.clientDeviceLastChainHash = body.item(1).getTextContent();
        this.userLastChainHash = body.item(2).getTextContent();
    }
    
    public Request getRequest() {
        return request;
    }
    
    public String getClientDeviceLastChainHash() {
        return clientDeviceLastChainHash;
    }
    
    public String getUserLastChainHash() {
        return userLastChainHash;
    }
    
    public static Response parse(String receive) {
        InputStream stream;
        javax.xml.soap.SOAPMessage message = null;
        
        try {
            stream = new ByteArrayInputStream(receive.getBytes(StandardCharsets.UTF_8));
            message = MessageFactory.newInstance().createMessage(null, stream);
        } catch (SOAPException | IOException ex) {
            Logger.getLogger(SOAPMessage.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return new Response(message);
    }
}
