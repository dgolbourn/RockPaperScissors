package com.marigoldgames.rockpaperscissors.ai;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface Model<T> {
    Confidence<T> predictNext();

    double chanceOf(final T event);

    void recordEvent(final T event);

    int read(final InputStream is) throws IOException;

    void write(final OutputStream os) throws IOException;

    String status();

    void selected();
}
