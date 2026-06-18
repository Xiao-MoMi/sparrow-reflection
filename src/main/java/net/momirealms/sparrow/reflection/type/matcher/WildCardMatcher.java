package net.momirealms.sparrow.reflection.type.matcher;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;

final class WildCardMatcher implements TypeMatcher {
    public static final WildCardMatcher SIMPLE = new WildCardMatcher(null, null);
    private final TypeMatcher[] upper;
    private final TypeMatcher[] lower;

    WildCardMatcher(@Nullable TypeMatcher[] upper, @Nullable TypeMatcher[] lower) {
        this.upper = upper;
        this.lower = lower;
    }

    // 通配符          上界      下界
    // ?          ->  Object   Null
    // ? extend T ->  T        Null
    // ? super  T ->  Object,   T
    @Override
    public boolean matches(Type type) {
        if (!(type instanceof WildcardType wildcardType)) return false;

        if (this.upper != null && this.upper.length != 0) {
            Type[] upperBounds = wildcardType.getUpperBounds();
            if (upperBounds.length != this.upper.length) return false;
            for (int i = 0; i < upperBounds.length; i++) {
                if (!this.upper[i].matches(upperBounds[i])) return false;
            }
        }

        if (this.lower != null && this.lower.length != 0) {
            Type[] lowerBounds = wildcardType.getLowerBounds();
            if (lowerBounds.length != this.lower.length) return false;
            for (int i = 0; i < lowerBounds.length; i++) {
                if (!this.lower[i].matches(lowerBounds[i])) return false;
            }
        }
        return true;
    }
}
