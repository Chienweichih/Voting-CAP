package message.twostep.csn;

import message.Operation;
import message.SOAPMessage;

/**
 *
 * @author Scott
 */
public class Request extends SOAPMessage {
    private static final long serialVersionUID = 20141006L;
    private final Operation operation;
    private final Integer consecutiveSequenceNumber;
    
    public Request(Operation op, Integer csn) {
        super("request");
        
        this.operation = op;
        this.consecutiveSequenceNumber = csn;
        
        add2Body(operation.toMap());
        add2Body("CSN", consecutiveSequenceNumber.toString());
    }
    
    public Operation getOperation() {
        return operation;
    }
    
    public Integer getConsecutiveSequenceNumber() {
        return consecutiveSequenceNumber;
    }
}
