package com.amadeus.session;

import java.util.ArrayList;
import java.util.Collection;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;

/**
 * Convenient matchers for checking Strings against regular expressions.
 *
 * @author copied from https://github.com/jcabi/jcabi-matchers
 */
public final class RegexMatchers {

    /**
     * Private ctor, it's a utility class.
     */
    private RegexMatchers() {
        // Utility class, shouldn't be instantiated.
    }

    /**
     * Checks whether a String matches at lease one of given regular
     * expressions.
     * @param patterns Regular expression patterns
     * @return Matcher suitable for JUnit/Hamcrest matching
     */
    public static Matcher<String> matchesAnyPattern(final String...patterns) {
        final Collection<Matcher<? super String>> matchers =
            new ArrayList<Matcher<? super String>>(patterns.length);
        for (final String pattern : patterns) {
            matchers.add(new RegexMatchingPatternMatcher(pattern));
        }
        return CoreMatchers.anyOf(matchers);
    }

    /**
     * Checks whether a String matches the given regular expression. Works in a
     * similar manner to {@link String#matches(String)}. For example:
     *
     * <pre> MatcherAssert.assert(
     *   "abc123",
     *   RegexMatchers.matchesPattern("[a-c]+\\d{3}")
     * );</pre>
     *
     * @param pattern The pattern to match against
     * @return Matcher suitable for JUnit/Hamcrest matching
     */
    public static Matcher<String> matchesPattern(final String pattern) {
        return new RegexMatchingPatternMatcher(pattern);
    }

    /**
     * Checks whether a String contains a subsequence matching the given regular
     * expression. Works in a similar manner to
     * {@link java.util.regex.Matcher#find()}. For example:
     *
     * <pre> MatcherAssert.assert(
     *   "fooBar123",
     *   RegexMatchers.containsPattern("Bar12")
     * );</pre>
     *
     * @param pattern The pattern to match against
     * @return Matcher suitable for JUnit/Hamcrest matching
     */
    public static Matcher<String> containsPattern(final String pattern) {
        return new RegexContainingPatternMatcher(pattern);
    }

    /**
     * Checks whether a {@link String} contains a subsequence matching any of
     * the given regular expressions.
     * @param patterns The patterns to match against
     * @return Matcher suitable for JUnit/Hamcrest matching
     * @see java.util.regex.Matcher#find()
     * @see #containsPattern(String)
     */
    public static Matcher<String> containsAnyPattern(final String... patterns) {
        return CoreMatchers
            .anyOf(createContainingMatchers(patterns));
    }

    /**
     * Checks whether a {@link String} contains a subsequence matching any of
     * the given regular expressions.
     * @param patterns The patterns to match against
     * @return Matcher suitable for JUnit/Hamcrest matching
     * @see java.util.regex.Matcher#find()
     * @see #containsPattern(String)
     */
    public static Matcher<String> containsAllPatterns(
        final String... patterns) {
        return CoreMatchers
            .allOf(createContainingMatchers(patterns));
    }

    /**
     * Creates a {@link Collection} of {@link Matcher}'s for the given patterns.
     * @param patterns The given patterns
     * @return A {@link Collection} of {@link Matcher}'s
     */
    private static Collection<Matcher<? super String>> createContainingMatchers(
        final String... patterns) {
        final Collection<Matcher<? super String>> matchers =
            new ArrayList<Matcher<? super String>>(patterns.length);
        for (final String pattern : patterns) {
            matchers.add(new RegexContainingPatternMatcher(pattern));
        }
        return matchers;
    }

}