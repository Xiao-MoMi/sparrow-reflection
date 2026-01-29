package net.momirealms.sparrow.reflection.method;

import org.jetbrains.annotations.Nullable;

public abstract class SMethod {

    public abstract Object invoke(@Nullable Object instance, Object... args);
}
