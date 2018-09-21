package com.marigoldgames.rockpaperscissors;

import com.marigoldgames.rockpaperscissors.ai.BayesSelector;
import com.marigoldgames.rockpaperscissors.ai.BetterThanVisitor;
import com.marigoldgames.rockpaperscissors.ai.BoundedDouble;
import com.marigoldgames.rockpaperscissors.ai.CountingModel;
import com.marigoldgames.rockpaperscissors.ai.EnumOrdinalVisitor;
import com.marigoldgames.rockpaperscissors.ai.EnumPredictor;
import com.marigoldgames.rockpaperscissors.ai.IKnowYouKnow;
import com.marigoldgames.rockpaperscissors.ai.Model;
import com.marigoldgames.rockpaperscissors.ai.RandomModel;
import com.marigoldgames.rockpaperscissors.ai.SelectionStrategy;
import com.marigoldgames.rockpaperscissors.ai.WorseThanVisitor;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

public class Trainer {
    private static final int SMALLEST_INTERESTING_AMOUNT = 3;
    private final Model<Move> model;
    private final Map<AbstractMap.SimpleImmutableEntry<Move, Move>, Integer> tells = new HashMap<>();
    private final Map<AbstractMap.SimpleImmutableEntry<Move, Move>, Integer> responses = new HashMap<>();
    private Move nextMove = Move.randomMove();
    private int winSession = 0;
    private int drawSession = 0;
    private int loseSession = 0;
    private int winStreakSession = 0;
    private int drawStreakSession = 0;
    private int loseStreakSession = 0;
    private int winAllTime = 0;
    private int drawAllTime = 0;
    private int loseAllTime = 0;
    private int winStreakAllTime = 0;
    private int drawStreakAllTime = 0;
    private int loseStreakAllTime = 0;
    private int rockSession = 0;
    private int paperSession = 0;
    private int scissorsSession = 0;
    private int rockStreakSession = 0;
    private int paperStreakSession = 0;
    private int scissorsStreakSession = 0;
    private int rockAllTime = 0;
    private int paperAllTime = 0;
    private int scissorsAllTime = 0;
    private int rockStreakAllTime = 0;
    private int paperStreakAllTime = 0;
    private int scissorsStreakAllTime = 0;
    private int winStreakNow = 0;
    private int drawStreakNow = 0;
    private int loseStreakNow = 0;
    private int rockStreakNow = 0;
    private int paperStreakNow = 0;
    private int scissorsStreakNow = 0;
    private int eventsSession = 0;
    private int eventsAllTime = 0;
    private Move lastEvent = Move.randomMove();
    private Move lastMove = Move.randomMove();
    private Event event = new Event() {
        @Override
        public void apply(String eventText, EventType id) {
        }
    };
    private ModelEvents modelEvents = new ModelEvents();

