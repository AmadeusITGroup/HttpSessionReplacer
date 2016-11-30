/**
 * Contains fork of Slf4J classes and StaticLoggerBinder that allows automatic
 * selection of one of several logging frameworks. The selection is done in
 * following order:
 *
 * <ul>
 * <li>log4j (if <code>org.apache.log4j.Logger</code> class is present in class
 * loader)
 * <li>commons logging (if <code>org.apache.commons.logging.Log</code> class is
 * present in class loader)
 * <li>JDK4 logging
 * <li>simple logging to <code>stderr</code>
 * </ul>
 */
package org.slf4j.impl;