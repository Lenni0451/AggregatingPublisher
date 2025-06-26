package net.lenni0451.aggregatingpublisher.utils;

@FunctionalInterface
public interface ProgressConsumer {

    void accept(float progress);

}
