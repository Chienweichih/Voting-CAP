package wei_chih.message.wei_chih;

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
        String opClientID = operation.item(3).getTextContent();

        this.operation = new Operation(opType, path, msg, opClientID);
    }

    public Operation getOperation() {
        return operation;
    }

    public static Request parse(String receive) {
        return new Request(SOAPMessage.parseSOAP(receive));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }

        final Operation objOp = ((Request) obj).operation;

        return objOp.getType() == this.operation.getType()
                && 0 == objOp.getPath().compareTo(this.operation.getPath())
                && 0 == objOp.getMessage().compareTo(this.operation.getMessage());
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 53 * hash + (this.operation != null ? this.operation.hashCode() : 0);
        return hash;
    }
}
