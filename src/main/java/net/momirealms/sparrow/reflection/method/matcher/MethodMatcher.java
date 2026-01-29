package net.momirealms.sparrow.reflection.method.matcher;

import net.momirealms.sparrow.reflection.type.matcher.TypeMatcher;

import java.lang.reflect.Method;

public interface MethodMatcher {

    boolean matches(final Method method);

    default MethodMatcher or(final MethodMatcher matcher) {
        return new OrMatcher(this, matcher);
    }

    default MethodMatcher and(final MethodMatcher matcher) {
        return new AndMatcher(this, matcher);
    }

    static MethodMatcher any() {
        return AnyMatcher.INSTANCE;
    }

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
        return new NameMatcher(name, true);
    }

    static MethodMatcher namedNoRemap(String name) {
        return new NameMatcher(name, false);
    }

    static MethodMatcher named(String... names) {
        return new NamesMatcher(names, true);
    }

    static MethodMatcher namedNoRemap(String... names) {
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
