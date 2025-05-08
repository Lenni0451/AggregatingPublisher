package net.lenni0451.aggregatingpublisher.utils;

import java.io.InputStream;

public class EmptyInputStream extends InputStream {

    public static final EmptyInputStream INSTANCE = new EmptyInputStream();

    @Override
    public int read() {
        return -1;
    }

}
