package com.amadeus.session.repository.inmemory;

import com.amadeus.session.SessionConfiguration;
import com.amadeus.session.SessionRepository;
import com.amadeus.session.SessionRepositoryFactory;

/**
 * This class creates in-memory session repositories. In-memory session
 * repositories are not replicated and are intended for use with
 * non-distributable sessions and for testing purposes.
 */
public class InMemoryRepositoryFactory implements SessionRepositoryFactory {

  @Override
  public SessionRepository repository(SessionConfiguration sessionConfiguration) {
    return new InMemoryRepository(sessionConfiguration.getNamespace());
  }

  @Override
  public boolean isDistributed() {
    return false;
  }
}
