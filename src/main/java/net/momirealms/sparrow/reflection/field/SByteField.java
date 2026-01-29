package net.momirealms.sparrow.reflection.field;

import org.jetbrains.annotations.Nullable;

public abstract class SByteField {

    public abstract byte get(@Nullable Object instance);

    public abstract void set(@Nullable Object instance, byte value);
}