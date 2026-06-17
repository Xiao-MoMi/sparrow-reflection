package net.momirealms.sparrow.reflection.method.matcher;

import net.momirealms.sparrow.reflection.type.matcher.TypeMatcher;

import java.lang.reflect.Method;

public interface MethodMatcher {

    boolean matches(final Method method);

    default MethodMatcher or(final MethodMatcher matcher) {
        return method -> matches(method) || matcher.matches(method);
    }

    default MethodMatcher and(final MethodMatcher matcher) {
        return method -> matches(method) && matcher.matches(method);
    }

    static MethodMatcher any() {
        return method -> true;
    }

    static MethodMatcher anyOf(final MethodMatcher... matchers) {
        return method -> {
            for (final MethodMatcher matcher : matchers) {
                if (matcher.matches(method)) {
                    return true;
                }
            }
            return false;
        };
    }

    static MethodMatcher allOf(final MethodMatcher... matchers) {
        return method -> {
            for (final MethodMatcher matcher : matchers) {
                if (!matcher.matches(method)) {
                    return false;
                }
            }
            return true;
        };
    }

    static MethodMatcher not(final MethodMatcher matcher) {
        return method -> !matcher.matches(method);
    }

    static MethodMatcher noneOf(final MethodMatcher... matchers) {
        return not(anyOf(matchers));
    }

    static MethodMatcher named(String name) {
        return new NameMatcher(name, true);
    }

    static MethodMatcher namedNoRemap(String name) {
        return new NameMatcher(name, false);
    }

    static MethodMatcher named(String... names) {
        if (names.length == 1) {
            return named(names[0]);
        }
        return new NamesMatcher(names, true);
    }

    static MethodMatcher namedNoRemap(String... names) {
        if (names.length == 1) {
            return namedNoRemap(names[0]);
        }
        return new NamesMatcher(names, false);
    }

    static MethodMatcher returnType(final Class<?> type) {
        return new ReturnTypeMatcher(type);
    }

    static MethodMatcher returnType(final TypeMatcher matcher) {
        return new GenericReturnTypeMatcher(matcher);
    }

    static MethodMatcher takeArguments(final Class<?>... types) {
        return new TakeArgumentsMatcher(types);
    }

    static MethodMatcher takeArguments(final TypeMatcher... matchers) {
        return new TakeGenericArgumentsMatcher(matchers);
    }

    static MethodMatcher takeArgument(final int index, final Class<?> type) {
        return new TakeArgumentMatcher(index, type);
    }

    static MethodMatcher takeArgument(final int index, final TypeMatcher matcher) {
        return new TakeGenericArgumentMatcher(index, matcher);
    }

    static MethodMatcher privateMethod() {
        return PrivateMatcher.INSTANCE;
    }

    static MethodMatcher publicMethod() {
        return PublicMatcher.INSTANCE;
    }

    static MethodMatcher protectedMethod() {
        return ProtectedMatcher.INSTANCE;
    }

    static MethodMatcher staticMethod() {
        return StaticMatcher.INSTANCE;
    }

    static MethodMatcher instanceMethod() {
        return InstanceMatcher.INSTANCE;
    }

    static MethodMatcher finalMethod() {
        return FinalMatcher.INSTANCE;
    }
}
