package message.fourstep.chainhash_lsn;

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
        return new ReplyResponse(SOAPMessage.parseSOAP(receive));
    }
}