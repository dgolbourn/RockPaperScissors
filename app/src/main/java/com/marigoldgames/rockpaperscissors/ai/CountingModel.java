package com.marigoldgames.rockpaperscissors.ai;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Random;

public class CountingModel<T> implements Model<T> {
    private final Map<T, Integer> counts = new HashMap<>();

    private final Queue<T> history = new LinkedList<>();
    private final int historyDepth;
    private final BoundedDouble bounded;
    private Map.Entry<T, Integer> bestGuess;
    private Runnable selectedEvent = new Runnable() {
        @Override
        public void run() {

        }
    };

    public CountingModel(final int historyDepth, final BoundedDouble bounded, final T[] values) {
        this.historyDepth = historyDepth;
        this.bounded = bounded;
        bestGuess = new AbstractMap.SimpleImmutableEntry<T, Integer>(values[new Random().nextInt(values.length)], 0);
    }

    public void setSelectedEvent(Runnable selectedEvent) {
        this.selectedEvent = selectedEvent;
    }

    @Override
    public Confidence<T> predictNext() {
        return new Confidence<T>(bestGuess.getKey(), bounded.apply((double) (bestGuess.getValue()) / history.size()));
    }

    @Override
    public double chanceOf(final T event) {
        Integer count = counts.get(event);
        if (count == null) {
            count = 0;
        }
        return bounded.apply(((double) count) / history.size());
    }

    @Override
    public void recordEvent(final T event) {
        while (history.size() >= historyDepth) {
            final T oldEvent = history.poll();
            final Integer integer = counts.get(oldEvent);
            if (integer != null && integer > 0) {
                counts.put(oldEvent, integer - 1);
            }
        }
        history.add(event);
        Integer integer = counts.get(event);
        if (integer == null) {
            integer = 0;
        }
        counts.put(event, integer + 1);

        int bestCount = Integer.MIN_VALUE;
        for (Map.Entry<T, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > bestCount) {
                bestGuess = entry;
            }
        }
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
                .append("Best guess: ").append(bestGuess.getKey().toString()).append(" confidence: ").append(bounded.apply((double) (bestGuess.getValue()) / history.size())).append("\n");
        return builder.toString();
    }

    @Override
    public void selected() {
        selectedEvent.run();
    }
}
