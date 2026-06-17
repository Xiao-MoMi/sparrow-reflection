package net.momirealms.sparrow.reflection.field.matcher;

import java.lang.reflect.Field;

public interface FieldMatcher {

    boolean matches(final Field field);

    default FieldMatcher or(final FieldMatcher matcher) {
        return field -> matches(field) || matcher.matches(field);
    }

    default FieldMatcher and(final FieldMatcher matcher) {
        return field -> matches(field) && matcher.matches(field);
    }

    static FieldMatcher any() {
        return field -> true;
    }

    static FieldMatcher anyOf(final FieldMatcher... matchers) {
        return field -> {
            for (final FieldMatcher matcher : matchers) {
                if (matcher.matches(field)) {
                    return true;
                }
            }
            return false;
        };
    }

    static FieldMatcher allOf(final FieldMatcher... matchers) {
        return field -> {
            for (final FieldMatcher matcher : matchers) {
                if (!matcher.matches(field)) {
                    return false;
                }
            }
            return true;
        };
    }

    static FieldMatcher not(final FieldMatcher matcher) {
        return field -> !matcher.matches(field);
    }

    static FieldMatcher noneOf(final FieldMatcher... matchers) {
        return not(anyOf(matchers));
    }

    static FieldMatcher named(String name) {
        return new NameMatcher(name, true);
    }

    static FieldMatcher namedNoRemap(String name) {
        return new NameMatcher(name, false);
    }

    static FieldMatcher named(String... names) {
        if (names.length == 1) {
            return named(names[0]);
        }
        return new NamesMatcher(names, true);
    }

    static FieldMatcher namedNoRemap(String... names) {
        if (names.length == 1) {
            return namedNoRemap(names[0]);
        }
        return new NamesMatcher(names, false);
    }

    static FieldMatcher type(Class<?> clazz) {
        return new TypeMatcher(clazz);
    }

    static FieldMatcher type(net.momirealms.sparrow.reflection.type.matcher.TypeMatcher typeMatcher) {
        return new GenericTypeMatcher(typeMatcher);
    }

    static FieldMatcher privateField() {
        return PrivateMatcher.INSTANCE;
    }

    static FieldMatcher publicField() {
        return PublicMatcher.INSTANCE;
    }

    static FieldMatcher protectedField() {
        return ProtectedMatcher.INSTANCE;
    }

    static FieldMatcher staticField() {
        return StaticMatcher.INSTANCE;
    }

    static FieldMatcher instanceField() {
        return InstanceMatcher.INSTANCE;
    }

    static FieldMatcher finalField() {
        return FinalMatcher.INSTANCE;
    }
}
