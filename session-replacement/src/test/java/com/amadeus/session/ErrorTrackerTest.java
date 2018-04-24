package com.amadeus.session;

import static org.junit.Assert.*;

import org.junit.Test;

public class ErrorTrackerTest {

  @Test
  public void test() throws InterruptedException{
    ErrorTracker d = new ErrorTracker(10,20);

    d.addError( 0 );
    assertEquals(1, d.size());
    
    d.addError( 1 );
    assertEquals(2, d.size());
    
    d.addError( 2 );
    assertEquals(3, d.size());

    d.addError( 3 );
    assertEquals(4, d.size());

    d.addError( 4 );
    assertEquals(5, d.size());

    d.addError( 5 );
    assertEquals(6, d.size());

    d.addError( 6 );
    assertEquals(7, d.size());
    
    
    d.addError( 7 );
    assertEquals(8, d.size());
    
    d.addError( 8 );
    assertEquals(9, d.size());
    
    d.addError( 9 );
    assertEquals(10, d.size());
    
    d.addError( 10 );
    assertEquals(11, d.size());

    d.addError( 11 );
    assertEquals(11, d.size());

    d.addError( 11 );
    assertEquals(12, d.size());
    assertEquals(false, d.reachLimits());

    d.addError( 11 );
    assertEquals(13, d.size());
    assertEquals(false, d.reachLimits());

    d.addError( 11 );
    assertEquals(14, d.size());
    assertEquals(false, d.reachLimits());

    d.addError( 11 );
    assertEquals(15, d.size());
    assertEquals(false, d.reachLimits());

    d.addError( 11 );
    assertEquals(16, d.size());
    assertEquals(false, d.reachLimits());

    d.addError( 11 );
    assertEquals(17, d.size());
    assertEquals(false, d.reachLimits());

    d.addError( 11 );
    assertEquals(18, d.size());
    assertEquals(false, d.reachLimits());

    d.addError( 11 );
    assertEquals(19, d.size());
    assertEquals(false, d.reachLimits());

    d.addError( 11 );
    assertEquals(20, d.size());
    assertEquals(true, d.reachLimits());
    
    d.reset();
    assertEquals(0, d.size());
    assertEquals(false, d.reachLimits());
    
  }

}
