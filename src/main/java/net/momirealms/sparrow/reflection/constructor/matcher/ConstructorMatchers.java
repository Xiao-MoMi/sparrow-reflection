package net.momirealms.sparrow.reflection.constructor.matcher;

import net.momirealms.sparrow.reflection.type.matcher.TypeMatcher;

public interface ConstructorMatchers {

    static ConstructorMatcher cAny() {
        return AnyMatcher.INSTANCE;
    }

    static ConstructorMatcher cAnyOf(final ConstructorMatcher... matchers) {
        return new AnyOfMatcher(matchers);
    }

    static ConstructorMatcher cAllOf(final ConstructorMatcher... matchers) {
        return new AllOfMatcher(matchers);
    }

    static ConstructorMatcher cNot(final ConstructorMatcher matcher) {
        return new NotMatcher(matcher);
    }

    static ConstructorMatcher cNoneOf(final ConstructorMatcher... matchers) {
        return cNot(cAnyOf(matchers));
    }

    static ConstructorMatcher cTakeArguments(final Class<?>... types) {
        return new TakeArgumentsMatcher(types);
    }

    static ConstructorMatcher cTakeArguments(final TypeMatcher... matchers) {
        return new TakeGenericArgumentsMatcher(matchers);
    }

    static ConstructorMatcher cTakeArgument(final int index, final Class<?> type) {
        return new TakeArgumentMatcher(index, type);
    }

    static ConstructorMatcher cTakeArgument(final int index, final TypeMatcher matcher) {
        return new TakeGenericArgumentMatcher(index, matcher);
    }

    static ConstructorMatcher cPrivate() {
        return PrivateMatcher.INSTANCE;
    }

    static ConstructorMatcher cPublic() {
        return PublicMatcher.INSTANCE;
    }

    static ConstructorMatcher cProtected() {
        return ProtectedMatcher.INSTANCE;
    }
}