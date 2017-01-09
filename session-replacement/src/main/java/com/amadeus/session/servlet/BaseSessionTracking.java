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

  private boolean appendTimeStamp;

  @Override
  public void configure(SessionConfiguration configuration) {
    // Read standard configuration
    idName = configuration.getSessionIdName();
    String idProviderType = configuration.getAttribute(SessionConfiguration.SESSION_ID_PROVIDER, "random");
    appendTimeStamp = configuration.isTimeStampSufix();
    switch (idProviderType) {
    case "uuid":
      idProvider = new UuidProvider();
      break;
    default:
      idProvider = new RandomIdProvider();
      break;
    }
    idProvider.configure(configuration);
  }

  @Override
  public String newId() {
    if (appendTimeStamp) {
      StringBuilder newId = new StringBuilder().append(idProvider.newId());
      return newId.append("!").append(System.currentTimeMillis()).toString();
    }
    else {
      return idProvider.newId();
    }
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
    return idProvider.readId(value);
  }

}
