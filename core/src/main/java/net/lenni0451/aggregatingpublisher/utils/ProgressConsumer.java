package net.lenni0451.aggregatingpublisher.utils;

@FunctionalInterface
public interface ProgressConsumer {

    void accept(final float progress, final int step, final int totalSteps);

    default void accept(final float progress) {
        this.accept(progress, 1, 1);
    }

}
