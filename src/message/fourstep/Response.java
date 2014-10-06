package message.fourstep;

import message.SOAPMessage;

/**
 *
 * @author Scott
 */
public class Response extends SOAPMessage {
    private static final long serialVersionUID = 20141006L;
    private final Request request;
    private final String result;
    private final String lastChainHash;
    
    public Response(Request req, String result, String hash) {
        super("response");
        
        this.request = req;
        this.result = result;
        this.lastChainHash = hash;
        
        add2Body("request", request.toString());
        add2Body("result", result);
        add2Body("chainhash", lastChainHash);
    }
    
    public Request getRequest() {
        return request;
    }
    
    // for C&L
    public String getResult() {
        return result;
    }
    
    // for DC&L
    public String getClientLastChainHash() {
        return result;
    }
    
    public String getChainHash() {
        return lastChainHash;
    }
}
