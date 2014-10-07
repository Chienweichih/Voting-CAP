package message;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.KeyValue;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author Scott
 */
public abstract class XMLDocument {
    protected Document document;
    private boolean signed;
    
    public XMLDocument(String docName) {
        try {
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            builderFactory.setNamespaceAware(true);
            
            DocumentBuilder documentBuilder = builderFactory.newDocumentBuilder();
            
            document = documentBuilder.newDocument();
            
            document.appendChild(document.createElement(docName));
            
            signed = false;
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(XMLDocument.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public XMLDocument(Document doc) {
        document = doc;
        signed = doc.getElementsByTagName("Signature").getLength() == 1;
    }
    
    public void sign(KeyPair keyPair) {
        if (signed) {
            return ;
        }
        
        XMLSignatureFactory signFactory = XMLSignatureFactory.getInstance("DOM");
        KeyInfoFactory keyInfoFactory = signFactory.getKeyInfoFactory();
        Reference ref;
        SignedInfo signedInfo;
        KeyInfo keyInfo;
        
        try {
            ref = signFactory.newReference("",
                                           signFactory.newDigestMethod(DigestMethod.SHA1, null),
                                           Collections.singletonList(signFactory.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null)),
                                           null,
                                           null);
            signedInfo = signFactory.newSignedInfo(signFactory.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null),
                                                   signFactory.newSignatureMethod(SignatureMethod.RSA_SHA1, null),
                                                   Collections.singletonList(ref));
            
            KeyValue keyValue = keyInfoFactory.newKeyValue(keyPair.getPublic());
            keyInfo = keyInfoFactory.newKeyInfo(Collections.singletonList(keyValue));
            
            DOMSignContext dsc = new DOMSignContext(keyPair.getPrivate(), document.getDocumentElement());
            
            XMLSignature signature = signFactory.newXMLSignature(signedInfo, keyInfo);
            
            signature.sign(dsc);
            
            signed = true;
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException ex) {
            Logger.getLogger(XMLDocument.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MarshalException | XMLSignatureException | KeyException ex) {
            Logger.getLogger(XMLDocument.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public boolean verifyDigitalSignature(PublicKey pubKey) {
        try {
            NodeList nl = document.getElementsByTagName("Signature");
            
            // Create a DOM XMLSignatureFactory that will be used to unmarshal the
            // document containing the XMLSignature
            XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");
            
            // Create a DOMValidateContext and specify a KeyValue KeySelector
            // and document context
            DOMValidateContext valContext = new DOMValidateContext(pubKey, nl.item(0));
            
            // unmarshal the XMLSignature
            XMLSignature signature = fac.unmarshalXMLSignature(valContext);
            
            // Validate the XMLSignature (generated above)
            return signature.validate(valContext);
        } catch (MarshalException | XMLSignatureException ex) {
            Logger.getLogger(XMLDocument.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return false;
    }
    
    public String getSignatureValue() throws NoSuchFieldException {
        if (signed) {
            Node root = document.getDocumentElement();
            Node signNode = root.getLastChild().getChildNodes().item(1);
            String signVal = signNode.getTextContent();
            
            return signVal.replaceAll("\n", "");
        } else {
            throw new NoSuchFieldException("Not signed yet.");
        }
    }
    
    protected Element createElement(String name, String value) {
        Element t = document.createElement(name);
        
        t.setTextContent(value);
        
        return t;
    }
    
    public static Document parse2Document(String str) {
        try {
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            builderFactory.setNamespaceAware(true);
            
            DocumentBuilder documentBuilder = builderFactory.newDocumentBuilder();
            
            InputSource inputSource = new InputSource(new StringReader(str));
            
            return documentBuilder.parse(inputSource);
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            Logger.getLogger(XMLDocument.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return null;
    }
    
    @Override
    public String toString() {
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
//            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
//            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            
            StreamResult result = new StreamResult(new StringWriter());
            DOMSource source = new DOMSource(document);
            transformer.transform(source, result);
            
            return result.getWriter().toString().replaceAll("\n", "");
        } catch (TransformerConfigurationException ex) {
            Logger.getLogger(XMLDocument.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TransformerException ex) {
            Logger.getLogger(XMLDocument.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return "[toString failed]";
    }
}
