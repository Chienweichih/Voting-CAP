package message.nonpov;

import message.Operation;
import message.SOAPMessage;

/**
 *
 * @author Scott
 */
public class Request extends SOAPMessage {
    private static final long serialVersionUID = 20141006L;
    private final Operation operation;
    
    public Request(Operation op) {
        super("request");
        
        this.operation = op;
        
        add2Body(operation.toMap());
    }
    
    public Operation getOperation() {
        return operation;
    }
}
