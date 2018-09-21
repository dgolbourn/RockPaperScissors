package com.marigoldgames.rockpaperscissors;

import java.util.Random;

public enum Move {
    NONE,
    ROCK,
    PAPER,
    SCISSORS;

    static private Random rand = new Random();

    static public Move randomMove() {
        final Move[] moves = values();
        return moves[rand.nextInt(moves.length - 1) + 1];
    }

    static public Move betterThan(final Move move) {
        if (move == null) {
            return randomMove();
        }
        switch (move) {
            default:
            case NONE:
                return randomMove();
            case ROCK:
                return PAPER;
            case PAPER:
                return SCISSORS;
            case SCISSORS:
                return ROCK;
        }
    }

    static public Move noneIfNull(final Move move) {
        if (move == null) {
            return NONE;
        } else {
            return move;
        }
    }

    static public Move worseThan(final Move move) {
        if (move == null) {
            return randomMove();
        }
        switch (move) {
            default:
            case NONE:
                return randomMove();
            case ROCK:
                return SCISSORS;
            case PAPER:
                return ROCK;
            case SCISSORS:
                return PAPER;
        }
    }

    static public Outcome toOutcome(final Move subject, final Move opponent) {
        if (subject.equals(opponent)) {
            return Outcome.DRAW;
        } else if (subject.equals(betterThan(opponent))) {
            return Outcome.WIN;
        } else if (subject.equals(worseThan(opponent))) {
            return Outcome.LOSE;
        } else {
            return Outcome.DRAW;
        }
    }
}
