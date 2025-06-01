package net.lenni0451.aggregatingpublisher.utils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.function.BiConsumer;

public class XmlUtils {

    public static Document parse(final String xml) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringElementContentWhitespace(true);
        factory.setExpandEntityReferences(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xml)));
    }

    public static String toString(final Document document) throws TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        return writer.toString();
    }

    @Nullable
    public static Element getChildElement(final Element parent, final String... childPath) {
        Element current = parent;
        for (String path : childPath) {
            current = getChildElement(current, path);
            if (current == null) return null;
        }
        return current;
    }

    @Nullable
    public static Element getChildElement(final Element parent, final String childName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equals(childName)) {
                return (Element) child;
            }
        }
        return null;
    }

    public static void merge(final Element from, final Element to, final String childName, final BiConsumer<Element, Element> mergeAction) {
        Element fromChild = getChildElement(from, childName);
        if (fromChild == null) return;

        Element toChild = getChildElement(to, childName);
        if (toChild == null) return;

        mergeAction.accept(fromChild, toChild);
    }

    public static void copyOrMerge(final Element from, final Element to, final String childName, final BiConsumer<Element, Element> mergeAction) {
        Element fromChild = getChildElement(from, childName);
        if (fromChild == null) return;

        Element toChild = getChildElement(to, childName);
        if (toChild != null) {
            mergeAction.accept(fromChild, toChild);
        } else {
            Element newChild = (Element) to.getOwnerDocument().importNode(fromChild, true);
            to.appendChild(newChild);
        }
    }

    public static void removeWhitespaceNodes(final Node node) {
        NodeList children = node.getChildNodes();
        for (int i = children.getLength() - 1; i >= 0; i--) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.TEXT_NODE && child.getTextContent().trim().isEmpty()) {
                node.removeChild(child);
            } else if (child.hasChildNodes()) {
                removeWhitespaceNodes(child);
            }
        }
    }

}
