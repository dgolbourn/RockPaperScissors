package com.marigoldgames.rockpaperscissors.ai;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

public class BranchPredictor {
    final int mask;
    final byte predictors[];

    public BranchPredictor(final int maxHistoryDepth) {
        if (maxHistoryDepth > 31 || maxHistoryDepth < 1) {
            throw new IllegalArgumentException();
        }
        final long states = 1L << maxHistoryDepth;
        int predictorSize = (int) (states / 4);
        if (predictorSize < 1) {
            predictorSize = 1;
        }
        predictors = new byte[predictorSize];
        mask = (int) (states - 1);
    }

    private static boolean predictNext(final byte[] predictors, final int history) {
        final int shift = (history & 0x03) * 2;
        final int index = history >>> 2;
        final boolean lookup[] = {false, false, true, true};
        return lookup[(predictors[index] >>> shift) & 0x03];
    }

    private static void updatePredictor(final byte[] predictors, final int history, final boolean branch) {
        final byte b = (byte) (branch ? 0x01 : 0x00);
        final int shift = (history & 0x03) * 2;
        final int index = history >>> 2;
        final byte lookup[] = {0x00, 0x00, 0x01, 0x02, 0x01, 0x02, 0x03, 0x03};

        byte p = predictors[index];
        byte q = lookup[b * 4 + ((p >>> shift) & 0x03)];
        q <<= shift;
        p &= ~(0x03 << shift);
        p |= q;
        predictors[index] = p;
    }

    public int updateHistory(final int history, final boolean branch) {
        return (((history & mask) << 1) | (branch ? 0x01 : 0x00)) & mask;
    }

    public int read(final InputStream is) throws IOException {
        return is.read(predictors);
    }

    public void write(final OutputStream os) throws IOException {
        os.write(predictors);
    }

    public void randomise() {
        new Random().nextBytes(predictors);
    }

    public void recordEvent(final int history, final boolean branch) {
        updatePredictor(predictors, history & mask, branch);
    }

    public boolean predictEvent(final int history) {
        return predictNext(predictors, history & mask);
    }
}
