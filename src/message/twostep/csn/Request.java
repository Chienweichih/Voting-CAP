package message.twostep.csn;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPException;
import message.Operation;
import message.OperationType;
import message.SOAPMessage;
import org.w3c.dom.NodeList;

/**
 *
 * @author Scott
 */
public class Request extends SOAPMessage {
    private static final long serialVersionUID = 20141021L;
    private final Operation operation;
    private final Integer consecutiveSequenceNumber;
    
    public Request(Operation op, Integer csn) {
        super("request");
        
        this.operation = op;
        this.consecutiveSequenceNumber = csn;
        
        add2Body(operation.toMap());
        add2Body("CSN", consecutiveSequenceNumber.toString());
    }
    
    private Request(javax.xml.soap.SOAPMessage message) {
        super(message);
        
        NodeList body = getBody();
        NodeList operation = body.item(0).getChildNodes();
        
        OperationType opType = OperationType.valueOf(operation.item(0).getTextContent());
        String path = operation.item(1).getTextContent();
        String msg = operation.item(2).getTextContent();
        
        String csn = body.item(1).getTextContent();
        
        this.operation = new Operation(opType, path, msg);
        this.consecutiveSequenceNumber = Integer.parseInt(csn);
    }
    
    public Operation getOperation() {
        return operation;
    }
    
    public Integer getConsecutiveSequenceNumber() {
        return consecutiveSequenceNumber;
    }
    
    public static Request parse(String receive) {
        InputStream stream;
        javax.xml.soap.SOAPMessage message = null;
        
        try {
            stream = new ByteArrayInputStream(receive.getBytes(StandardCharsets.UTF_8));
            message = MessageFactory.newInstance().createMessage(null, stream);
        } catch (SOAPException | IOException ex) {
            Logger.getLogger(SOAPMessage.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return new Request(message);
    }
}
