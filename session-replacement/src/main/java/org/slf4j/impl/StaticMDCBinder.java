package org.slf4j.impl;

import org.slf4j.helpers.NOPMDCAdapter;
import org.slf4j.spi.MDCAdapter;

/**
 * This implementation is bound to {@link NOPMDCAdapter}.
 *
 * @author Ceki G&uuml;lc&uuml;
 */
public class StaticMDCBinder {

    /**
     * The unique instance of this class.
     */
    public static final StaticMDCBinder SINGLETON = new StaticMDCBinder();

    private StaticMDCBinder() {
    }

    /**
     * Return the singleton of this class.
     *
     * @return the StaticMDCBinder singleton
     * @since 1.7.14
     */
    public static final StaticMDCBinder getSingleton() {
        return SINGLETON;
    }

    /**
     * Currently this method always returns an instance of
     * {@link StaticMDCBinder}.
     * @return
     */
    public MDCAdapter getMDCA() {
      try {
        return (MDCAdapter) Class.forName(getMDCAdapterClassStr()).newInstance();
      } catch (Exception e) { // NOSONAR we can't log here as we are actually instantiating log here
        System.err.println("Unable to instantiate mdc adapter " + getMDCAdapterClassStr()); // NOSONAR
        e.printStackTrace(); // NOSONAR
        return new NOPMDCAdapter();
      }
    }

    /**
     * Name of MDC adapter class
     * @return
     */
    public String getMDCAdapterClassStr() {
        return StaticLoggerBinder.mdcAdapterClassStr;
    }
}
