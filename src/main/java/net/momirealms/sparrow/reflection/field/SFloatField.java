package net.momirealms.sparrow.reflection.field;

import org.jetbrains.annotations.Nullable;

public abstract class SFloatField {

    public abstract float get(@Nullable Object instance);

    public abstract void set(@Nullable Object instance, float value);
}