    public Trainer(final SelectionStrategy selectionStrategy,
                   final int maxHistoryDepth,
                   final double predictorConfidence,
                   final int countingHistoryDepth,
                   final int metaHistoryDepth,
                   final double metaConfidence) {
        final BoundedDouble bounded = new BoundedDouble(.9999d, .0001d, 1d / 3d);

        // set up Bayes selector composite model
        final BayesSelector<Move> bayesSelector = new BayesSelector<>(bounded);
        bayesSelector.selectModelBasedOn(selectionStrategy);
        bayesSelector.setSelectedEvent(new Runnable() {
            @Override
            public void run() {
                modelEvents.modelEvent();
            }
        });

        // set up pattern predictor model
        final EnumOrdinalVisitor<Move> enumOrdinalVisitor = new EnumOrdinalVisitor<>();

        for (int i = 1; i <= maxHistoryDepth; ++i) {
            final EnumPredictor<Move> enumPredictor = new EnumPredictor<>(i, Move.values(), enumOrdinalVisitor, predictorConfidence);
            bayesSelector.addModel(enumPredictor, 1d);
            bayesSelector.setCurrentModel(enumPredictor);
            enumPredictor.setSelectedEvent(new Runnable() {
                @Override
                public void run() {
                    modelEvents.predictorEvent();
                }
            });
        }

        // set up random model
        final Move[] moves = {Move.ROCK, Move.PAPER, Move.SCISSORS};
        final RandomModel<Move> randomModel = new RandomModel<>(moves);
        randomModel.setSelectedEvent(new Runnable() {
            @Override
            public void run() {
                modelEvents.randomEvent();
            }
        });
        bayesSelector.addModel(randomModel, 1d);

        // set up proportional model
        final CountingModel<Move> countingModel = new CountingModel<>(countingHistoryDepth, bounded, Move.values());
        countingModel.setSelectedEvent(new Runnable() {
            @Override
            public void run() {
                modelEvents.countingEvent();
            }
        });
        bayesSelector.addModel(countingModel, 1d);

        // set up meta model
        final IKnowYouKnow<Move> model = new IKnowYouKnow<>(bounded);
        model.selectModelBasedOn(selectionStrategy);
        model.setBetterThan(new BetterThanVisitor<Move>() {
            @Override
            public Move apply(final Move event) {
                return Move.betterThan(event);
            }
        });
        model.setWorseThan(new WorseThanVisitor<Move>() {
            @Override
            public Move apply(final Move event) {
                return Move.worseThan(event);
            }
        });
        model.setModel(bayesSelector, 1d);

        final EnumPredictor<Move> metaEnumPredictor = new EnumPredictor<>(metaHistoryDepth, Move.values(), enumOrdinalVisitor, metaConfidence);
        metaEnumPredictor.setSelectedEvent(new Runnable() {
            @Override
            public void run() {
                modelEvents.metaModelEvent();
            }
        });
        model.setSelfModel(metaEnumPredictor, 1d);

        this.model = model;
    }

    private static AbstractMap.SimpleImmutableEntry<Move, Move> getFavourite(final Map<AbstractMap.SimpleImmutableEntry<Move, Move>, Integer> map) {
        AbstractMap.SimpleImmutableEntry<Move, Move> favourite = new AbstractMap.SimpleImmutableEntry<>(Move.NONE, Move.NONE);
        int count = Integer.MIN_VALUE;

        for (final Map.Entry<AbstractMap.SimpleImmutableEntry<Move, Move>, Integer> entry : map.entrySet()) {
            final int val = entry.getValue();
            if (val >= count) {
                count = val;
                favourite = entry.getKey();
            }
        }
        return favourite;
    }

    private static Integer zeroIfNull(final Integer integer) {
        if (integer == null) {
            return 0;
        }
        return integer;
    }

    public void setEvent(final Event event) {
        this.event = event;
    }

    public void resetSession() {
        winSession = 0;
        drawSession = 0;
        loseSession = 0;
        winStreakSession = 0;
        drawStreakSession = 0;
        loseStreakSession = 0;
        rockSession = 0;
        paperSession = 0;
        scissorsSession = 0;
        rockStreakSession = 0;
        paperStreakSession = 0;
        scissorsStreakSession = 0;
        winStreakNow = 0;
        drawStreakNow = 0;
        loseStreakNow = 0;
        rockStreakNow = 0;
        paperStreakNow = 0;
        scissorsStreakNow = 0;
        eventsSession = 0;
        modelEvents.resetEvents();
    }

    public void resetAll() {
        winSession = 0;
        drawSession = 0;
        loseSession = 0;
        winStreakSession = 0;
        drawStreakSession = 0;
        loseStreakSession = 0;
        winAllTime = 0;
        drawAllTime = 0;
        loseAllTime = 0;
        winStreakAllTime = 0;
        drawStreakAllTime = 0;
        loseStreakAllTime = 0;
        rockSession = 0;
        paperSession = 0;
        scissorsSession = 0;
        rockStreakSession = 0;
        paperStreakSession = 0;
        scissorsStreakSession = 0;
        rockAllTime = 0;
        paperAllTime = 0;
        scissorsAllTime = 0;
        rockStreakAllTime = 0;
        paperStreakAllTime = 0;
        scissorsStreakAllTime = 0;
        winStreakNow = 0;
        drawStreakNow = 0;
        loseStreakNow = 0;
        rockStreakNow = 0;
        paperStreakNow = 0;
        scissorsStreakNow = 0;
        eventsSession = 0;
        eventsAllTime = 0;
        modelEvents.resetEvents();
    }

