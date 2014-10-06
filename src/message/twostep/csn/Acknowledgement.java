package message.twostep.csn;

import message.SOAPMessage;

/**
 *
 * @author Scott
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
    
    public String getResult() {
        return result;
    }
    
    public Request getRequest() {
        return request;
    }
}
