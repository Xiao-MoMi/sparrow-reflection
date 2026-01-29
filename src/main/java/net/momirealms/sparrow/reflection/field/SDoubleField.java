package net.momirealms.sparrow.reflection.field;

import org.jetbrains.annotations.Nullable;

public abstract class SDoubleField {

    public abstract double get(@Nullable Object instance);

    public abstract void set(@Nullable Object instance, double value);
}