    public int getWinSession() {
        return winSession;
    }

    public int getDrawSession() {
        return drawSession;
    }

    public int getLoseSession() {
        return loseSession;
    }

    public int getWinStreakSession() {
        return winStreakSession;
    }

    public int getDrawStreakSession() {
        return drawStreakSession;
    }

    public int getLoseStreakSession() {
        return loseStreakSession;
    }

    public int getWinAllTime() {
        return winAllTime;
    }

    public int getDrawAllTime() {
        return drawAllTime;
    }

    public int getLoseAllTime() {
        return loseAllTime;
    }

    public int getWinStreakAllTime() {
        return winStreakAllTime;
    }

    public int getDrawStreakAllTime() {
        return drawStreakAllTime;
    }

    public int getLoseStreakAllTime() {
        return loseStreakAllTime;
    }

    public int getRockSession() {
        return rockSession;
    }

    public int getPaperSession() {
        return paperSession;
    }

    public int getScissorsSession() {
        return scissorsSession;
    }

    public int getRockStreakSession() {
        return rockStreakSession;
    }

    public int getPaperStreakSession() {
        return paperStreakSession;
    }

    public int getScissorsStreakSession() {
        return scissorsStreakSession;
    }

    public int getRockAllTime() {
        return rockAllTime;
    }

    public int getPaperAllTime() {
        return paperAllTime;
    }

    public int getScissorsAllTime() {
        return scissorsAllTime;
    }

    public int getRockStreakAllTime() {
        return rockStreakAllTime;
    }

    public int getPaperStreakAllTime() {
        return paperStreakAllTime;
    }

    public int getScissorsStreakAllTime() {
        return scissorsStreakAllTime;
    }

    public int getEventsSession() {
        return eventsSession;
    }

    public int getEventsAllTime() {
        return eventsAllTime;
    }

    public AbstractMap.SimpleImmutableEntry<Move, Move> getFavouriteTell() {
        return getFavourite(tells);
    }

    public AbstractMap.SimpleImmutableEntry<Move, Move> getFavouriteResponse() {
        return getFavourite(responses);
    }

    public String getFavouriteModel() {
        return modelEvents.favouriteModel();
    }

    public int getTellCount(final AbstractMap.SimpleImmutableEntry<Move, Move> tell) {
        return zeroIfNull(tells.get(tell));
    }

    public int getResponseCount(final AbstractMap.SimpleImmutableEntry<Move, Move> response) {
        return zeroIfNull(responses.get(response));
    }

