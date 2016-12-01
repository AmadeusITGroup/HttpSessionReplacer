package com.amadeus.session.repository.redis;

public class DummyException extends Exception {
  private static final long serialVersionUID = 2861728518655741253L;

  public DummyException(String message) {
    super(message);
  }

  public DummyException(Throwable cause) {
    super(cause);
  }

  public DummyException(String message, Throwable cause) {
    super(message, cause);
  }
}
