package net.momirealms.sparrow.reflection.constructor;

import net.momirealms.sparrow.reflection.SReflection;
import net.momirealms.sparrow.reflection.exception.SparrowReflectionException;

import java.lang.reflect.Constructor;

final class UnsafeConstructor extends SConstructor {
    private final Class<?> clazz;

    UnsafeConstructor(Constructor<?> constructor) {
        this.clazz = constructor.getDeclaringClass();
    }

    @Override
    public Object newInstance(Object... args) {
        try {
            return SReflection.UNSAFE.allocateInstance(this.clazz);
        } catch (InstantiationException e) {
            throw new SparrowReflectionException("Failed to create " + this.clazz.getName() + " instance with unsafe methods", e);
        }
    }
}
