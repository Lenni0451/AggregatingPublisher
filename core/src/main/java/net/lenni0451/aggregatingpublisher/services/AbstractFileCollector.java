package net.lenni0451.aggregatingpublisher.services;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractFileCollector implements DeploymentManagerService {

    protected final Map<String, byte[]> files = new ConcurrentHashMap<>();

    public Map<String, byte[]> getFiles() {
        return Map.copyOf(this.files);
    }

    public boolean isEmpty() {
        return this.files.isEmpty();
    }

    public void clearFiles() {
        this.files.clear();
        this.onClear();
    }

    @Override
    public void aggregateFile(String path, byte[] file) {
        this.files.put(path, file);
        this.onFileAdded(path);
    }

    protected abstract void onFileAdded(String path);

    protected abstract void onClear();

}