    public int read(final FileInputStream fis) throws IOException {
        final ByteBuffer intBuffer = ByteBuffer.allocate(4);
        int totalRead = 0;
        int read = 0;

        read = fis.read(intBuffer.array());
        if (read < 0) {
            return -1;
        }
        eventsAllTime = intBuffer.asIntBuffer().get();
        totalRead += read;

        read = fis.read(intBuffer.array());
        if (read < 0) {
            return -1;
        }
        winAllTime = intBuffer.asIntBuffer().get();
        totalRead += read;

        read = fis.read(intBuffer.array());
        if (read < 0) {
            return -1;
        }
        drawAllTime = intBuffer.asIntBuffer().get();
        totalRead += read;

        read = fis.read(intBuffer.array());
        if (read < 0) {
            return -1;
        }
        loseAllTime = intBuffer.asIntBuffer().get();
        totalRead += read;

        read = fis.read(intBuffer.array());
        if (read < 0) {
            return -1;
        }
        winStreakAllTime = intBuffer.asIntBuffer().get();
        totalRead += read;

        read = fis.read(intBuffer.array());
        if (read < 0) {
            return -1;
        }
        drawStreakAllTime = intBuffer.asIntBuffer().get();
        totalRead += read;

        read = fis.read(intBuffer.array());
        if (read < 0) {
            return -1;
        }
        loseStreakAllTime = intBuffer.asIntBuffer().get();
        totalRead += read;

        read = fis.read(intBuffer.array());
        if (read < 0) {
            return -1;
        }
        rockAllTime = intBuffer.asIntBuffer().get();
        totalRead += read;

        read = fis.read(intBuffer.array());
        if (read < 0) {
            return -1;
        }
        paperAllTime = intBuffer.asIntBuffer().get();
        totalRead += read;

        read = fis.read(intBuffer.array());
        if (read < 0) {
            return -1;
        }
        scissorsAllTime = intBuffer.asIntBuffer().get();
        totalRead += read;

        read = fis.read(intBuffer.array());
        if (read < 0) {
            return -1;
        }
        rockStreakAllTime = intBuffer.asIntBuffer().get();
        totalRead += read;

        read = fis.read(intBuffer.array());
        if (read < 0) {
            return -1;
        }
        paperStreakAllTime = intBuffer.asIntBuffer().get();
        totalRead += read;

        read = fis.read(intBuffer.array());
        if (read < 0) {
            return -1;
        }
        scissorsStreakAllTime = intBuffer.asIntBuffer().get();
        totalRead += read;

        read = model.read(fis);
        if (read < 0) {
            return -1;
        }
        totalRead += read;

        return totalRead;
    }

    public void write(final FileOutputStream fos) throws IOException {
        final ByteBuffer intBuffer = ByteBuffer.allocate(4);

        intBuffer.asIntBuffer().put(eventsAllTime);
        fos.write(intBuffer.array());

        intBuffer.asIntBuffer().put(winAllTime);
        fos.write(intBuffer.array());

        intBuffer.asIntBuffer().put(drawAllTime);
        fos.write(intBuffer.array());

        intBuffer.asIntBuffer().put(loseAllTime);
        fos.write(intBuffer.array());

        intBuffer.asIntBuffer().put(winStreakAllTime);
        fos.write(intBuffer.array());

        intBuffer.asIntBuffer().put(drawStreakAllTime);
        fos.write(intBuffer.array());

        intBuffer.asIntBuffer().put(loseStreakAllTime);
        fos.write(intBuffer.array());

        intBuffer.asIntBuffer().put(rockAllTime);
        fos.write(intBuffer.array());

        intBuffer.asIntBuffer().put(paperAllTime);
        fos.write(intBuffer.array());

        intBuffer.asIntBuffer().put(scissorsAllTime);
        fos.write(intBuffer.array());

        intBuffer.asIntBuffer().put(rockStreakAllTime);
        fos.write(intBuffer.array());

        intBuffer.asIntBuffer().put(paperStreakAllTime);
        fos.write(intBuffer.array());

        intBuffer.asIntBuffer().put(scissorsStreakAllTime);
        fos.write(intBuffer.array());

        model.write(fos);
    }

