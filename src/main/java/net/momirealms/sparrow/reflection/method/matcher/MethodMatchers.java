package net.momirealms.sparrow.reflection.method.matcher;

import net.momirealms.sparrow.reflection.type.matcher.TypeMatcher;

public interface MethodMatchers {

    static MethodMatcher mAny() {
        return AnyMatcher.INSTANCE;
    }

    static MethodMatcher mAnyOf(final MethodMatcher... matchers) {
        return new AnyOfMatcher(matchers);
    }

    static MethodMatcher mAllOf(final MethodMatcher... matchers) {
        return new AllOfMatcher(matchers);
    }

    static MethodMatcher mNot(final MethodMatcher matcher) {
        return new NotMatcher(matcher);
    }

    static MethodMatcher mNoneOf(final MethodMatcher... matchers) {
        return mNot(mAnyOf(matchers));
    }

    static MethodMatcher mNamed(String name) {
        return new NameMatcher(name, true);
    }

    static MethodMatcher mNamedNoRemap(String name) {
        return new NameMatcher(name, false);
    }

    static MethodMatcher mNamed(String... names) {
        return new NamesMatcher(names, true);
    }

    static MethodMatcher mNamedNoRemap(String... names) {
        return new NamesMatcher(names, false);
    }

    static MethodMatcher mReturnType(final Class<?> type) {
        return new ReturnTypeMatcher(type);
    }

    static MethodMatcher mReturnType(final TypeMatcher matcher) {
        return new GenericReturnTypeMatcher(matcher);
    }

    static MethodMatcher mTakeArguments(final Class<?>... types) {
        return new TakeArgumentsMatcher(types);
    }

    static MethodMatcher mTakeArguments(final TypeMatcher... matchers) {
        return new TakeGenericArgumentsMatcher(matchers);
    }

    static MethodMatcher mTakeArgument(final int index, final Class<?> type) {
        return new TakeArgumentMatcher(index, type);
    }

    static MethodMatcher mTakeArgument(final int index, final TypeMatcher matcher) {
        return new TakeGenericArgumentMatcher(index, matcher);
    }

    static MethodMatcher mPrivate() {
        return PrivateMatcher.INSTANCE;
    }

    static MethodMatcher mPublic() {
        return PublicMatcher.INSTANCE;
    }

    static MethodMatcher mProtected() {
        return ProtectedMatcher.INSTANCE;
    }

    static MethodMatcher mStatic() {
        return StaticMatcher.INSTANCE;
    }

    static MethodMatcher mInstance() {
        return InstanceMatcher.INSTANCE;
    }

    static MethodMatcher mFinal() {
        return FinalMatcher.INSTANCE;
    }
}