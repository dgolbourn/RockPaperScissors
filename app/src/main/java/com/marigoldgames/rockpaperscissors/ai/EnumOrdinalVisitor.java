package com.marigoldgames.rockpaperscissors.ai;

public class EnumOrdinalVisitor<E extends Enum<E>> implements OrdinalVisitor<E> {
    @Override
    public int toOrdinal(final E type) {
        return type.ordinal();
    }
}
