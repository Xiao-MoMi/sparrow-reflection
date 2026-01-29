package net.momirealms.sparrow.reflection.field.matcher;

public interface FieldMatchers {

    static FieldMatcher fAny() {
        return AnyMatcher.INSTANCE;
    }

    static FieldMatcher fAnyOf(final FieldMatcher... matchers) {
        return new AnyOfMatcher(matchers);
    }

    static FieldMatcher fAllOf(final FieldMatcher... matchers) {
        return new AllOfMatcher(matchers);
    }

    static FieldMatcher fNot(final FieldMatcher matcher) {
        return new NotMatcher(matcher);
    }

    static FieldMatcher fNoneOf(final FieldMatcher... matchers) {
        return fNot(fAnyOf(matchers));
    }

    static FieldMatcher fNamed(String name) {
        return new NameMatcher(name, true);
    }

    static FieldMatcher fNamedNoRemap(String name) {
        return new NameMatcher(name, false);
    }

    static FieldMatcher fNamed(String... names) {
        return new NamesMatcher(names, true);
    }

    static FieldMatcher fNamedNoRemap(String... names) {
        return new NamesMatcher(names, false);
    }

    static FieldMatcher fType(Class<?> clazz) {
        return new TypeMatcher(clazz);
    }

    static FieldMatcher fType(net.momirealms.sparrow.reflection.type.matcher.TypeMatcher typeMatcher) {
        return new GenericTypeMatcher(typeMatcher);
    }

    static FieldMatcher fPrivate() {
        return PrivateMatcher.INSTANCE;
    }

    static FieldMatcher fPublic() {
        return PublicMatcher.INSTANCE;
    }

    static FieldMatcher fProtected() {
        return ProtectedMatcher.INSTANCE;
    }

    static FieldMatcher fStatic() {
        return StaticMatcher.INSTANCE;
    }

    static FieldMatcher fInstance() {
        return InstanceMatcher.INSTANCE;
    }

    static FieldMatcher fFinal() {
        return FinalMatcher.INSTANCE;
    }
}