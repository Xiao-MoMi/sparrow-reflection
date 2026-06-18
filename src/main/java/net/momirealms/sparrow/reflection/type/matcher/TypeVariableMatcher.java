package net.momirealms.sparrow.reflection.type.matcher;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

final class TypeVariableMatcher implements TypeMatcher {
    public static final TypeVariableMatcher SIMPLE = new TypeVariableMatcher(null, null);
    private final String name;
    private final TypeMatcher[] bounds;

    TypeVariableMatcher(@Nullable String name, @Nullable TypeMatcher[] bounds) {
        this.name = name;
        this.bounds = bounds;
    }

    @Override
    public boolean matches(Type type) {
        if (!(type instanceof TypeVariable<?> typeVariable)) return false;
        if (this.name != null) {
            if (!typeVariable.getName().equals(this.name)) return false;
        }
        if (this.bounds != null) {
            Type[] bounds = typeVariable.getBounds();
            if (this.bounds.length != bounds.length) return false;
            for (int i = 0; i < this.bounds.length; i++) {
                if (!this.bounds[i].matches(bounds[i])) return false;
            }
        }
        return true;
    }
}
