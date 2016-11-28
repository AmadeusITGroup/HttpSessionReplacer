package com.amadeus.session;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

/**
 * Matcher of Regex patterns against a String, similar to
 * {@link String#matches(String)}.
 *
 * @author copied from https://github.com/jcabi/jcabi-matchers
 */
final class RegexContainingPatternMatcher extends TypeSafeMatcher<String> {

  /**
   * The Regex pattern.
   */
  private final transient String pattern;

  /**
   * Public ctor.
   * @param regex The regular expression to match against.
   */
  public RegexContainingPatternMatcher(final String regex) {
      super();
      this.pattern = regex;
  }

  @Override
  public void describeTo(final Description description) {
      description.appendText("a String matching the regular expression ")
          .appendText(this.pattern);
  }

  @Override
  public boolean matchesSafely(final String item) {
      return item.contains(this.pattern);
  }

}