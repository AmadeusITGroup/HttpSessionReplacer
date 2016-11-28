/**
 * Copyright (c) 2004-2011 QOS.ch
 * All rights reserved.
 *
 * Permission is hereby granted, free  of charge, to any person obtaining
 * a  copy  of this  software  and  associated  documentation files  (the
 * "Software"), to  deal in  the Software without  restriction, including
 * without limitation  the rights to  use, copy, modify,  merge, publish,
 * distribute,  sublicense, and/or sell  copies of  the Software,  and to
 * permit persons to whom the Software  is furnished to do so, subject to
 * the following conditions:
 *
 * The  above  copyright  notice  and  this permission  notice  shall  be
 * included in all copies or substantial portions of the Software.
 *
 * THE  SOFTWARE IS  PROVIDED  "AS  IS", WITHOUT  WARRANTY  OF ANY  KIND,
 * EXPRESS OR  IMPLIED, INCLUDING  BUT NOT LIMITED  TO THE  WARRANTIES OF
 * MERCHANTABILITY,    FITNESS    FOR    A   PARTICULAR    PURPOSE    AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE,  ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package org.slf4j.impl;

import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggerFactoryBinder;

/**
 * The binding of {@link LoggerFactory} class with an actual instance of
 * {@link ILoggerFactory} is performed using information returned by this class.
 * We've chosen different strategy compared to standard SLF4J framework. Instead
 * of providing implementation in the classpath at runtime, we try to find
 * which of well known logging libraries is present and use that library
 * as underlying logging mechanism. The order of priorities is as follows:
 * <ul>
 * <li>log4j
 * <li>commons logging
 * <li>JDK4 logging
 * <li>simple logging to <code>stderr</code>
 * </ul>
 */
public final class StaticLoggerBinder implements LoggerFactoryBinder {

  static {
    String lfc;
    String mdca;
    try {
      Class.forName("org.apache.log4j.Logger");
      lfc = "org.slf4j.impl.Log4jLoggerFactory";
      mdca = "org.slf4j.impl.Log4jMDCAdapter";
    } catch (ClassNotFoundException e) { // NOSONAR we can't log here as we are actually instantiating log here
      try {
        Class.forName("org.apache.commons.logging.Log");
        lfc = "org.slf4j.impl.JCLLoggerFactory";
        mdca = "org.slf4j.helpers.NOPMDCAdapter";
      } catch (ClassNotFoundException e1) { // NOSONAR we can't log here as we are actually instantiating log here
        try {
          Class.forName("java.util.logging.Logger");
          lfc = "org.slf4j.impl.JDK14LoggerFactory";
          mdca = "org.slf4j.helpers.BasicMDCAdapter";
        } catch (ClassNotFoundException e2) { // NOSONAR we can't log here as we are actually instantiating log here
          lfc = "org.slf4j.impl.SimpleLoggerFactory";
          mdca = "org.slf4j.helpers.NOPMDCAdapter";
        }
      }
    }
    loggerFactoryClassStr = lfc;
    mdcAdapterClassStr = mdca;
    SINGLETON = new StaticLoggerBinder();
  }

    /**
     * The unique instance of this class.
     *
     */
    private static final StaticLoggerBinder SINGLETON;

    /**
     * Return the singleton of this class.
     *
     * @return the StaticLoggerBinder singleton
     */
    public static final StaticLoggerBinder getSingleton() {
        return SINGLETON;
    }

    /**
     * Declare the version of the SLF4J API this implementation is compiled against.
     * The value of this field is modified with each major release.
     */
    // to avoid constant folding by the compiler, this field must *not* be final
    public static String REQUESTED_API_VERSION = "1.6.99"; // NOSONAR !final

    private static final String loggerFactoryClassStr; // NOSONAR
    static final String mdcAdapterClassStr; // NOSONAR

    /**
     * The ILoggerFactory instance returned by the {@link #getLoggerFactory}
     * method should always be the same object
     */
    private final ILoggerFactory loggerFactory;

    private StaticLoggerBinder() {
        ILoggerFactory lf ;
        try {
          lf = (ILoggerFactory) Class.forName(loggerFactoryClassStr).newInstance();
        } catch (Exception e) { // NOSONAR we can't log here as we are actually instantiating log here
          System.err.println("Unable to instantiate logger factory " + loggerFactoryClassStr);  // NOSONAR
          e.printStackTrace(); // NOSONAR
          lf = new SimpleLoggerFactory();
        }
        loggerFactory = lf;
    }

    @Override
    public ILoggerFactory getLoggerFactory() {
        return loggerFactory;
    }

    @Override
    public String getLoggerFactoryClassStr() {
        return loggerFactoryClassStr;
    }
}
