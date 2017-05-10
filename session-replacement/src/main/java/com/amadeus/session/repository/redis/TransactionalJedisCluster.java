package com.amadeus.session.repository.redis;

import java.util.Set;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisClusterCommand;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Transaction;

/**
 * Extension of Redis cluster interface that supports transactions.
 */
class TransactionalJedisCluster extends JedisCluster {


  TransactionalJedisCluster(Set<HostAndPort> hostAndPort, int timeout, JedisPoolConfig config) {
    super(hostAndPort, timeout, config);
  }

  /**
   * Implementation of transaction functionality on redis cluster. Transactions
   * are linked to a single key, and thus are insured to run on the same node.
   *
   * @param key
   *          key to which transaction is related
   * @param transaction
   *          the sequence of redis commands to run
   * @return result of transaction
   */
  public <T> RedisFacade.ResponseFacade<T> transaction(final byte[] key, final RedisFacade.TransactionRunner<T> transaction) {
    return new JedisClusterCommand<RedisFacade.ResponseFacade<T>>(connectionHandler, maxAttempts) {
      @Override
      public RedisFacade.ResponseFacade<T> execute(Jedis connection) {
        Transaction t = connection.multi();
        RedisFacade.ResponseFacade<T> response = transaction.run(AbstractJedisFacade.wrapJedisTransaction(t));
        t.exec();
        return response;
      }
    }.runBinary(key);
  }

  /**
   * Intentionally providing the functionality in order to get Redis version.
   */
  @Override
  public String info(final String section) {
    // INFO command can be sent to any node
    return new JedisClusterCommand<String>(connectionHandler, maxAttempts) {
      @Override
      public String execute(Jedis connection) {
        return connection.info(section);
      }
    }.runWithAnyNode();
  }
}
