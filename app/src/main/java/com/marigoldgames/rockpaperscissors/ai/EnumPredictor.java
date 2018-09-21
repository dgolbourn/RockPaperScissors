package com.marigoldgames.rockpaperscissors.ai;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Random;

public class EnumPredictor<T> implements Model<T> {
    private static final Random random = new Random();
    private final OrdinalVisitor<T> ordinalVisitor;
    private final T[] values;
    private final int maxHistoryDepth;
    private final BranchPredictor predictor;
    private final Queue<T> historyEnums;
    private final int numBits;
    private final double bestConfidence;
    private final double otherConfidence;
    private int historyBits;
    private T bestGuess;
    private Runnable selectedEvent = new Runnable() {
        @Override
        public void run() {

        }
    };

    public EnumPredictor(final int maxHistoryDepth, final T[] values, final OrdinalVisitor<T> ordinalVisitor, final double confidence) {
        this.ordinalVisitor = ordinalVisitor;
        this.maxHistoryDepth = maxHistoryDepth;
        this.values = values;
        predictor = new BranchPredictor(nextHighestPowerOfTwo(values.length) * maxHistoryDepth);
        historyEnums = new ArrayDeque<>(maxHistoryDepth);
        numBits = logBaseTwo(values.length);
        bestConfidence = Math.max(Math.min(confidence, 1d), 0d);
        otherConfidence = (1d - bestConfidence) / (values.length - 1);
        bestGuess = values[random.nextInt(values.length)];
    }

    static private int nextHighestPowerOfTwo(int v) {
        if (v <= 1) {
            return 1;
        }
        v--;
        v |= v >>> 1;
        v |= v >>> 2;
        v |= v >>> 4;
        v |= v >>> 8;
        v |= v >>> 16;
        v++;
        return v;
    }

    static private int logBaseTwo(int v) {
        int res = 0;
        while (v > 0) {
            v >>>= 1;
            ++res;
        }
        return res;
    }

    private boolean[] bitsOf(final T event) {
        final boolean[] bits = new boolean[numBits];
        int ordinal = ordinalVisitor.toOrdinal(event);
        for (int i = 0; i < bits.length; ++i) {
            if ((ordinal & 0x01) == 0x01) {
                bits[i] = true;
            }
            ordinal >>= 1;
        }
        return bits;
    }

    private T valueOf(final boolean[] bits) {
        int ordinal = 0;
        for (int i = 0; i < bits.length; ++i) {
            if (bits[i]) {
                ordinal |= 0x01 << i;
            }
        }
        if (ordinal >= values.length || ordinal < 0) {
            return values[random.nextInt(values.length)];
        } else {
            return values[ordinal];
        }
    }

    @Override
    public Confidence<T> predictNext() {
        return new Confidence<T>(bestGuess, bestConfidence);
    }

    @Override
    public double chanceOf(final T event) {
        if (event.equals(bestGuess)) {
            return bestConfidence;
        } else {
            return otherConfidence;
        }
    }

    public void recordEvent(final T event) {
        while (historyEnums.size() >= maxHistoryDepth) {
            historyEnums.poll();
        }
        historyEnums.add(event);
        for (boolean b : bitsOf(event)) {
            predictor.recordEvent(historyBits, b);
            historyBits = predictor.updateHistory(historyBits, b);
        }

        int historyBits = this.historyBits;
        final boolean bits[] = new boolean[numBits];
        for (int i = 0; i < bits.length; ++i) {
            final boolean b = predictor.predictEvent(historyBits);
            historyBits = predictor.updateHistory(historyBits, b);
            bits[i] = b;
        }
        bestGuess = valueOf(bits);
    }

    public int read(final InputStream is) throws IOException {
        return predictor.read(is);
    }

    public void write(final OutputStream os) throws IOException {
        predictor.write(os);
    }

    @Override
    public String status() {
        final StringBuilder builder = new StringBuilder();
        builder
                .append("Model: ").append(toString()).append("\n")
                .append("Best guess: ").append(bestGuess.toString()).append(" confidence: ").append(bestConfidence).append("\n");

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