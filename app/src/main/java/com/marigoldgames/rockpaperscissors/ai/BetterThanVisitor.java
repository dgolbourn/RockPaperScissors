package com.marigoldgames.rockpaperscissors.ai;

public interface BetterThanVisitor<T> {
    T apply(T event);
}
