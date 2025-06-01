package net.lenni0451.aggregatingpublisher.publisher.maven;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.function.BiConsumer;

public class MavenMetadataMerger {

    public static String merge(@Nullable final String remoteMetadata, final String publicationMetadata) throws IOException {
        if (remoteMetadata == null) return publicationMetadata;

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setIgnoringElementContentWhitespace(true);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();

            Document remoteDoc = builder.parse(new InputSource(new StringReader(remoteMetadata)));
            Document pubDoc = builder.parse(new InputSource(new StringReader(publicationMetadata)));
            merge(remoteDoc.getDocumentElement(), pubDoc.getDocumentElement(), "metadata", (remoteMeta, pubMeta) -> {
                merge(remoteMeta, pubMeta, "versioning", (fromVersioning, toVersioning) -> {
                    copyOrMerge(fromVersioning, toVersioning, "latest", (from, to) -> {});
                    copyOrMerge(fromVersioning, toVersioning, "release", (from, to) -> {});
                    copyOrMerge(fromVersioning, toVersioning, "snapshot", (from, to) -> {});
                    copyOrMerge(fromVersioning, toVersioning, "lastUpdated", (from, to) -> {});
                    copyOrMerge(fromVersioning, toVersioning, "versions", (fromVersions, toVersions) -> {
                        NodeList fromVersionsList = fromVersions.getChildNodes();
                        for (int i = 0; i < fromVersionsList.getLength(); i++) {
                            Node fromVersionNode = fromVersionsList.item(i);
                            if (fromVersionNode.getNodeType() == Node.ELEMENT_NODE && "version".equals(fromVersionNode.getNodeName())) {
                                String versionText = fromVersionNode.getTextContent();
                                boolean exists = false;
                                NodeList toVersionsList = toVersions.getChildNodes();
                                for (int j = 0; j < toVersionsList.getLength(); j++) {
                                    Node toVersionNode = toVersionsList.item(j);
                                    if (toVersionNode.getNodeType() == Node.ELEMENT_NODE && "version".equals(toVersionNode.getNodeName())) {
                                        if (toVersionNode.getTextContent().equals(versionText)) {
                                            exists = true;
                                            break;
                                        }
                                    }
                                }
                                if (!exists) {
                                    Element newVersionElem = pubDoc.createElement("version");
                                    newVersionElem.setTextContent(versionText);
                                    toVersions.appendChild(newVersionElem);
                                }
                            }
                        }
                    });
                    copyOrMerge(fromVersioning, toVersioning, "snapshotVersions", (fromSnapshotVersions, toSnapshotVersions) -> {});
                });
            });

            removeWhitespaceNodes(pubDoc.getDocumentElement());
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(pubDoc), new StreamResult(writer));

            return writer.toString();
        } catch (Exception e) {
            throw new IOException("Failed to merge Maven metadata", e);
        }
    }

    @Nullable
    private static Element getChildElement(final Element parent, final String childName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equals(childName)) {
                return (Element) child;
            }
        }
        return null;
    }

    private static void merge(final Element from, final Element to, final String childName, final BiConsumer<Element, Element> mergeAction) {
        Element fromChild = getChildElement(from, childName);
        if (fromChild != null) {
            Element toChild = getChildElement(to, childName);
            if (toChild != null) {
                mergeAction.accept(fromChild, toChild);
            }
        }
    }

    private static void copyOrMerge(final Element from, final Element to, final String childName, final BiConsumer<Element, Element> mergeAction) {
        Element fromChild = getChildElement(from, childName);
        if (fromChild != null) {
            Element toChild = getChildElement(to, childName);
            if (toChild != null) {
                mergeAction.accept(fromChild, toChild);
            } else {
                Element newChild = (Element) to.getOwnerDocument().importNode(fromChild, true);
                to.appendChild(newChild);
            }
        }
    }

    private static void removeWhitespaceNodes(Node node) {
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
