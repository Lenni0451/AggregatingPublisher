package net.lenni0451.aggregatingpublisher.swingui;

import net.lenni0451.aggregatingpublisher.services.DeploymentManagerService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FileCollector implements DeploymentManagerService {

    private final Map<String, byte[]> files = new ConcurrentHashMap<>();

    public Map<String, byte[]> getFiles() {
        return Map.copyOf(this.files);
    }

    public boolean isEmpty() {
        return this.files.isEmpty();
    }

    public void clearFiles() {
        this.files.clear();
        Window.clearFiles();
    }

    @Override
    public void aggregateFile(String path, byte[] file) {
        if (this.files.isEmpty()) Window.open();
        this.files.put(path, file);
        Window.addFile(path);
    }

}
