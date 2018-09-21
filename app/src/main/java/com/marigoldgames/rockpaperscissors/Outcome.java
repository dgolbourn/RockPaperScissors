package com.marigoldgames.rockpaperscissors;

public enum Outcome {
    LOSE,
    DRAW,
    WIN;

    public static int sign(final Outcome o) {
        switch (o) {
            case LOSE:
                return -1;
            default:
            case DRAW:
                return 0;
            case WIN:
                return 1;
        }
    }
}
