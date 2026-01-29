package net.momirealms.sparrow.reflection.field;

import org.jetbrains.annotations.Nullable;

public abstract class SIntField {

    public abstract int get(@Nullable Object instance);

    public abstract void set(@Nullable Object instance, int value);
}
