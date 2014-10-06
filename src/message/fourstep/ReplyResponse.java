package message.fourstep;

import message.SOAPMessage;

/**
 *
 * @author Scott
 */
public class ReplyResponse extends SOAPMessage {
    private static final long serialVersionUID = 20141006L;
    private final Response response;
    
    public ReplyResponse(Response res) {
        super("reply-response");
        
        this.response = res;
        
        add2Body("response", response.toString());
    }
    
    public Response getResponse() {
        return response;
    }
}