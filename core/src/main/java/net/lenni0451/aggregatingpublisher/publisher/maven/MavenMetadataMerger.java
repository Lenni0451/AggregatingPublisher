package net.lenni0451.aggregatingpublisher.publisher.maven;

import net.lenni0451.aggregatingpublisher.utils.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.annotation.Nullable;
import java.io.IOException;

public class MavenMetadataMerger {

    public static String merge(@Nullable final String remoteMetadata, final String publicationMetadata) throws IOException {
        if (remoteMetadata == null) return publicationMetadata;

        try {
            Document remoteDoc = XmlUtils.parse(remoteMetadata);
            Document pubDoc = XmlUtils.parse(publicationMetadata);
            XmlUtils.merge(remoteDoc.getDocumentElement(), pubDoc.getDocumentElement(), "versioning", (fromVersioning, toVersioning) -> {
                XmlUtils.copyOrMerge(fromVersioning, toVersioning, "latest", (from, to) -> {});
                XmlUtils.copyOrMerge(fromVersioning, toVersioning, "release", (from, to) -> {});
                XmlUtils.copyOrMerge(fromVersioning, toVersioning, "snapshot", (fromSnapshot, toSnapshot) -> {
                    Element remoteBuildNumber = XmlUtils.getChildElement(fromSnapshot, "buildNumber");
                    if (remoteBuildNumber != null) {
                        Element newBuildNumber = pubDoc.createElement("remoteBuildNumber");
                        newBuildNumber.setTextContent(remoteBuildNumber.getTextContent());
                        toSnapshot.appendChild(newBuildNumber);
                    }
                });
                XmlUtils.copyOrMerge(fromVersioning, toVersioning, "lastUpdated", (from, to) -> {});
                XmlUtils.copyOrMerge(fromVersioning, toVersioning, "versions", (fromVersions, toVersions) -> {
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
                XmlUtils.copyOrMerge(fromVersioning, toVersioning, "snapshotVersions", (fromSnapshotVersions, toSnapshotVersions) -> {});
            });

            XmlUtils.removeWhitespaceNodes(pubDoc.getDocumentElement());
            return XmlUtils.toString(pubDoc);
        } catch (Throwable t) {
            throw new IOException("Failed to merge Maven metadata", t);
        }
    }

}
