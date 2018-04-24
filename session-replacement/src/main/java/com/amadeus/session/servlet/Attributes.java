package com.amadeus.session.servlet;

import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;

import com.amadeus.session.SessionManager;
import com.amadeus.session.SessionRepositoryFactory;

/**
 * This class contains attributes that are stored in {@link ServletContext} or
 * {@link ServletRequest} attributes. Those attributes are used internally.
 */
final class Attributes {
  /**
   * {@link ServletContext} attribute containing the {@link SessionManager}. The
   * value is used internally.
   */
  static final String SESSION_MANAGER = SessionManager.class.getName();

  /**
   * {@link ServletRequest} attribute containing the <code>true</code> if the
   * session has been propagated to client. The value is used internally and
   * stored in {@link ServletRequest}.
   */
  static final String SESSION_PROPAGATED = "com.amadeus.session.isPropagated";

  /**
   * {@link ServletContext} attribute containing the
   * {@link ServletContextDescriptor} for the context. The value is used
   * internally and stored in {@link ServletContext}.
   */
  static final String SERVLET_CONTEXT_DESCRIPTOR = ServletContextDescriptor.class.getName();

  /**
   * {@link ServletContext} attribute containing the {@link Map} with mapping
   * between provider name and the name of {@link SessionRepositoryFactory}
   * implementation. The value is used internally and stored in
   * {@link ServletContext}.
   */
  static final String PROVIDERS = "com.amadeus.session.providers";

  static final String ResetManager = "com.amadeus.session.ResetManager";
  
  
  // Hide constructor
  private Attributes() {
  }
}