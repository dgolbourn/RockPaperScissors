package com.marigoldgames.rockpaperscissors.ai;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IKnowYouKnow<T> implements Model<T> {
    private final BoundedDouble bounded;
    private Confidence<Model<T>> model;
    private Confidence<Model<T>> selfModel;
    private BetterThanVisitor<T> betterThan;
    private WorseThanVisitor<T> worseThan;
    private SelectionStrategy strategy;
    private boolean useModel = true;
    private Confidence<T> bestGuess = new Confidence<>(null, 1d);
    private Runnable selectedEvent = new Runnable() {
        @Override
        public void run() {

        }
    };

    public IKnowYouKnow(final BoundedDouble bounded) {
        this.bounded = bounded;
    }

    public void selectModelBasedOn(final SelectionStrategy strategy) {
        this.strategy = strategy;
    }

    public void setBetterThan(final BetterThanVisitor<T> betterThan) {
        this.betterThan = new BetterThanVisitor<T>() {
            @Override
            public T apply(T event) {
                return betterThan.apply(betterThan.apply(event));
            }
        };
    }

    public void setWorseThan(final WorseThanVisitor<T> worseThan) {
        this.worseThan = new WorseThanVisitor<T>() {
            @Override
            public T apply(T event) {
                return worseThan.apply(worseThan.apply(event));
            }
        };
    }

    public void setModel(final Model<T> model, final double confidence) {
        this.model = new Confidence<>(model, confidence);
    }

    public void setSelfModel(final Model<T> model, final double confidence) {
        selfModel = new Confidence<>(model, confidence);
    }

    @Override
    public Confidence<T> predictNext() {
        return bestGuess;
    }

    @Override
    public double chanceOf(final T event) {
        if (useModel) {
            return model.get().chanceOf(event);
        } else {
            return selfModel.get().chanceOf(worseThan.apply(event));
        }
    }

    @Override
    public void recordEvent(final T event) {
        model.setConfidence(model.get().chanceOf(event) * model.getConfidence());
        model.get().recordEvent(event);

        selfModel.setConfidence(selfModel.get().chanceOf(worseThan.apply(event)) * selfModel.getConfidence());

        final double confidence = model.getConfidence() + selfModel.getConfidence();
        model.setConfidence(bounded.apply(model.getConfidence() / confidence));
        selfModel.setConfidence(bounded.apply(selfModel.getConfidence() / confidence));

        final boolean oldUseModel = useModel;
        switch (strategy) {
            default:
            case MOST_CONFIDENT_PICKS:
                useModel = model.get().predictNext().getConfidence() * model.getConfidence() > selfModel.get().predictNext().getConfidence() * selfModel.getConfidence();
                break;
            case BEST_RECORD_PICKS:
                useModel = model.getConfidence() > selfModel.getConfidence();
                break;
        }

        if (useModel) {
            bestGuess = model.get().predictNext();
        } else {
            final Confidence<T> bestGuess = selfModel.get().predictNext();
            this.bestGuess = new Confidence<T>(betterThan.apply(bestGuess.get()), bestGuess.getConfidence());
        }

        final T nextEvent = model.get().predictNext().get();
        if (nextEvent != null) {
            selfModel.get().recordEvent(nextEvent);
        }

        if (useModel != oldUseModel) {
            if (useModel) {
                model.get().selected();
            } else {
                selfModel.get().selected();
            }
        }
    }

    @Override
    public int read(final InputStream is) throws IOException {
        int read = model.get().read(is);
        if (read > 0) {
            read += selfModel.get().read(is);
        }
        return read;
    }

    @Override
    public void write(final OutputStream os) throws IOException {
        model.get().write(os);
        selfModel.get().write(os);
    }

    @Override
    public String status() {
        final StringBuilder builder = new StringBuilder();
        builder
                .append("Model: ").append(toString()).append("\n")
                .append("Best guess: ").append(bestGuess.get().toString()).append("\n")
                .append("Sub-model: ").append(model.get().toString()).append(" confidence: ").append(model.getConfidence()).append("\n")
                .append(model.get().status())
                .append("Sub-model: ").append(selfModel.get().toString()).append(" confidence: ").append(selfModel.getConfidence()).append("\n")
                .append(selfModel.get().status());
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
