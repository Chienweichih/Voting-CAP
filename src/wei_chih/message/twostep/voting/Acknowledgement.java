package wei_chih.message.twostep.voting;

import message.SOAPMessage;
import org.w3c.dom.NodeList;

/**
 *
 * @author Chienweichih
 */
public class Acknowledgement extends SOAPMessage {
    private static final long serialVersionUID = 20141006L;
    //private final String result;
    private final String roothash;
    private final String fileHash;
    private final Request request;
    
    public Acknowledgement(String roothash, String fileHash, Request req) {
        super("acknowledgement");
        
        this.roothash = roothash;
        this.fileHash = fileHash;
        this.request = req;
        
        add2Body("roothash", roothash);
        add2Body("fileHash", fileHash);
        add2Body("request", request.toString());
    }
    
    private Acknowledgement(javax.xml.soap.SOAPMessage message) {
        super(message);
        
        NodeList body = getBody();
        
        this.roothash = body.item(0).getTextContent();
        this.fileHash = body.item(1).getTextContent();
        this.request = Request.parse(body.item(2).getTextContent());
    }
    
    public String getRoothash() {
        return roothash;
    }
    
    public String getFileHash() {
        return fileHash;
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
        
        return this.roothash.equals(objAck.roothash) && this.fileHash.equals(objAck.fileHash) && this.request.equals(objAck.request);
    }
    
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 53 * hash + (this.roothash != null ? this.roothash.hashCode() : 0);
        hash = 53 * hash + (this.fileHash != null ? this.fileHash.hashCode() : 0);
        hash = 53 * hash + (this.request != null ? this.request.hashCode() : 0);
        return hash;
    }
}
