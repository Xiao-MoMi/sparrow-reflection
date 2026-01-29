package net.momirealms.sparrow.reflection.type.matcher;

public interface TypeMatchers {

    static TypeMatcher tAny() {
        return AnyMatcher.INSTANCE;
    }

    static TypeMatcher tAnyOf(final TypeMatcher... matchers) {
        return new AnyOfMatcher(matchers);
    }

    static TypeMatcher tAllOf(final TypeMatcher... matchers) {
        return new AllOfMatcher(matchers);
    }

    static TypeMatcher tNot(final TypeMatcher matcher) {
        return new NotMatcher(matcher);
    }

    static TypeMatcher tNoneOf(final TypeMatcher... matchers) {
        return tNot(tAnyOf(matchers));
    }

    static TypeMatcher tParameterized(final TypeMatcher raw, TypeMatcher... parameters) {
        return new ParameterizedMatcher(raw, null, parameters);
    }

    static TypeMatcher tParameterized(final TypeMatcher raw, TypeMatcher owner, TypeMatcher... parameters) {
        return new ParameterizedMatcher(raw, owner, parameters);
    }

    static TypeMatcher tGenericArray(final TypeMatcher component) {
        return new GenericArrayMatcher(component);
    }

    static TypeMatcher tWildcard() {
        return WildCardMatcher.SIMPLE;
    }

    static TypeMatcher tWildcard(final TypeMatcher[] upper, final TypeMatcher[] lower) {
        return new WildCardMatcher(upper, lower);
    }

    static TypeMatcher tWildcardUpper(final TypeMatcher... wildcard) {
        return new WildCardMatcher(wildcard, null);
    }

    static TypeMatcher tWildcardLower(final TypeMatcher... wildcard) {
        return new WildCardMatcher(null, wildcard);
    }

    static TypeMatcher tTypeVariable() {
        return TypeVariableMatcher.SIMPLE;
    }

    static TypeMatcher tTypeVariable(final String name) {
        return new TypeVariableMatcher(name, null);
    }

    static TypeMatcher tTypeVariable(final String name, final TypeMatcher... bounds) {
        return new TypeVariableMatcher(name, bounds);
    }

    static TypeMatcher tClazz(final Class<?> clazz) {
        return new ClassMatcher(clazz);
    }
}
