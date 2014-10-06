package message.fourstep;

import message.SOAPMessage;

/**
 *
 * @author Scott
 */
public class Acknowledgement extends SOAPMessage {
    private static final long serialVersionUID = 20141006L;
    private final String result;
    private final ReplyResponse replyResponse;
    
    public Acknowledgement(String result, ReplyResponse rr) {
        super("acknowledgement");
        
        this.result = result;
        this.replyResponse = rr;
        
        add2Body("result", result);
        add2Body("reply-response", replyResponse.toString());
    }
    
    public String getResult() {
        return result;
    }
    
    public ReplyResponse getReplyResponse() {
        return replyResponse;
    }
}