    public Outcome onMove(final Move move) {
        ++eventsSession;
        ++eventsAllTime;
        modelEvents.countEvent();

        model.recordEvent(move);

        final AbstractMap.SimpleImmutableEntry<Move, Move> newEntry = new AbstractMap.SimpleImmutableEntry<>(lastEvent, move);
        tells.put(newEntry, zeroIfNull(tells.get(newEntry)) + 1);

        final AbstractMap.SimpleImmutableEntry<Move, Move> entry = new AbstractMap.SimpleImmutableEntry<>(lastMove, move);
        responses.put(entry, zeroIfNull(responses.get(entry)) + 1);

        lastMove = nextMove;
        lastEvent = move;

        boolean rockStreakEvent = false;
        boolean paperStreakEvent = false;
        boolean scissorsStreakEvent = false;
        boolean winStreakEvent = false;
        boolean drawStreakEvent = false;
        boolean loseStreakEvent = false;

        switch (move) {
            case ROCK:
                ++rockAllTime;
                ++rockSession;
                ++rockStreakNow;
                paperStreakNow = 0;
                scissorsStreakNow = 0;
                if (rockStreakNow > rockStreakSession) {
                    rockStreakSession = rockStreakNow;
                    if (rockStreakSession > rockStreakAllTime) {
                        rockStreakEvent = true;
                        rockStreakAllTime = rockStreakSession;
                    }
                }
                break;
            case PAPER:
                ++paperAllTime;
                ++paperSession;
                ++paperStreakNow;
                rockStreakNow = 0;
                scissorsStreakNow = 0;
                if (paperStreakNow > paperStreakSession) {
                    paperStreakSession = paperStreakNow;
                    if (paperStreakSession > paperStreakAllTime) {
                        paperStreakEvent = true;
                        paperStreakAllTime = paperStreakSession;
                    }
                }
                break;
            case SCISSORS:
                ++scissorsAllTime;
                ++scissorsSession;
                ++scissorsStreakNow;
                rockStreakNow = 0;
                paperStreakNow = 0;
                if (scissorsStreakNow > scissorsStreakSession) {
                    scissorsStreakSession = scissorsStreakNow;
                    if (scissorsStreakSession > scissorsStreakAllTime) {
                        scissorsStreakEvent = true;
                        scissorsStreakAllTime = scissorsStreakSession;
                    }
                }
                break;
            default:
            case NONE:
                break;
        }

        final Outcome outcome = Move.toOutcome(move, nextMove);
        switch (outcome) {
            case WIN:
                ++winAllTime;
                ++winSession;
                ++winStreakNow;
                drawStreakNow = 0;
                loseStreakNow = 0;
                if (winStreakNow > winStreakSession) {
                    winStreakSession = winStreakNow;
                    winStreakEvent = true;
                    if (winStreakSession > winStreakAllTime) {
                        winStreakAllTime = winStreakSession;
                    }
                }
                break;
            case LOSE:
                ++loseAllTime;
                ++loseSession;
                ++loseStreakNow;
                winStreakNow = 0;
                drawStreakNow = 0;
                if (loseStreakNow > loseStreakSession) {
                    loseStreakSession = loseStreakNow;
                    loseStreakEvent = true;
                    if (loseStreakSession > loseStreakAllTime) {
                        loseStreakAllTime = loseStreakSession;
                    }
                }
                break;
            default:
            case DRAW:
                ++drawAllTime;
                ++drawSession;
                ++drawStreakNow;
                winStreakNow = 0;
                loseStreakNow = 0;
                if (drawStreakNow > drawStreakSession) {
                    drawStreakSession = drawStreakNow;
                    drawStreakEvent = true;
                    if (drawStreakSession > drawStreakAllTime) {
                        drawStreakAllTime = drawStreakSession;
                    }
                }
                break;
        }

        nextMove = Move.betterThan(model.predictNext().get());

        if (rockStreakEvent) {
            if (rockStreakSession > SMALLEST_INTERESTING_AMOUNT) {
                event.apply("You love to ROCK! " + rockStreakSession + " Rocks in a row!", EventType.HINT);
            }
        }

        if (paperStreakEvent) {
            if (paperStreakSession > SMALLEST_INTERESTING_AMOUNT) {
                event.apply("Papers please! " + paperStreakSession + " Papers in a row!", EventType.HINT);
            }
        }

        if (scissorsStreakEvent) {
            if (scissorsStreakSession > SMALLEST_INTERESTING_AMOUNT) {
                event.apply("Don't run with scissors! " + scissorsStreakSession + " Scissors in a row!", EventType.HINT);
            }
        }

        if (winStreakEvent) {
            if (winStreakSession > SMALLEST_INTERESTING_AMOUNT) {
                event.apply("Excellent, you're on a WINNING STREAK! " + winStreakSession + " wins in a row!", EventType.ACHIEVEMENT);
            }
        }

        if (drawStreakEvent) {
            if (drawStreakSession > SMALLEST_INTERESTING_AMOUNT) {
                event.apply("Stalemate! We are playing a similar strategy, use this to your advantage. " + drawStreakSession + " draws in a row!", EventType.HINT);
            }
        }

        if (loseStreakEvent) {
            if (loseStreakSession > SMALLEST_INTERESTING_AMOUNT) {
                event.apply("You are too predictable. Try a different strategy! " + loseStreakSession + " losses in a row!", EventType.WARNING);
            }
        }

        return outcome;
    }

