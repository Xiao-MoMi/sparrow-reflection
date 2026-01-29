package net.momirealms.sparrow.reflection.field;

import org.jetbrains.annotations.Nullable;

public abstract class SCharField {

    public abstract char get(@Nullable Object instance);

    public abstract void set(@Nullable Object instance, char value);
}