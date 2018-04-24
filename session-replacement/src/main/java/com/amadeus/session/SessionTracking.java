package com.amadeus.session;

/**
 * A service for extracting session id from request, propagating session into
 * response or generating new session id.
 */
public interface SessionTracking {
  /**
   * Configures tracking.
   *
   * @param sessionConfiguration
   *          the session configuration
   */
  void configure(SessionConfiguration sessionConfiguration);

  /**
   * Retrieves session id from the request
   *
   * @param request
   *          the current request object
   * @return session id and source type or null if id is not present in the request
   */
  IdAndSource retrieveId(RequestWithSession request);

  /**
   * Propagates session to client. Implementation must allow multiple idempotent
   * calls for same request.
   *
   * @param request
   *          the current request object
   * @param response
   *          the response object
   */
  void propagateSession(RequestWithSession request, Object response);

  /**
   * Generates new session id.
   *
   * @return new session id
   */
  String newId();

  /**
   * Encodes passed URL adding session if needed.
   *
   * @param request
   *          the current request object
   * @param url
   *          the URL to encode
   * @return encoded URL
   */
  String encodeUrl(RequestWithSession request, String url);

  /**
   * Tells whether the tracking is done using cookies or URL rewrite (example with ;jsessionid=... in the URL).
   * 
   * @return true if tracking is done with cookies, false if it is done using URL rewrite
   */
  boolean isCookieTracking();

  /**
   * If there are multiple ways of session tracking, sets next session tracking implementation
   * in the chain of responsibilities. 
   * 
   * @param next
   *          sets next session tracking implementation in chain of responsabilities 
   */
  void nextSessionTracking(SessionTracking next);

  /**
   * Represtents session id and source type;
   */  
  public static class IdAndSource {
    public final String id;
    public final boolean cookie;
    
    public IdAndSource(String id, boolean cookie) {
      this.id = id;
      this.cookie = cookie;
    }

    public String toString() {
      return "IdAndSource [id="+id+"; cookie="+cookie+"]";
    }
  }
}
