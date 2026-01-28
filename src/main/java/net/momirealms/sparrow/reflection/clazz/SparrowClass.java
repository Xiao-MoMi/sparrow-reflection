package net.momirealms.sparrow.reflection.clazz;

import net.momirealms.sparrow.reflection.SReflection;
import net.momirealms.sparrow.reflection.constructor.SparrowConstructor;
import net.momirealms.sparrow.reflection.constructor.matcher.ConstructorMatcher;
import net.momirealms.sparrow.reflection.field.SparrowField;
import net.momirealms.sparrow.reflection.field.matcher.FieldMatcher;
import net.momirealms.sparrow.reflection.method.SparrowMethod;
import net.momirealms.sparrow.reflection.method.matcher.MethodMatcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;

@SuppressWarnings("DuplicatedCode")
public final class SparrowClass<T> {
    public final Class<T> clazz;

    public SparrowClass(Class<T> clazz) {
        this.clazz = clazz;
    }

    public static <T> SparrowClass<T> of(@NotNull Class<T> clazz) {
        Objects.requireNonNull(clazz, "class cannot be null");
        return new SparrowClass<>(clazz);
    }

    @Nullable
    public static <T> SparrowClass<T> ofNullable(@Nullable Class<T> clazz) {
        return clazz == null ? null : new SparrowClass<>(clazz);
    }

    public Class<T> clazz() {
        return this.clazz;
    }

    public boolean isInstance(@NotNull Object object) {
        return this.clazz.isInstance(object);
    }

    /*

    fields

     */

    @Nullable
    public Field getField(FieldMatcher matcher) {
        for (Field field : this.clazz.getFields()) {
            if (matcher.matches(field)) {
                return field;
            }
        }
        return null;
    }

    @Nullable
    public Field getDeclaredField(FieldMatcher matcher) {
        for (Field field : this.clazz.getDeclaredFields()) {
            if (matcher.matches(field)) {
                return SReflection.setAccessible(field);
            }
        }
        return null;
    }

    @Nullable
    public Field getField(FieldMatcher matcher, int index) {
        Field[] fields = this.clazz.getFields();
        int i = 0;
        for (Field field : fields) {
            if (matcher.matches(field)) {
                if (i == index) {
                    return field;
                }
                i++;
            }
        }
        return null;
    }

    @Nullable
    public Field getDeclaredField(FieldMatcher matcher, int index) {
        Field[] fields = this.clazz.getDeclaredFields();
        int i = 0;
        for (Field field : fields) {
            if (matcher.matches(field)) {
                if (i == index) {
                    return SReflection.setAccessible(field);
                }
                i++;
            }
        }
        return null;
    }

    @Nullable
    public Field getFieldBackwards(FieldMatcher matcher, int index) {
        Field[] fields = this.clazz.getFields();
        int i = 0;
        for (int j = fields.length - 1; j >= 0; j--) {
            if (matcher.matches(fields[j])) {
                if (i == index) {
                    return fields[j];
                }
                i++;
            }
        }
        return null;
    }

    @Nullable
    public Field getDeclaredFieldBackwards(FieldMatcher matcher, int index) {
        Field[] fields = this.clazz.getDeclaredFields();
        int i = 0;
        for (int j = fields.length - 1; j >= 0; j--) {
            if (matcher.matches(fields[j])) {
                if (i == index) {
                    return SReflection.setAccessible(fields[j]);
                }
                i++;
            }
        }
        return null;
    }

    @Nullable
    public SparrowField getSparrowField(FieldMatcher matcher) {
        return SparrowField.ofNullable(getField(matcher));
    }

    @Nullable
    public SparrowField getDeclaredSparrowField(FieldMatcher matcher) {
        return SparrowField.ofNullable(getDeclaredField(matcher));
    }

    @Nullable
    public SparrowField getSparrowField(FieldMatcher matcher, int index) {
        return SparrowField.ofNullable(getField(matcher, index));
    }

    @Nullable
    public SparrowField getDeclaredSparrowField(FieldMatcher matcher, int index) {
        return SparrowField.ofNullable(getDeclaredField(matcher, index));
    }

    @Nullable
    public SparrowField getSparrowFieldBackwards(FieldMatcher matcher, int index) {
        return SparrowField.ofNullable(getFieldBackwards(matcher, index));
    }

    @Nullable
    public SparrowField getDeclaredSparrowFieldBackwards(FieldMatcher matcher, int index) {
        return SparrowField.ofNullable(getDeclaredFieldBackwards(matcher, index));
    }

    /*

    constructors

     */

