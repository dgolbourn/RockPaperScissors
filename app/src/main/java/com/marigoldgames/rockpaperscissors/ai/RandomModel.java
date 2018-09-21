package com.marigoldgames.rockpaperscissors.ai;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

public class RandomModel<T> implements Model<T> {
    private final T[] values;
    private final Random rand = new Random();
    private final double confidence;
    private T nextGuess;
    private Runnable selectedEvent = new Runnable() {
        @Override
        public void run() {

        }
    };

    public RandomModel(final T[] values) {
        this.values = values;
        confidence = 1d / values.length;
    }

    @Override
    public Confidence<T> predictNext() {
        return new Confidence<>(nextGuess, confidence);
    }

    @Override
    public double chanceOf(final T event) {
        return confidence;
    }

    @Override
    public void recordEvent(final T event) {
        nextGuess = values[rand.nextInt(values.length)];
    }

    @Override
    public int read(final InputStream is) throws IOException {
        return 0;
    }

    @Override
    public void write(final OutputStream os) throws IOException {
    }

    @Override
    public String status() {
        final StringBuilder builder = new StringBuilder();
        builder
                .append("Model: ").append(toString()).append("\n")
                .append("Best guess ").append(nextGuess.toString()).append(" confidence: ").append(confidence).append("\n");
        return builder.toString();
    }

    public void setSelectedEvent(Runnable selectedEvent) {
        this.selectedEvent = selectedEvent;
    }

    @Override
    public void selected() {
        selectedEvent.run();
    }
}
