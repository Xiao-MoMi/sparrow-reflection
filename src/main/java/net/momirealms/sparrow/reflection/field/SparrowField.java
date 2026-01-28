package net.momirealms.sparrow.reflection.field;

import net.momirealms.sparrow.reflection.exception.SparrowReflectionException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.Objects;

public final class SparrowField {
    public final Field field;

    public SparrowField(Field field) {
        this.field = field;
    }

    public static SparrowField of(@NotNull final Field field) {
        Objects.requireNonNull(field, "field cannot be null");
        return new SparrowField(field);
    }

    public static SparrowField ofNullable(@Nullable final Field field) {
        return field == null ? null : new SparrowField(field);
    }

    public Field field() {
        return field;
    }

    public SField mh() {
        return new MethodHandleFieldAccessor(this.field);
    }

    public SField asm() {
        try {
            return FieldAccessorFactory.create(this.field);
        } catch (Throwable e) {
            throw new SparrowReflectionException("Failed to create ASM field accessor", e);
        }
    }

    public SIntField asm$int() {
        try {
            return IntFieldAccessorFactory.create(this.field);
        } catch (Throwable e) {
            throw new SparrowReflectionException("Failed to create ASM int accessor", e);
        }
    }

    public SFloatField asm$float() {
        try {
            return FloatFieldAccessorFactory.create(this.field);
        } catch (Throwable e) {
            throw new SparrowReflectionException("Failed to create ASM float accessor", e);
        }
    }

    public SDoubleField asm$double() {
        try {
            return DoubleFieldAccessorFactory.create(this.field);
        } catch (Throwable e) {
            throw new SparrowReflectionException("Failed to create ASM double accessor", e);
        }
    }

    public SBooleanField asm$boolean() {
        try {
            return BooleanFieldAccessorFactory.create(this.field);
        } catch (Throwable e) {
            throw new SparrowReflectionException("Failed to create ASM boolean accessor", e);
        }
    }

    public SByteField asm$byte() {
        try {
            return ByteFieldAccessorFactory.create(this.field);
        } catch (Throwable e) {
            throw new SparrowReflectionException("Failed to create ASM byte accessor", e);
        }
    }

    public SShortField asm$short() {
        try {
            return ShortFieldAccessorFactory.create(this.field);
        } catch (Throwable e) {
            throw new SparrowReflectionException("Failed to create ASM short accessor", e);
        }
    }

    public SCharField asm$char() {
        try {
            return CharFieldAccessorFactory.create(this.field);
        } catch (Throwable e) {
            throw new SparrowReflectionException("Failed to create ASM char accessor", e);
        }
    }

    public SLongField asm$long() {
        try {
            return LongFieldAccessorFactory.create(this.field);
        } catch (Throwable e) {
            throw new SparrowReflectionException("Failed to create ASM long accessor", e);
        }
    }
}
