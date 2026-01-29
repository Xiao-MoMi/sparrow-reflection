package net.momirealms.sparrow.reflection.field;

import org.jetbrains.annotations.Nullable;

public abstract class SLongField {

    public abstract long get(@Nullable Object instance);

    public abstract void set(@Nullable Object instance, long value);
}