package com.amadeus.session.servlet;

import static com.amadeus.session.servlet.Attributes.PROVIDERS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterRegistration.Dynamic;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.amadeus.session.SessionConfiguration;
import com.amadeus.session.repository.inmemory.InMemoryRepositoryFactory;
import com.amadeus.session.repository.redis.JedisSessionRepositoryFactory;

@SuppressWarnings("javadoc")
public class TestInitializeSessionManagement {

  @Test
  public void testNotEnabled() throws ServletException {
    InitializeSessionManagement ism = new InitializeSessionManagement();
    ServletContext context = mock(ServletContext.class);
    when(context.getInitParameter(SessionConfiguration.DISABLED_SESSION)).thenReturn("true");
    ism.onStartup(null, context);
    verify(context, never()).addFilter(eq("com.amdeus.session.filter"), any(SessionFilter.class));
  }

  @Test
  public void testDefaultWithNullClasses() throws ServletException {
    InitializeSessionManagement ism = new InitializeSessionManagement();
    ServletContext context = mock(ServletContext.class);
    when(context.getClassLoader()).thenReturn(this.getClass().getClassLoader());
    Dynamic dynamic = mock(Dynamic.class);
    when(context.addFilter(any(String.class), any(Filter.class))).thenReturn(dynamic);
    ism.onStartup(null, context);
    verify(context).addFilter(eq("com.amdeus.session.filter"), any(SessionFilter.class));
  }

  @Test
  public void testDefault() throws ServletException {
    InitializeSessionManagement ism = new InitializeSessionManagement();
    Set<Class<?>> classes = Collections.emptySet();
    ServletContext context = mock(ServletContext.class);
    Dynamic dynamic = mock(Dynamic.class);
    when(context.addFilter(any(String.class), any(Filter.class))).thenReturn(dynamic);
    when(context.getClassLoader()).thenReturn(this.getClass().getClassLoader());
    ism.onStartup(classes, context);
    verify(context).addFilter(eq("com.amdeus.session.filter"), any(SessionFilter.class));
  }

  @Test
  public void testWithProviders() throws ServletException {
    InitializeSessionManagement ism = new InitializeSessionManagement();
    ServletContext context = mock(ServletContext.class);
    Dynamic dynamic = mock(Dynamic.class);
    when(context.addFilter(any(String.class), any(Filter.class))).thenReturn(dynamic);
    when(context.getClassLoader()).thenReturn(this.getClass().getClassLoader());
    ism.onStartup(null, context);
    @SuppressWarnings("rawtypes")
    ArgumentCaptor<HashMap> arg = ArgumentCaptor.forClass(HashMap.class);
    verify(context).setAttribute(eq(PROVIDERS), arg.capture());
    assertTrue(arg.getValue().containsKey("redis"));
    assertEquals(JedisSessionRepositoryFactory.class.getName(), arg.getValue().get("redis"));
    assertTrue(arg.getValue().containsKey("in-memory"));
    assertEquals(InMemoryRepositoryFactory.class.getName(), arg.getValue().get("in-memory"));
  }

}
