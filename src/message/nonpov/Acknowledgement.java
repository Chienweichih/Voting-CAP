package message.nonpov;

import message.twostep.csn.*;
import message.SOAPMessage;

/**
 *
 * @author Scott
 */
public class Acknowledgement extends SOAPMessage {
    private static final long serialVersionUID = 20141006L;
    private final String result;
    
    public Acknowledgement(String result) {
        super("acknowledgement");
        
        this.result = result;
        
        add2Body("result", result);
    }
    
    public String getResult() {
        return result;
    }
}