    public String odds() {
        if (winSession == 0) {
            if (loseSession == 0) {
                return "0:0";
            } else {
                return "0:1";
            }
        } else if (loseSession == 0) {
            return "1:0";
        } else if (winSession > loseSession) {
            return ((winSession + (loseSession / 2)) / loseSession) + ":1";
        } else if (winSession < loseSession) {
            return "1:" + ((loseSession + (winSession / 2)) / winSession);
        } else {
            return "1:1";
        }
    }

    public double ratioSession() {
        return (double) (winSession - loseSession) / (winSession + loseSession + drawSession);
    }

    public double ratioAllTime() {
        return (double) (winAllTime - loseAllTime) / (winAllTime + loseAllTime + drawAllTime);
    }

    public String getStatus() {
        return new StringBuilder()
                .append("RESULTS: ")
                .append("WIN ").append(getWinSession())
                .append(" DRAW ").append(getDrawSession())
                .append(" LOSE ").append(getLoseSession())
                .append(" WIN:LOSE ").append(odds())
                .append(" DIFF ratio ").append(ratioSession())
                .append("\n")
                .append(model.status())
                .toString();
    }

    public enum EventType {
        HINT,
        ACHIEVEMENT,
        WARNING
    }

    public interface Event {
        void apply(String eventText, EventType id);
    }

    private class ModelEvents {
        boolean usingMetaModel = false;
        boolean usingPredictor = false;
        boolean usingCounting = false;
        boolean usingRandom = false;

        int metaModelEvents = 0;
        int predictorEvents = 0;
        int countingEvents = 0;
        int randomEvents = 0;

        void resetEvents() {
            metaModelEvents = 0;
            predictorEvents = 0;
            countingEvents = 0;
            randomEvents = 0;
        }

        void countEvent() {
            if (usingMetaModel) {
                ++metaModelEvents;
            } else if (usingPredictor) {
                ++predictorEvents;
            } else if (usingCounting) {
                ++countingEvents;
            } else if (usingRandom) {
                ++randomEvents;
            }
        }

        String favouriteModel() {
            final int maxEvents = Math.max(metaModelEvents, Math.max(predictorEvents, Math.max(countingEvents, randomEvents)));
            if (metaModelEvents == maxEvents) {
                return "Meta";
            } else if (predictorEvents == maxEvents) {
                return "Predictor";
            } else if (countingEvents == maxEvents) {
                return "Counter";
            } else {
                return "Random";
            }
        }

        void predictorMessage() {
            event.apply("You're moves are quite predictable. Play a different sequence of moves.", EventType.WARNING);
        }

        void countingMessage() {
            event.apply("You play some moves too often. Use other moves to be less predictable.", EventType.WARNING);
        }

        void randomMessage() {
            event.apply("Good! Your moves are hard to predict!", EventType.ACHIEVEMENT);
        }

        void metaModelMessage() {
            event.apply("Excellent!", EventType.ACHIEVEMENT);
        }

        void modelEvent() {
            if (usingMetaModel) {
                if (usingPredictor) {
                    predictorMessage();
                } else if (usingCounting) {
                    countingMessage();
                } else if (usingRandom) {
                    randomMessage();
                }
            }
            usingMetaModel = false;
        }

        void metaModelEvent() {
            if (!usingMetaModel) {
                metaModelMessage();
            }
            usingMetaModel = true;
        }

        void predictorEvent() {
            usingPredictor = true;
            usingCounting = false;
            usingRandom = false;
            if (!usingMetaModel) {
                predictorMessage();
            }
        }

        void countingEvent() {
            usingPredictor = false;
            usingCounting = true;
            usingRandom = false;
            if (!usingMetaModel) {
                countingMessage();
            }
        }

        void randomEvent() {
            usingPredictor = false;
            usingCounting = false;
            usingRandom = true;
            if (!usingMetaModel) {
                randomMessage();
            }
        }
    }
}
