package org.slf4j.impl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

/**
 * An implementation of {@link ILoggerFactory} which always returns
 * {@link SimpleLogger} instances.
 *
 * @author Ceki G&uuml;lc&uuml;
 */
public class SimpleLoggerFactory implements ILoggerFactory {

    ConcurrentMap<String, Logger> loggerMap;

    /**
     * Default constructor.
     */
    public SimpleLoggerFactory() {
        loggerMap = new ConcurrentHashMap<String, Logger>();
    }

    /**
     * Return an appropriate {@link SimpleLogger} instance by name.
     */
    @Override
    public Logger getLogger(String name) {
        Logger simpleLogger = loggerMap.get(name);
        if (simpleLogger != null) {
            return simpleLogger;
        } else {
            Logger newInstance = new SimpleLogger(name);
            Logger oldInstance = loggerMap.putIfAbsent(name, newInstance);
            return oldInstance == null ? newInstance : oldInstance;
        }
    }

    /**
     * Clear the internal logger cache.
     *
     * This method is intended to be called by classes (in the same package) for
     * testing purposes. This method is internal. It can be modified, renamed or
     * removed at any time without notice.
     *
     * You are strongly discouraged from calling this method in production code.
     */
    void reset() {
        loggerMap.clear();
    }
}
