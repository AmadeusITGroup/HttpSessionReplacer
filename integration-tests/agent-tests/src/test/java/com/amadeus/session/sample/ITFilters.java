package com.amadeus.session.sample;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;

import org.junit.Test;

import com.amadeus.session.servlet.SessionHelpers;

public class ITFilters {
  @Test
  public void testAnnotatedFilter() throws Exception {
    assertFullInstrumentedFilter(AnnotatedFilter.class);

    invokeInit(AnnotatedFilter.class, 1);
  }

  @Test
  public void testFinalFilter() throws Exception {
    assertFullInstrumentedFilter(FinalFilter.class);

    invokeInit(FinalFilter.class, 1);
  }

  @Test
  public void testNormalFilter() throws Exception {
    assertFullInstrumentedFilter(NormalFilter.class);

    invokeInit(NormalFilter.class, 1);
  }

  @Test
  public void testDerivedFilter() throws Exception {
    // This is flaky test as as we might not know that DerivedFilter is Filter -
    // it inherits from
    // AbstractFilter, but doesn't explicitly implement Filter.
    Filter filter = simplyInvokeInit(DerivedFilter.class);
    if (SessionHelpers.initSessionManagementCalled.get() != 1
        && SessionHelpers.initSessionManagementCalled.get() != 2) {
      fail(
          "Filter initialization method should have been called 1 or 2 times - this is flaky test and number of invocation depends on class-loading order.");
    }
    assertEquals("Class should have been initialized", Boolean.TRUE, DerivedFilter.class.getField("init").get(filter));
  }

  @Test
  public void testDerivedAndImplementsFilter() throws Exception {
    assertFullInstrumentedFilter(DerivedAndImplementsFilter.class);
    invokeInit(DerivedAndImplementsFilter.class, 2);
  }

  @Test
  public void testAbstractFilter() throws Exception {
    assertFullInstrumentedFilter(AbstractFilter.class);
  }

  static void assertFullInstrumentedFilter(Class<?> clazz) throws Exception {
    assertTrue("Missing destroy method", hasDestroy(clazz));
    assertTrue("Missing declared init method", hasDeclaredInit(clazz));
    assertTrue("Missing declared $$injected_initForSession method", hasDeclaredInitForSession(clazz));
    assertTrue("Missing declared doFilter method", hasDeclaredDoFilter(clazz));
  }

  static boolean hasDestroy(Class<?> clazz) throws Exception {
    Method[] methods = clazz.getMethods();
    for (Method m : methods) {
      if (m.getName().equals("destroy")) {
        return true;
      }
    }
    return false;
  }

  static void invokeInit(Class<?> clazz, int expectedCalls) throws Exception {
    Filter filter = simplyInvokeInit(clazz);
    assertEquals("Filter initialization method should have been called this number of times", expectedCalls,
        SessionHelpers.initSessionManagementCalled.get());
    assertEquals("Class should have been initialized", Boolean.TRUE, clazz.getField("init").get(filter));
  }

  static Filter simplyInvokeInit(Class<?> clazz) throws Exception {
    if (Filter.class.isAssignableFrom(clazz)) {
      @SuppressWarnings("unchecked")
      Class<? extends Filter> filterClass = (Class<? extends Filter>)clazz;
      FilterConfig filterConfig = mock(FilterConfig.class);
      ServletContext servletContext = new MockServletContext();
      when(filterConfig.getServletContext()).thenReturn(servletContext);
      Filter filter = filterClass.newInstance();
      assertNotNull(filter);
      SessionHelpers.resetForTests();
      filter.init(filterConfig);
      return filter;
    } else {
      fail("Not a Filter: " + clazz);
      return null;
    }
  }

  static boolean hasDeclaredInit(Class<?> clazz) throws Exception {
    Method[] methods = clazz.getDeclaredMethods();
    for (Method m : methods) {
      if (m.getName().equals("init") && m.getParameterTypes().length == 1
          && FilterConfig.class.isAssignableFrom(m.getParameterTypes()[0])) {
        return true;
      }
    }
    return false;
  }

  static boolean hasDeclaredInitForSession(Class<?> clazz) throws Exception {
    Method[] methods = clazz.getDeclaredMethods();
    for (Method m : methods) {
      if (m.getName().equals("$$injected_initForSession") && m.getParameterTypes().length == 1
          && FilterConfig.class.isAssignableFrom(m.getParameterTypes()[0])) {
        return true;
      }
    }
    return false;
  }

  static boolean hasDeclaredDoFilter(Class<?> clazz) throws Exception {
    Method[] methods = clazz.getDeclaredMethods();
    for (Method m : methods) {
      if (m.getName().equals("doFilter")) {
        return true;
      }
    }
    return false;
  }
}
