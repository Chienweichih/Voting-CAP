package message.twostep.chainhash;

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
public class Acknowledgement extends SOAPMessage {
    private static final long serialVersionUID = 20141006L;
    private final String result;
    private final Request request;
    private final String lastChainHash;
    
    public Acknowledgement(String result, Request req, String hash) {
        super("acknowledgement");
        
        this.result = result;
        this.request = req;
        this.lastChainHash = hash;
        
        add2Body("result", result);
        add2Body("request", request.toString());
        add2Body("chainhash", lastChainHash);
    }
    
    private Acknowledgement(javax.xml.soap.SOAPMessage message) {
        super(message);
        
        NodeList body = getBody();
        
        this.result = body.item(0).getTextContent();
        this.request = Request.parse(body.item(1).getTextContent());
        this.lastChainHash = body.item(2).getTextContent();
    }
    
    public String getResult() {
        return result;
    }
    
    public Request getRequest() {
        return request;
    }
    
    public String getChainHash() {
        return lastChainHash;
    }
    
    public static Acknowledgement parse(String receive) {
        InputStream stream;
        javax.xml.soap.SOAPMessage message = null;
        
        try {
            stream = new ByteArrayInputStream(receive.getBytes(StandardCharsets.UTF_8));
            message = MessageFactory.newInstance().createMessage(null, stream);
        } catch (SOAPException | IOException ex) {
            Logger.getLogger(SOAPMessage.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return new Acknowledgement(message);
    }
}
