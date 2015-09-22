package wei_shian_pov.message.twostep.voting;

import message.SOAPMessage;
import org.w3c.dom.NodeList;

/**
 *
 * @author Chienweichih
 */
public class Acknowledgement extends SOAPMessage {
    private static final long serialVersionUID = 20141006L;
    private final String roothash;
    private final String fileHash;
    private final Request request;
    private final String lastChainHash;
    
    public Acknowledgement(String roothash, String fileHash, Request req, String hash) {
        super("acknowledgement");
        
        this.roothash = roothash;
        this.fileHash = fileHash;
        this.request = req;
        this.lastChainHash = hash;
        
        add2Body("roothash", roothash);
        add2Body("fileHash", fileHash);
        add2Body("request", request.toString());
        add2Body("chainhash", lastChainHash);
    }
    
    private Acknowledgement(javax.xml.soap.SOAPMessage message) {
        super(message);
        
        NodeList body = getBody();
        
        this.roothash = body.item(0).getTextContent();
        this.fileHash = body.item(1).getTextContent();
        this.request = Request.parse(body.item(2).getTextContent());
        this.lastChainHash = body.item(3).getTextContent();
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
    
    public String getChainHash() {
        return lastChainHash;
    }
    
    public static Acknowledgement parse(String receive) {
        return new Acknowledgement(SOAPMessage.parseSOAP(receive));
    }
}
