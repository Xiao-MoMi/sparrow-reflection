package net.momirealms.sparrow.reflection.field;

import org.jetbrains.annotations.Nullable;

public abstract class SShortField {

    public abstract short get(@Nullable Object instance);

    public abstract void set(@Nullable Object instance, short value);
}