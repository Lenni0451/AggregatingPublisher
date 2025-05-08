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
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Thank ChatGPT for this one.
 */
public class MavenMetadataMerger {

    public static String merge(@Nullable final String remoteMetadata, final String publicationMetadata) throws IOException {
        // If there's no existing metadata, return the publication metadata directly
        if (remoteMetadata == null) return publicationMetadata;

        try {
            // Set up XML parsers
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setIgnoringElementContentWhitespace(true);
            DocumentBuilder builder = factory.newDocumentBuilder();

            // Parse XML strings into DOM documents
            Document remoteDoc = builder.parse(new InputSource(new StringReader(remoteMetadata)));
            Document pubDoc = builder.parse(new InputSource(new StringReader(publicationMetadata)));

            // Locate versioning elements
            Element remoteVersioning = (Element) remoteDoc.getElementsByTagName("versioning").item(0);
            Element pubVersioning = (Element) pubDoc.getElementsByTagName("versioning").item(0);

            // 1. Merge <release> element: preserve remote release if present
            NodeList remoteReleaseList = remoteVersioning.getElementsByTagName("release");
            if (remoteReleaseList.getLength() > 0) {
                String releaseText = remoteReleaseList.item(0).getTextContent();
                // Remove any existing <release> in publication
                NodeList pubReleaseList = pubVersioning.getElementsByTagName("release");
                if (pubReleaseList.getLength() > 0) {
                    pubVersioning.removeChild(pubReleaseList.item(0));
                }
                // Create and insert <release> after <latest>
                Element releaseElem = pubDoc.createElement("release");
                releaseElem.setTextContent(releaseText);
                NodeList latestList = pubVersioning.getElementsByTagName("latest");
                if (latestList.getLength() > 0) {
                    Node latestNode = latestList.item(0);
                    Node next = latestNode.getNextSibling();
                    pubVersioning.insertBefore(releaseElem, next);
                } else {
                    pubVersioning.insertBefore(releaseElem, pubVersioning.getFirstChild());
                }
            }

            // 2. Merge <versions> list: union remote and publication versions
            Set<String> versions = new LinkedHashSet<>();
            Element remoteVersionsElem = (Element) remoteVersioning.getElementsByTagName("versions").item(0);
            NodeList remoteVersions = remoteVersionsElem.getElementsByTagName("version");
            for (int i = 0; i < remoteVersions.getLength(); i++) {
                versions.add(remoteVersions.item(i).getTextContent());
            }
            Element pubVersionsElem = (Element) pubVersioning.getElementsByTagName("versions").item(0);
            NodeList pubVersions = pubVersionsElem.getElementsByTagName("version");
            for (int i = 0; i < pubVersions.getLength(); i++) {
                versions.add(pubVersions.item(i).getTextContent());
            }
            // Clear existing and append merged versions
            while (pubVersionsElem.hasChildNodes()) {
                pubVersionsElem.removeChild(pubVersionsElem.getFirstChild());
            }
            for (String v : versions) {
                Element versionElem = pubDoc.createElement("version");
                versionElem.setTextContent(v);
                pubVersionsElem.appendChild(versionElem);
            }

            // 3. Update <lastUpdated> to publication's timestamp (already present in publication)
            // No action needed since publicationMetadata includes its own <lastUpdated>.

            // Remove any whitespace-only text nodes to avoid blank lines
            removeWhitespaceNodes(pubDoc.getDocumentElement());

            // 4. Serialize DOM back to XML string
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
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
