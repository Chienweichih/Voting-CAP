package message.nonpov;

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
    
    public Acknowledgement(String result) {
        super("acknowledgement");
        
        this.result = result;
        
        add2Body("result", result);
    }
    
    private Acknowledgement(javax.xml.soap.SOAPMessage message) {
        super(message);
        
        NodeList body = getBody();
        
        this.result = body.item(0).getTextContent();
    }
    
    public String getResult() {
        return result;
    }
    
    public static Acknowledgement parse(String receive) {
        return new Acknowledgement(SOAPMessage.parseSOAP(receive));
    }
}
