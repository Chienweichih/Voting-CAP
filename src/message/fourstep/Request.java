package message.fourstep;

import message.Operation;
import message.SOAPMessage;

/**
 *
 * @author Scott
 */
public class Request extends SOAPMessage {
    private static final long serialVersionUID = 20141006L;
    private final Operation operation;
    private final String clientID;
    private final Integer localSequenceNumber;
    
    public Request(Operation op, String id, Integer lsn) {
        super("request");
        
        this.operation = op;
        this.clientID = id;
        this.localSequenceNumber = lsn;
        
        add2Body(operation.toMap());
        add2Body("client id", clientID);
        add2Body("lsn", localSequenceNumber.toString());
    }
    
    public Operation getOperation() {
        return operation;
    }
    
    public String getClientID() {
        return clientID;
    }
    
    public Integer getLocalSequenceNumber() {
        return localSequenceNumber;
    }
}
