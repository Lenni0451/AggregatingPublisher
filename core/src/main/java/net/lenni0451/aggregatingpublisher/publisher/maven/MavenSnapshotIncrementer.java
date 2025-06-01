package net.lenni0451.aggregatingpublisher.publisher.maven;

import lombok.extern.slf4j.Slf4j;
import net.lenni0451.aggregatingpublisher.utils.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
public class MavenSnapshotIncrementer {

    @Nullable
    public static String incrementSnapshotVersion(final String metadataPath, final String metadata, final Map<String, byte[]> files) throws IOException {
        try {
            Document document = XmlUtils.parse(metadata);
            Element snapshot = XmlUtils.getChildElement(document.getDocumentElement(), "versioning", "snapshot");
            if (snapshot == null) return null; //Not a snapshot version

            Element timestamp = XmlUtils.getChildElement(snapshot, "timestamp");
            Element buildNumber = XmlUtils.getChildElement(snapshot, "buildNumber");
            Element remoteBuildNumber = XmlUtils.getChildElement(snapshot, "remoteBuildNumber");
            if (buildNumber == null) return null; //No build number to increment

            //Increment the build number
            String timestampValue = timestamp == null ? null : timestamp.getTextContent();
            int buildNumberValue = Integer.parseInt(buildNumber.getTextContent());
            int newBuildNumberValue;
            if (remoteBuildNumber == null) {
                //No remote build number, just begin with the current build number
                newBuildNumberValue = buildNumberValue;
            } else {
                //Remote build number is present, increment it and remove the remote build number
                newBuildNumberValue = Integer.parseInt(remoteBuildNumber.getTextContent()) + 1;
                remoteBuildNumber.getParentNode().removeChild(remoteBuildNumber);
            }
            buildNumber.setTextContent(String.valueOf(newBuildNumberValue));

            //Update the id in the snapshot versions
            Map<String, String> versionToUpdate = new HashMap<>();
            Element snapshotVersions = XmlUtils.getChildElement(document.getDocumentElement(), "versioning", "snapshotVersions");
            if (snapshotVersions != null) {
                NodeList versionsList = snapshotVersions.getChildNodes();
                for (int i = 0; i < versionsList.getLength(); i++) {
                    Node versionNode = versionsList.item(i);
                    if (versionNode.getNodeType() == Node.ELEMENT_NODE && "snapshotVersion".equals(versionNode.getNodeName())) {
                        Element versionElement = (Element) versionNode;
                        Element value = XmlUtils.getChildElement(versionElement, "value");
                        if (value != null) {
                            String valueValue = value.getTextContent();
                            if (timestampValue == null) {
                                //No timestamp is found, just increment all matching build numbers
                                String suffix = "-" + buildNumberValue;
                                if (valueValue.endsWith(suffix)) {
                                    String newValue = valueValue.substring(0, valueValue.length() - suffix.length()) + "-" + newBuildNumberValue;
                                    value.setTextContent(newValue);
                                    versionToUpdate.put(valueValue, newValue);
                                }
                            } else {
                                //Timestamp is found, also try matching the timestamp
                                String suffix = "-" + timestampValue + "-" + buildNumberValue;
                                if (valueValue.endsWith(suffix)) {
                                    String newValue = valueValue.substring(0, valueValue.length() - suffix.length()) + "-" + timestampValue + "-" + newBuildNumberValue;
                                    value.setTextContent(newValue);
                                    versionToUpdate.put(valueValue, newValue);
                                }
                            }
                        }
                    }
                }
            }

            //Rename artifact files
            String path = metadataPath.substring(0, metadataPath.lastIndexOf('/') + 1);
            for (Map.Entry<String, byte[]> entry : Set.copyOf(files.entrySet())) {
                if (!entry.getKey().startsWith(path)) continue; //Skip files not in the same directory as the metadata
                String fileName = entry.getKey().substring(path.length());
                if (fileName.contains("/")) continue; //After the path, there should be no subdirectories

                for (Map.Entry<String, String> version : versionToUpdate.entrySet()) {
                    String newFileName = fileName.replace(version.getKey(), version.getValue());
                    if (!newFileName.equals(fileName)) {
                        log.debug("Renaming file: {} to {}", fileName, newFileName);
                        files.put(path + newFileName, entry.getValue());
                        files.remove(entry.getKey());
                        break; //Only rename once per file
                    }
                }
            }

            //Return the updated metadata as a string
            return XmlUtils.toString(document);
        } catch (Throwable t) {
            throw new IOException("Failed to increment Maven snapshot version", t);
        }
    }

}
