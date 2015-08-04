package voting_pov.message.twostep.voting;

import message.Operation;
import message.OperationType;
import message.SOAPMessage;
import org.w3c.dom.NodeList;

/**
 *
 * @author Chienweichih
 */
public class Request extends SOAPMessage {
    private static final long serialVersionUID = 20141006L;
    private final Operation operation;
    
    public Request(Operation op) {
        super("request");
        
        this.operation = op;
        
        add2Body(operation.toMap());
    }
    
    private Request(javax.xml.soap.SOAPMessage message) {
        super(message);
        
        NodeList body = getBody();
        NodeList operation = body.item(0).getChildNodes();
        
        OperationType opType = OperationType.valueOf(operation.item(0).getTextContent());
        String path = operation.item(1).getTextContent();
        String msg = operation.item(2).getTextContent();
        
        this.operation = new Operation(opType, path, msg);
    }
    
    public Operation getOperation() {
        return operation;
    }
    
    public static Request parse(String receive) {
        return new Request(SOAPMessage.parseSOAP(receive));
    }
}
