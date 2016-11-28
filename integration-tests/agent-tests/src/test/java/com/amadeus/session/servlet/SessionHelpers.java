package com.amadeus.session.servlet;

import static java.lang.invoke.MethodType.methodType;
import static org.mockito.Mockito.mock;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.EventListener;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpSessionEvent;

/**
 * This class hides SessionHelpers from implementation and is used for testing purposes only.
 * It mocks the methods of the SessionHelpers class and simply counts number of requests as
 * well as mocks request and response objects.
 */
@SuppressWarnings("javadoc")
public class SessionHelpers {
  public final static AtomicInteger initSessionManagementCalled = new AtomicInteger();
  public final static AtomicInteger prepareRequestCalled = new AtomicInteger();
  public final static AtomicInteger prepareResponseCalled = new AtomicInteger();
  public final static AtomicInteger interceptListener = new AtomicInteger();
  public static ServletRequest mockedRequest;
  public static ServletResponse mockedResponse;

  public static void resetForTests() {
    initSessionManagementCalled.set(0);
    prepareRequestCalled.set(0);
    prepareResponseCalled.set(0);
    interceptListener.set(0);
    mockedRequest = null;
    mockedResponse = null;
  }

  public ServletRequest prepareRequest(ServletRequest request, ServletResponse response,
      ServletContext servletContext) {
    prepareRequestCalled.incrementAndGet();

    if (mockedRequest == null) {
      mockedRequest = mock(ServletRequest.class);
    }

    return mockedRequest;
  }

  public ServletResponse prepareResponse(ServletRequest request, ServletResponse response) {
    if (mockedResponse == null) {
      mockedResponse = mock(ServletResponse.class);
    }

    return mockedResponse;
  }

  public MethodHandle[] initSessionManagement(ServletContext servletContext) {
    initSessionManagementCalled.incrementAndGet();
    return prepareMethodCalls(servletContext);
  }


  /**
   * This method introspects this class and records {@link MethodHandle} of
   * public methods. This allows direc invocation of said methods from
   * instrumented classes.
   *
   * @param servletContext
   * @return
   */
  private MethodHandle[] prepareMethodCalls(ServletContext servletContext) {
    try {
      MethodHandles.Lookup lookup = MethodHandles.lookup();
      MethodType mt = methodType(void.class, ServletContext.class, Object.class);
      MethodHandle onAddListner = lookup.bind(this, "onAddListener", mt);
      mt = methodType(void.class, EventListener.class, HttpSessionEvent.class);
      MethodHandle interceptHttpListener = lookup.bind(this, "interceptHttpListener", mt);
      mt = methodType(MethodHandle[].class, ServletContext.class);
      MethodHandle initSessionManagement = lookup.bind(this, "initSessionManagement", mt);
      mt = methodType(ServletResponse.class, ServletRequest.class, ServletResponse.class);
      MethodHandle prepareResponse = lookup.bind(this, "prepareResponse", mt);
      mt = methodType(ServletRequest.class, ServletRequest.class, ServletResponse.class, ServletContext.class);
      MethodHandle prepareRequest = lookup.bind(this, "prepareRequest", mt);
      mt = methodType(void.class, ServletRequest.class, ServletRequest.class);
      MethodHandle commitRequest = lookup.bind(this, "commitRequest", mt);
      MethodHandle[] methods = new MethodHandle[] { onAddListner, interceptHttpListener, initSessionManagement,
          prepareResponse, prepareRequest, commitRequest };
      return methods;
    } catch (NoSuchMethodException e) {
      throw new IllegalStateException("Unable to find method for invoking", e);
    } catch (IllegalAccessException e) {
      throw new IllegalStateException("Unable to introspect class " + SessionHelpers.class, e);
    }
  }

  public void commitRequest(ServletRequest request, ServletRequest oldRequest) {

  }

  public void interceptHttpListener(EventListener caller, HttpSessionEvent event) {
    interceptListener.incrementAndGet();
  }

  public void onAddListener(ServletContext servletContext, Object listener) {

  }
}
