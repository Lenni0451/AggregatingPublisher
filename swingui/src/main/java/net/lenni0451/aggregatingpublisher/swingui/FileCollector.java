package net.lenni0451.aggregatingpublisher.swingui;

import net.lenni0451.aggregatingpublisher.services.AbstractFileCollector;

public class FileCollector extends AbstractFileCollector {

    @Override
    protected void onFileAdded(String path) {
        if (this.files.size() == 1) Window.open();
        Window.addFile(path);
    }

    @Override
    protected void onClear() {
        Window.clearFiles();
    }

}
