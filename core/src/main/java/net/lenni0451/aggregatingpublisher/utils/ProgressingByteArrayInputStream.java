package net.lenni0451.aggregatingpublisher.utils;

import java.io.InputStream;

public class ProgressingByteArrayInputStream extends InputStream {

    private final ProgressConsumer progressConsumer;
    private final byte[] buf;
    private int pos = 0;

    public ProgressingByteArrayInputStream(final ProgressConsumer progressConsumer, final byte[] buf) {
        this.progressConsumer = progressConsumer;
        this.buf = buf;
    }

    @Override
    public int read() {
        if (this.pos >= this.buf.length) return -1;
        this.pos++;
        this.progressConsumer.accept(1F / this.buf.length * this.pos);
        return this.buf[this.pos] & 0xFF;
    }

    @Override
    public int read(byte[] b, int off, int len) {
        if (this.pos >= this.buf.length) return -1;
        int bytesRead = Math.min(len, this.buf.length - this.pos);
        System.arraycopy(this.buf, this.pos, b, off, bytesRead);
        this.pos += bytesRead;
        this.progressConsumer.accept(1F / this.buf.length * this.pos);
        return bytesRead;
    }

    @Override
    public int available() {
        return this.buf.length - this.pos;
    }

}
