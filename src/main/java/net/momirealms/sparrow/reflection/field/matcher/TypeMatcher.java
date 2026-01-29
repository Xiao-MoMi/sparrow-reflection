package net.momirealms.sparrow.reflection.field.matcher;

import java.lang.reflect.Field;

public class TypeMatcher implements FieldMatcher {
    private final Class<?> type;

    public TypeMatcher(Class<?> type) {
        this.type = type;
    }

    @Override
    public boolean matches(Field field) {
        return this.type.equals(field.getType());
    }
}
