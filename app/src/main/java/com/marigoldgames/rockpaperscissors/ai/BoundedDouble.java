package com.marigoldgames.rockpaperscissors.ai;

public class BoundedDouble {
    final double max;
    final double min;
    final double nan;

    public BoundedDouble(final double max,
                         final double min,
                         final double nan) {
        this.max = max;
        this.min = min;
        this.nan = nan;
    }

    public static double bounded(final double val, final double max, final double min, final double nan) {
        if (val > max) {
            return max;
        } else if (val < min) {
            return min;
        } else if (val != val) {
            return nan;
        } else {
            return val;
        }
    }

    public double apply(final double val) {
        return bounded(val, max, min, nan);
    }
}