    @Nullable
    @SuppressWarnings("unchecked")
    public Constructor<T> getConstructor(ConstructorMatcher matcher) {
        for (Constructor<?> constructor : this.clazz.getConstructors()) {
            if (matcher.matches(constructor)) {
                return (Constructor<T>) constructor;
            }
        }
        return null;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public Constructor<T> getDeclaredConstructor(ConstructorMatcher matcher) {
        for (Constructor<?> constructor : this.clazz.getDeclaredConstructors()) {
            if (matcher.matches(constructor)) {
                return (Constructor<T>) SReflection.setAccessible(constructor);
            }
        }
        return null;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public Constructor<T> getConstructor(ConstructorMatcher matcher, int index) {
        Constructor<?>[] constructors = this.clazz.getConstructors();
        int i = 0;
        for (Constructor<?> constructor : constructors) {
            if (matcher.matches(constructor)) {
                if (i == index) {
                    return (Constructor<T>) constructor;
                }
                i++;
            }
        }
        return null;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public Constructor<T> getDeclaredConstructor(ConstructorMatcher matcher, int index) {
        Constructor<?>[] constructors = this.clazz.getDeclaredConstructors();
        int i = 0;
        for (Constructor<?> constructor : constructors) {
            if (matcher.matches(constructor)) {
                if (i == index) {
                    return (Constructor<T>) SReflection.setAccessible(constructor);
                }
                i++;
            }
        }
        return null;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public Constructor<T> getConstructorBackwards(ConstructorMatcher matcher, int index) {
        Constructor<?>[] constructors = this.clazz.getConstructors();
        int i = 0;
        for (int j = constructors.length - 1; j >= 0; j--) {
            if (matcher.matches(constructors[j])) {
                if (i == index) {
                    return (Constructor<T>) constructors[j];
                }
                i++;
            }
        }
        return null;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public Constructor<T> getDeclaredConstructorBackwards(ConstructorMatcher matcher, int index) {
        Constructor<?>[] constructors = this.clazz.getDeclaredConstructors();
        int i = 0;
        for (int j = constructors.length - 1; j >= 0; j--) {
            if (matcher.matches(constructors[j])) {
                if (i == index) {
                    return (Constructor<T>) SReflection.setAccessible(constructors[j]);
                }
                i++;
            }
        }
        return null;
    }

    @Nullable
    public SparrowConstructor<T> getSparrowConstructor(ConstructorMatcher matcher) {
        return SparrowConstructor.ofNullable(getConstructor(matcher));
    }

    @Nullable
    public SparrowConstructor<T> getDeclaredSparrowConstructor(ConstructorMatcher matcher) {
        return SparrowConstructor.ofNullable(getDeclaredConstructor(matcher));
    }

    @Nullable
    public SparrowConstructor<T> getSparrowConstructor(ConstructorMatcher matcher, int index) {
        return SparrowConstructor.ofNullable(getConstructor(matcher, index));
    }

    @Nullable
    public SparrowConstructor<T> getDeclaredSparrowConstructor(ConstructorMatcher matcher, int index) {
        return SparrowConstructor.ofNullable(getDeclaredConstructor(matcher, index));
    }

    @Nullable
    public SparrowConstructor<T> getSparrowConstructorBackwards(ConstructorMatcher matcher, int index) {
        return SparrowConstructor.ofNullable(getConstructorBackwards(matcher, index));
    }

    @Nullable
    public SparrowConstructor<T> getDeclaredSparrowConstructorBackwards(ConstructorMatcher matcher, int index) {
        return SparrowConstructor.ofNullable(getDeclaredConstructorBackwards(matcher, index));
    }

    /*

    methods

     */

    @Nullable
    public Method getMethod(MethodMatcher matcher) {
        Method[] methods = this.clazz.getMethods();
        for (Method method : methods) {
            if (matcher.matches(method)) {
                return method;
            }
        }
        return null;
    }

    @Nullable
    public Method getDeclaredMethod(MethodMatcher matcher) {
        Method[] methods = this.clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (matcher.matches(method)) {
                return SReflection.setAccessible(method);
            }
        }
        return null;
    }

    @Nullable
    public Method getMethod(MethodMatcher matcher, int index) {
        Method[] methods = this.clazz.getMethods();
        int i = 0;
        for (Method method : methods) {
            if (matcher.matches(method)) {
                if (i == index) {
                    return method;
                }
                i++;
            }
        }
        return null;
    }

    @Nullable
    public Method getDeclaredMethod(MethodMatcher matcher, int index) {
        Method[] methods = this.clazz.getDeclaredMethods();
        int i = 0;
        for (Method method : methods) {
            if (matcher.matches(method)) {
                if (i == index) {
                    return SReflection.setAccessible(method);
                }
                i++;
            }
        }
        return null;
    }

    @Nullable
    public Method getMethodBackwards(MethodMatcher matcher, int index) {
        Method[] methods = this.clazz.getMethods();
        int i = 0;
        for (int j = methods.length - 1; j >= 0; j--) {
            if (matcher.matches(methods[j])) {
                if (i == index) {
                    return methods[j];
                }
                i++;
            }
        }
        return null;
    }

    @Nullable
    public Method getDeclaredMethodBackwards(MethodMatcher matcher, int index) {
        Method[] methods = this.clazz.getDeclaredMethods();
        int i = 0;
        for (int j = methods.length - 1; j >= 0; j--) {
            if (matcher.matches(methods[j])) {
                if (i == index) {
                    return SReflection.setAccessible(methods[j]);
                }
                i++;
            }
        }
        return null;
    }

    @Nullable
    public SparrowMethod getDeclaredSparrowMethod(MethodMatcher matcher) {
        return SparrowMethod.ofNullable(getDeclaredMethod(matcher));
    }

    @Nullable
    public SparrowMethod getSparrowMethod(MethodMatcher matcher) {
        return SparrowMethod.ofNullable(getMethod(matcher));
    }

    @Nullable
    public SparrowMethod getDeclaredSparrowMethod(MethodMatcher matcher, int index) {
        return SparrowMethod.ofNullable(getDeclaredMethod(matcher, index));
    }

    @Nullable
    public SparrowMethod getSparrowMethod(MethodMatcher matcher, int index) {
        return SparrowMethod.ofNullable(getMethod(matcher, index));
    }

    @Nullable
    public SparrowMethod getSparrowMethodBackwards(MethodMatcher matcher, int index) {
        return SparrowMethod.ofNullable(getMethodBackwards(matcher, index));
    }

    @Nullable
    public SparrowMethod getDeclaredSparrowMethodBackwards(MethodMatcher matcher, int index) {
        return SparrowMethod.ofNullable(getDeclaredMethodBackwards(matcher, index));
    }
}
