package net.momirealms.sparrow.reflection.constructor.matcher;

import java.lang.reflect.Constructor;

public interface ConstructorMatcher {

    static ConstructorMatcher anyOf(final ConstructorMatcher... matchers) {
        return new AnyOfMatcher(matchers);
    }

    static ConstructorMatcher allOf(final ConstructorMatcher... matchers) {
        return new AllOfMatcher(matchers);
    }

    static ConstructorMatcher not(final ConstructorMatcher matcher) {
        return new NotMatcher(matcher);
    }

    static ConstructorMatcher noneOf(final ConstructorMatcher... matchers) {
        return not(anyOf(matchers));
    }

    static ConstructorMatcher takeArguments(final Class<?>... types) {
        return new TakeArgumentsMatcher(types);
    }

    static ConstructorMatcher takeArgument(final int index, final Class<?> type) {
        return new TakeArgumentMatcher(index, type);
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

    boolean matches(final Constructor<?> constructor);

    default ConstructorMatcher or(final ConstructorMatcher matcher) {
        return new OrMatcher(this, matcher);
    }

    default ConstructorMatcher and(final ConstructorMatcher matcher) {
        return new AndMatcher(this, matcher);
    }
}
