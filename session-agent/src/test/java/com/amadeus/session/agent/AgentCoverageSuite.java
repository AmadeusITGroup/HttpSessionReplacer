package com.amadeus.session.agent;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  TestEnhanceAddListener.class,
  TestSessionAgent.class,
  TestServletContextAdapter.class,
  TestServletContextAndFilterTransformer.class
})
public class AgentCoverageSuite {

}
