package net.momirealms.sparrow.reflection.type.matcher;

import java.lang.reflect.Type;

public interface TypeMatcher {

    boolean matches(final Type type);

    default TypeMatcher or(final TypeMatcher matcher) {
        return new OrMatcher(this, matcher);
    }

    default TypeMatcher and(final TypeMatcher matcher) {
        return new AndMatcher(this, matcher);
    }

    static TypeMatcher any() {
        return AnyMatcher.INSTANCE;
    }

    static TypeMatcher anyOf(final TypeMatcher... matchers) {
        return new AnyOfMatcher(matchers);
    }

    static TypeMatcher allOf(final TypeMatcher... matchers) {
        return new AllOfMatcher(matchers);
    }

    static TypeMatcher not(final TypeMatcher matcher) {
        return new NotMatcher(matcher);
    }

    static TypeMatcher noneOf(final TypeMatcher... matchers) {
        return not(anyOf(matchers));
    }

    static TypeMatcher parameterized(final TypeMatcher raw, TypeMatcher... parameters) {
        return new ParameterizedMatcher(raw, null, parameters);
    }

    static TypeMatcher parameterized(final TypeMatcher raw, TypeMatcher owner, TypeMatcher... parameters) {
        return new ParameterizedMatcher(raw, owner, parameters);
    }

    static TypeMatcher genericArray(final TypeMatcher component) {
        return new GenericArrayMatcher(component);
    }

    static TypeMatcher wildcard() {
        return WildCardMatcher.SIMPLE;
    }

    static TypeMatcher wildcard(final TypeMatcher[] upper, final TypeMatcher[] lower) {
        return new WildCardMatcher(upper, lower);
    }

    static TypeMatcher wildcardUpper(final TypeMatcher... wildcard) {
        return new WildCardMatcher(wildcard, null);
    }

    static TypeMatcher wildcardLower(final TypeMatcher... wildcard) {
        return new WildCardMatcher(null, wildcard);
    }

    static TypeMatcher typeVariable() {
        return TypeVariableMatcher.SIMPLE;
    }

    static TypeMatcher typeVariable(final String name) {
        return new TypeVariableMatcher(name, null);
    }

    static TypeMatcher typeVariable(final String name, final TypeMatcher... bounds) {
        return new TypeVariableMatcher(name, bounds);
    }

    static TypeMatcher clazz(final Class<?> clazz) {
        return new ClassMatcher(clazz);
    }
}
