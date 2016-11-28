package com.amadeus.session;

/**
 * This class is used to wrap checked exceptions.
 */
public class WrappedException extends RuntimeException {
  private static final long serialVersionUID = 6519454833663635265L;

  /**
   * Default constructor.
   *
   * @param cause
   *          checked exception to wrap
   */
  public WrappedException(Exception cause) {
    super(cause);
  }
}
