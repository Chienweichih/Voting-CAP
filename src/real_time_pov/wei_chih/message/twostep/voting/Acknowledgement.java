package real_time_pov.wei_chih.message.twostep.voting;

import message.SOAPMessage;
import org.w3c.dom.NodeList;

/**
 *
 * @author Chienweichih
 */
public class Acknowledgement extends SOAPMessage {
    private static final long serialVersionUID = 20141006L;
    private final String result;
    private final Request request;
    
    public Acknowledgement(String result, Request req) {
        super("acknowledgement");
        
        this.result = result;
        this.request = req;
        
        add2Body("result", result);
        add2Body("request", request.toString());
    }
    
    private Acknowledgement(javax.xml.soap.SOAPMessage message) {
        super(message);
        
        NodeList body = getBody();
        
        this.result = body.item(0).getTextContent();
        this.request = Request.parse(body.item(1).getTextContent());
    }
    
    public String getResult() {
        return result;
    }
    
    public Request getRequest() {
        return request;
    }
    
    public static Acknowledgement parse(String receive) {
        return new Acknowledgement(SOAPMessage.parseSOAP(receive));
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        
        final Acknowledgement objAck = (Acknowledgement) obj;
        
        return this.result.equals(objAck.result) && this.request.equals(objAck.request);
    }
    
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 53 * hash + (this.result != null ? this.result.hashCode() : 0);
        hash = 53 * hash + (this.request != null ? this.request.hashCode() : 0);
        return hash;
    }
}
