package com.marigoldgames.rockpaperscissors.ai;

public class Confidence<T> {
    final T object;
    double confidence = 1d;

    Confidence(final T object, final double confidence) {
        this.object = object;
        this.confidence = confidence;
    }

    public double getConfidence() {
        return confidence;
    }

    void setConfidence(final double confidence) {
        this.confidence = confidence;
    }

    public T get() {
        return object;
    }
}
