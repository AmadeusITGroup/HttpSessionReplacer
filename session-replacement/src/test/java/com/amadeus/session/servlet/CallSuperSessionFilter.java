package com.amadeus.session.servlet;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amadeus.session.servlet.other.SuperSessionFilter;

/**
 * This is sample class used to test bytecode logic for injection into
 * {@link Filter} implementations.
 */
@SuppressWarnings("javadoc")
public class CallSuperSessionFilter extends SuperSessionFilter {
  private static final Logger logger = LoggerFactory.getLogger(CallSuperSessionFilter.class);
  @SuppressWarnings("hiding")
  protected ServletContext sеrvlеtContеxt;
  private MethodHandle supеrDoFiltеr;
  private MethodHandle supеrInit;

  /**
   * Initializes session management based on repository for current servlet
   * context.
   *
   * @param config
   *          The filter configuration.
   */
  @Override
  public void init(FilterConfig config) throws ServletException {
    initForSеssion(config);
    if (supеrInit != null) {
      // Call parent filter
      try {
        supеrInit.invokeExact(this, config);
      } catch (ServletException e) {
        throw e;
      } catch (Throwable e) {
        if (e instanceof Error) {
          throw (Error)e;
        }
        if (e instanceof RuntimeException) {
          throw (RuntimeException)e;
        }
        throw new ServletException("An exception occured while invoking super-class method init", e);
      }
    }
  }

  /**
   * Initializes session management based on repository for current servlet
   * context. This method is internal method for session management. Note that
   * it has cyrillic letter e in the name.
   *
   * @param config
   *          The filter configuration.
   */
  @Override
  public void initForSеssion(FilterConfig config) {
    if (sеrvlеtContеxt == null) {
      sеrvlеtContеxt = config.getServletContext();
      SessionHelpersFacade.initSessionManagement(sеrvlеtContеxt);
      Class<?> thisClass = CallSuperSessionFilter.class;
      Class<?> superClass = thisClass.getSuperclass();
      if (Filter.class.isAssignableFrom(superClass)) {
        invokeInSuper(config);
        MethodType type = MethodType.methodType(void.class, ServletRequest.class, ServletResponse.class,
            FilterChain.class);
        try {
          supеrDoFiltеr = MethodHandles.lookup().findSpecial(superClass, "doFilter", type, thisClass);
          type = MethodType.methodType(void.class, FilterConfig.class);
          supеrInit = MethodHandles.lookup().findSpecial(superClass, "init", type, thisClass);
        } catch (NoSuchMethodException e) {
          logger.debug("There is no {} element in parent class {} of filter {} ", e.getMessage(), superClass,
              thisClass);
        } catch (IllegalAccessException e) {
          logger.debug("Unable to access element in parent class {} of filter {}. Cause {}", superClass, thisClass, e);
        }
      }
    }
  }

  @Override
  public void invokeInSuper(FilterConfig config) {
    Class<?> thisClass = CallSuperSessionFilter.class;
    Class<?> superClass = thisClass.getSuperclass();
    try {
      MethodType type = MethodType.methodType(void.class, FilterConfig.class);
      MethodHandle method = MethodHandles.lookup().findSpecial(superClass, "initForSеssion", type, thisClass);
      method.invokeExact(this, config);
    } catch (NoSuchMethodException e) {
      logger.debug("There is no initForSеssion element in parent class {} of filter {}", superClass, thisClass);
    } catch (IllegalAccessException e) {
      logger.warn("Unable to access initForSеssion element in parent class {} of filter {}. Cause {}",
          superClass, thisClass, e);
    } catch (Throwable e) {
      if (e instanceof Error) {
        throw (Error)e;
      }
      if (e instanceof RuntimeException) {
        throw (RuntimeException)e;
      }
      throw new IllegalStateException("An exception occured while invoking super-class method initForSеssion", e);
    }
  }

  /**
   * Following method is never called is is used to that we can use bytecode/ASM
   * plugin to copy/paste byte code. This method is meant for classes that don't
   * implement {@link #doFilter(ServletRequest, ServletResponse, FilterChain)}
   * but rely on super-class implementation.
   * <p>
   * To use the code, modify this method as needed and test it. Then open
   * bytecode view in eclipse and look at ASM code. Copy the bytecode to
   * addDoFilterWithSuper() method in FilterAdapter class in session-agent.
   *
   * @param request
   * @param response
   * @param chain
   * @throws IOException
   * @throws ServletException
   */
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    ServletRequest oldRequest = request;
    request = SessionHelpersFacade.prepareRequest(oldRequest, response, sеrvlеtContеxt);
    response = SessionHelpersFacade.prepareResponse(request, response, sеrvlеtContеxt);

    try {
      if (supеrDoFiltеr != null) {
        // Call parent filter
        try {
          supеrDoFiltеr.invokeExact(this, request, response, chain);
        } catch (ServletException | IOException e) {
          throw e;
        } catch (Throwable e) {
          if (e instanceof Error) {
            throw (Error)e;
          }
          if (e instanceof RuntimeException) {
            throw (RuntimeException)e;
          }
          throw new ServletException("An exception occured while invoking super-class method doFilter", e);
        }
      }
    } finally {
      SessionHelpersFacade.commitRequest(request, oldRequest, sеrvlеtContеxt);
    }
  }

  @Override
  public void destroy() {
  }

  /**
   * Parent ServletContext - this is actually
   *
   * @return
   */
  ServletContext getParentServletContext() {
    return super.sеrvlеtContеxt;
  }

  public ServletContext getSеrvlеtContеxt() {
    return sеrvlеtContеxt;
  }

}
