package com.marigoldgames.rockpaperscissors.ai;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;

public class BayesSelector<T> implements Model<T> {
    private final Collection<Confidence<Model<T>>> models = new ArrayList<>();
    private final BoundedDouble bounded;
    private Model<T> currentModel;
    private SelectionStrategy strategy;
    private Runnable selectedEvent = new Runnable() {
        @Override
        public void run() {

        }
    };

    public BayesSelector(final BoundedDouble bounded) {
        this.bounded = bounded;
    }

    public void selectModelBasedOn(final SelectionStrategy strategy) {
        this.strategy = strategy;
    }

    public void addModel(final Model<T> model, final double prior) {
        models.add(new Confidence<>(model, prior));
    }

    public void setCurrentModel(final Model<T> model) {
        currentModel = model;
    }

    @Override
    public void recordEvent(final T event) {
        double totalConfidence = 0d;
        for (final Confidence<Model<T>> model : models) {
            final double confidence = model.get().chanceOf(event) * model.getConfidence();
            model.get().recordEvent(event);
            totalConfidence += confidence;
            model.setConfidence(confidence);
        }
        for (final Confidence<Model<T>> model : models) {
            model.setConfidence(bounded.apply(model.getConfidence() / totalConfidence));
        }

        double bestConfidence = Double.MIN_VALUE;

        final Model<T> oldModel = currentModel;

        switch (strategy) {
            default:
            case MOST_CONFIDENT_PICKS:
                for (final Confidence<Model<T>> model : models) {
                    final Confidence<T> guess = model.get().predictNext();
                    final double confidence = guess.getConfidence() * model.getConfidence();
                    if (confidence >= bestConfidence) {
                        bestConfidence = confidence;
                        currentModel = model.get();
                    }
                }
                break;
            case BEST_RECORD_PICKS:
                for (final Confidence<Model<T>> model : models) {
                    final double confidence = model.getConfidence();
                    if (confidence >= bestConfidence) {
                        bestConfidence = confidence;
                        currentModel = model.get();
                    }
                }
                break;
        }

        if (currentModel != oldModel) {
            currentModel.selected();
        }
    }

    @Override
    public double chanceOf(final T event) {
        return currentModel.chanceOf(event);
    }

    @Override
    public Confidence<T> predictNext() {
        return currentModel.predictNext();
    }

    public int read(final InputStream is) throws IOException {
        int read = 0;
        for (final Confidence<Model<T>> model : models) {
            int modelRead = model.get().read(is);
            if (modelRead == -1) {
                return -1;
            } else {
                read += modelRead;
            }
        }
        return read;
    }

    public void write(final OutputStream os) throws IOException {
        for (final Confidence<Model<T>> model : models) {
            model.get().write(os);
        }
    }

    @Override
    public String status() {
        final StringBuilder builder = new StringBuilder();
        builder
                .append("Model: ").append(toString()).append("\n")
                .append("Best guess: ").append(currentModel.toString()).append("\n");

        for (Confidence<Model<T>> model : models) {
            builder.append("Sub-model: ").append(model.get().toString()).append(" confidence: ").append(model.getConfidence()).append("\n");
            builder.append(model.get().status());
        }

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
