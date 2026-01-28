package net.momirealms.sparrow.reflection.method.matcher;

import java.lang.reflect.Method;

public interface MethodMatcher {

    static MethodMatcher anyOf(final MethodMatcher... matchers) {
        return new AnyOfMatcher(matchers);
    }

    static MethodMatcher allOf(final MethodMatcher... matchers) {
        return new AllOfMatcher(matchers);
    }

    static MethodMatcher not(final MethodMatcher matcher) {
        return new NotMatcher(matcher);
    }

    static MethodMatcher noneOf(final MethodMatcher... matchers) {
        return not(anyOf(matchers));
    }

    static MethodMatcher named(String name) {
        return new NameMatcher(name);
    }

    static MethodMatcher named(String... names) {
        return new NamesMatcher(names);
    }

    static MethodMatcher returnType(final Class<?> type) {
        return new ReturnTypeMatcher(type);
    }

    static MethodMatcher takeArguments(final Class<?>... types) {
        return new TakeArgumentsMatcher(types);
    }

    static MethodMatcher takeArgument(final int index, final Class<?> type) {
        return new TakeArgumentMatcher(index, type);
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

    boolean matches(final Method method);

    default MethodMatcher or(final MethodMatcher matcher) {
        return new OrMatcher(this, matcher);
    }

    default MethodMatcher and(final MethodMatcher matcher) {
        return new AndMatcher(this, matcher);
    }
}
