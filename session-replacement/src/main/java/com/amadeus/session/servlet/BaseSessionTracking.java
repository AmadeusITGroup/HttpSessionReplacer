package com.amadeus.session.servlet;

import java.nio.ByteBuffer;

import javax.servlet.ServletContext;

import com.amadeus.session.RandomIdProvider;
import com.amadeus.session.RequestWithSession;
import com.amadeus.session.SessionConfiguration;
import com.amadeus.session.SessionIdProvider;
import com.amadeus.session.SessionTracking;
import com.amadeus.session.UuidProvider;
import com.amadeus.session.Base64MaskingHelper;

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
    String newId = idProvider.newId();
    if (appendTimestamp) {
      StringBuilder suffixedId = new StringBuilder(newId);
      ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
      newId = suffixedId.append(SESSION_ID_TIMESTAMP_SEPARATOR)
              .append(Base64MaskingHelper.encode(buffer.putLong(System.currentTimeMillis()).array()))
              .toString();
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
