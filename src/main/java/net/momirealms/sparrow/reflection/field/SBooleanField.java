package net.momirealms.sparrow.reflection.field;

import org.jetbrains.annotations.Nullable;

public abstract class SBooleanField {

    public abstract boolean get(@Nullable Object instance);

    public abstract void set(@Nullable Object instance, boolean value);
}