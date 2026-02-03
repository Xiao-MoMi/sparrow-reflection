package net.momirealms.sparrow.reflection.constructor;

import net.momirealms.sparrow.reflection.SReflection;
import net.momirealms.sparrow.reflection.exception.SparrowReflectionException;

public final class UnsafeConstructor {
    private final Class<?> clazz;

    public UnsafeConstructor(Class<?> clazz) {
        this.clazz = clazz;
    }

    public Object newInstance() {
        try {
            return SReflection.UNSAFE.allocateInstance(this.clazz);
        } catch (InstantiationException e) {
            throw new SparrowReflectionException("Failed to create " + this.clazz.getName() + " instance with unsafe methods", e);
        }
    }
}
