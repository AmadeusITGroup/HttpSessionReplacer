package com.amadeus.session.repository.redis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class for redis facades. Contains methods that are common for both
 * single/sentinel node facade and cluster facade.
 *
 */
abstract class AbstractRedisFacade implements RedisFacade {
  private static final String CRLF = "\r\n"; 
  private static final String REDIS_VERSION_LABEL = "redis_version:";
  private static final Integer[] MIN_MULTISPOP_VERSION = new Integer[]{3,2};

  private List<Integer> version;

  /**
   * Multi spop is only supported redis 3.2+.
   */
  @Override
  public boolean supportsMultiSpop() {
    readVersion();
    for (int i = 0; i < MIN_MULTISPOP_VERSION.length && i < version.size(); i++) {
      if (version.get(i) < MIN_MULTISPOP_VERSION[i]) {
        return false;
      }
    }
    return true;
  }

  /**
   * Reads and parses version information from redis server. See
   * http://redis.io/commands/INFO for details how to obtain redis
   * version.
   */
  void readVersion() {
    if (version == null) {
      String info = info("server");
      if (info != null) {
        int start = info.indexOf(REDIS_VERSION_LABEL);
        if (start >= 0) {
          start += REDIS_VERSION_LABEL.length();
          // In RESP different parts of the protocol are always terminated with "\r\n" (CRLF).
          int end = info.indexOf(CRLF, start);
          if (end < 0) {
            end = info.length();
          }
          String[] coordiantes = info.substring(start, end).split("\\.");
          version = new ArrayList<>();
          for (String coordinate : coordiantes) {
            version.add(Integer.parseInt(coordinate));
          }
        }
      }
      if (version == null) {
        version = Collections.singletonList(0);
      }
    }
  }
}