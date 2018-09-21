package com.marigoldgames.rockpaperscissors.ai;

public interface WorseThanVisitor<T> {
    T apply(T event);
}
