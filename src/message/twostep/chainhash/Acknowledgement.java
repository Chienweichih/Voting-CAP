package message.twostep.chainhash;

import message.SOAPMessage;

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
    
    public String getResult() {
        return result;
    }
    
    public Request getRequest() {
        return request;
    }
    
    public String getChainHash() {
        return lastChainHash;
    }
}
