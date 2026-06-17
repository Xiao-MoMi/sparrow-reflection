package net.momirealms.sparrow.reflection.constructor.matcher;

import net.momirealms.sparrow.reflection.type.matcher.TypeMatcher;

import java.lang.reflect.Constructor;

public interface ConstructorMatcher {

    boolean matches(final Constructor<?> constructor);

    default ConstructorMatcher or(final ConstructorMatcher matcher) {
        return constructor -> this.matches(constructor) || matcher.matches(constructor);
    }

    default ConstructorMatcher and(final ConstructorMatcher matcher) {
        return constructor -> this.matches(constructor) && matcher.matches(constructor);
    }

    static ConstructorMatcher any() {
        return constructor -> true;
    }

    static ConstructorMatcher anyOf(final ConstructorMatcher... matchers) {
        return constructor -> {
            for (final ConstructorMatcher matcher : matchers) {
                if (matcher.matches(constructor)) {
                    return true;
                }
            }
            return false;
        };
    }

    static ConstructorMatcher allOf(final ConstructorMatcher... matchers) {
        return constructor -> {
            for (final ConstructorMatcher matcher : matchers) {
                if (!matcher.matches(constructor)) {
                    return false;
                }
            }
            return true;
        };
    }

    static ConstructorMatcher not(final ConstructorMatcher matcher) {
        return constructor -> !matcher.matches(constructor);
    }

    static ConstructorMatcher noneOf(final ConstructorMatcher... matchers) {
        return not(anyOf(matchers));
    }

    static ConstructorMatcher takeArguments(final Class<?>... types) {
        return new TakeArgumentsMatcher(types);
    }

    static ConstructorMatcher takeArguments(final TypeMatcher... matchers) {
        return new TakeGenericArgumentsMatcher(matchers);
    }

    static ConstructorMatcher takeArgument(final int index, final Class<?> type) {
        return new TakeArgumentMatcher(index, type);
    }

    static ConstructorMatcher takeArgument(final int index, final TypeMatcher matcher) {
        return new TakeGenericArgumentMatcher(index, matcher);
    }

    static ConstructorMatcher privateConstructor() {
        return PrivateMatcher.INSTANCE;
    }

    static ConstructorMatcher publicConstructor() {
        return PublicMatcher.INSTANCE;
    }

    static ConstructorMatcher protectedConstructor() {
        return ProtectedMatcher.INSTANCE;
    }
}
