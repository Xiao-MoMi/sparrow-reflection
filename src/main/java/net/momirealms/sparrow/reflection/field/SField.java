package net.momirealms.sparrow.reflection.field;

import org.jetbrains.annotations.Nullable;

public abstract class SField {

    public int getInt(Object instance) {
        return (int) get(instance);
    }

    public long getLong(Object instance) {
        return (long) get(instance);
    }

    public double getDouble(Object instance) {
        return (double) get(instance);
    }

    public boolean getBoolean(Object instance) {
        return (boolean) get(instance);
    }

    public float getFloat(Object instance) {
        return (float) get(instance);
    }

    public void setInt(Object instance, int value) {
        set(instance, value);
    }

    public void setLong(Object instance, long value) {
        set(instance, value);
    }

    public void setDouble(Object instance, double value) {
        set(instance, value);
    }

    public void setFloat(Object instance, float value) {
        set(instance, value);
    }

    public void setBoolean(Object instance, boolean value) {
        set(instance, value);
    }

    public abstract Object get(@Nullable Object instance);

    public abstract void set(@Nullable Object instance, @Nullable Object value);
}