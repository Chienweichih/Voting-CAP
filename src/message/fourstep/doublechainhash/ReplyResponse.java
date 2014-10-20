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
public class ReplyResponse extends SOAPMessage {
    private static final long serialVersionUID = 20141013L;
    private final Response response;
    
    public ReplyResponse(Response res) {
        super("reply-response");
        
        this.response = res;
        
        add2Body("response", response.toString());
    }
    
    private ReplyResponse(javax.xml.soap.SOAPMessage message) {
        super(message);
        
        NodeList body = getBody();
        
        this.response = Response.parse(body.item(0).getTextContent());
    }
    
    public Response getResponse() {
        return response;
    }
    
    public static ReplyResponse parse(String receive) {
        InputStream stream;
        javax.xml.soap.SOAPMessage message = null;
        
        try {
            stream = new ByteArrayInputStream(receive.getBytes(StandardCharsets.UTF_8));
            message = MessageFactory.newInstance().createMessage(null, stream);
        } catch (SOAPException | IOException ex) {
            Logger.getLogger(SOAPMessage.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return new ReplyResponse(message);
    }
}