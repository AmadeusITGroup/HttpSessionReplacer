package com.amadeus.session;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * The class is used to track the number of item during a time.
 * input parameter
 *  delay , time in milliseconds where items are keep;
 *  max: if the number if items is more than the max value the ErrorTracker reachLimits   
 *
 */

public class ErrorTracker {
  public ErrorTracker(int delay , int max) {
    super();
    this.delay = delay;
    this.max = max;
  }

  private ConcurrentLinkedQueue<Long> list = new ConcurrentLinkedQueue<Long>();

  /**
   * The class calculate the items during a  time
   */
  final int delay ;
  /**
   * After the limits the Tracker is considering in error
   */
  final int max ;
  
  public void addError( long now )  {
    
    list.add(new Long(now));
    boolean cont = true; 
    while ( cont ) {
      Long last = list.peek();
      if ( now - last > delay ) {
        list.poll();
      } else {
        cont = false;
      }
    }
    
  }

  public boolean reachLimits() {
    return list.size() >= max;
  }

  public int size() {
    return list.size();
  }

  public void reset() {
    list.clear();    
    
  }
}
