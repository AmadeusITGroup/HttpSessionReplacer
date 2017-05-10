package com.amadeus.session.servlet;

import javax.servlet.ServletContext;

import com.amadeus.session.RandomIdProvider;
import com.amadeus.session.RequestWithSession;
import com.amadeus.session.SessionConfiguration;
import com.amadeus.session.SessionIdProvider;
import com.amadeus.session.SessionTracking;
import com.amadeus.session.UuidProvider;

/**
 * This base class for session ID tracking that can be configured via
 * {@link ServletContext}.
 */
public abstract class BaseSessionTracking implements SessionTracking {

  protected String idName;

  protected SessionIdProvider idProvider;

  private boolean appendTimestamp;

  protected static final char SESSION_ID_TIMESTAMP_SEPARATOR = '!';

  @Override
  public void configure(SessionConfiguration configuration) {
    // Read standard configuration
    idName = configuration.getSessionIdName();
    String idProviderType = configuration.getAttribute(SessionConfiguration.SESSION_ID_PROVIDER, "random");
    appendTimestamp = configuration.isTimestampSufix();
    if ("uuid".equals(idProviderType)) {
      idProvider = new UuidProvider();
    }
    else {
      idProvider = new RandomIdProvider();
    }
    idProvider.configure(configuration);
  }

  @Override
  public String newId() {
    String newId = idProvider.newId();
    if (appendTimestamp) {
        StringBuilder suffixedId = new StringBuilder(newId.length() + 11).append(newId);
        newId = suffixedId.append(SESSION_ID_TIMESTAMP_SEPARATOR).append(System.currentTimeMillis()).toString();
    }
    return newId;
  }

  @Override
  public String encodeUrl(RequestWithSession request, String url) {
    return url;
  }

  /**
   * Returns cleaned value of the session or <code>null</code> if cookie value
   * has invalid id format.
   *
   * @param value
   *          the cookie value
   * @return extracted id or <code>null</code>
   */
  protected String clean(String value) {
    if (!appendTimestamp) {
      return idProvider.readId(value);
    }
    String timeStamp = "";
    String cleanValue = value;
    int separatorIndex = value.lastIndexOf(SESSION_ID_TIMESTAMP_SEPARATOR);
    if (separatorIndex != -1) {
        timeStamp = value.substring(separatorIndex);
        cleanValue = value.substring(0, separatorIndex);
    }
    cleanValue = idProvider.readId(cleanValue);
    return cleanValue != null ? cleanValue + timeStamp : cleanValue;
  }

}
