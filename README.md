# HTTP Session Replacement

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.amadeus/session/badge.svg?style=flat-square)](https://maven-badges.herokuapp.com/maven-central/com.amadeus/session)
[![Javadocs](http://www.javadoc.io/badge/com.amadeus/session-replacement.svg?style=flat-square)](http://www.javadoc.io/doc/com.amadeus/session-replacement)
[![Travis](https://img.shields.io/travis/AmadeusITGroup/HttpSessionReplacer.svg?style=flat-square)](http://travis-ci.org/AmadeusITGroup/HttpSessionReplacer)
[![license](https://img.shields.io/github/license/AmadeusITGroup/HttpSessionReplacer.svg?style=flat-square)](LICENSE)
[![Dependency Status](https://www.versioneye.com/user/projects/583d2f19d2fd57003fdfbe76/badge.svg?style=flat-square)](https://www.versioneye.com/user/projects/583d2f19d2fd57003fdfbe76/)
[![SonarQube Coverage](https://img.shields.io/sonar/http/sonarqube.com/com.amadeus:session/coverage.svg?style=flat-square)](https://sonarqube.com/dashboard?id=com.amadeus%3Asession)
[![SonarQube Tech Debt](https://img.shields.io/sonar/http/sonarqube.com/com.amadeus:session/tech_debt.svg?style=flat-square)](https://sonarqube.com/dashboard?id=com.amadeus%3Asession)

This project provides session management including, possibly, distributed
session repository for JEE and other java containers. Default implementation
comes with in-memory and Redis based implementation.

The project is inspired by Spring Session project and reuses some of redis logic from it.
Its objective is to avoid any dependency on Spring libraries, and, so, make it usable in applications that
don't use Spring, or that use an older version.
The implementation, however, uses the Jedis library directly.
This makes the algorithm easier to port to other languages.

The project aims to make session management transparent for existing webapps (zero code
change) and as compatible as possible with wide variety of the JEE containers, all the while
offering full support for session API including different session listeners.

Redis support includes both single instance, sentinel based and cluster modes.
Two session expiration strategies are available when using Redis. One is based
on Redis notifications, and antoher on sorted sets (ZRANGE).

Useful links:

* For details about installing the library see [docs/Usage.md](docs/Usage.md)
* For information about building this project see [docs/BUILD.md](docs/BUILD.md).
* For information about using session encryption see [docs/ENCRYPTION.md](docs/ENCRYPTION.md).

## HTTP Servlet support

The primary usecase is the support for session management for `HttpSessions`.
Support includes the following:

* Creation of sessions on demand
* Storing of session attributes between requests
* Invalidation of sessions
* Session expiration management
* Session propagation to clients via cookie and URL.
* Full support for all non-deprecated `HttpSession` methods
* Support for callbacks for values stored in session that implement
  `javax.servlet.http.HttpSessionActivationListener` or
  `javax.servlet.http.HttpSessionBindingListener`
* When used with an agent, support for listener objects such as
  `javax.servlet.http.HttpSessionListener` and
  `javax.servlet.http.HttpSessionAttributeListener`
* Support for Servlet 3.1 features such as session id switch
* Compatibility with Servlet 2.5
* Session stickiness - sessions can be sticked to node and expiration events will then be triggered on node owner of session
* Support for non-distributable web applications

### General Concepts

#### Session Repository

The session information needs to be stored between server request.
This storage is called session repository.
Different repository implementations are supported by the session replacement mechanism.

#### Configuration

Most of the configuration can be specified with `ServletContext`s initialization
parameters, system properties and some paramaters can be provided via the agent.
Unless otherwise specified the general rule for priority of configuration is as
follows in descending order:

* `ServletContext`s initiate parameters (set through `web.xml` or programmatically since Servlet 3.x )
* Agent configuration (when exists)
* System properties
* Default values

#### Architecture

Here is the block diagram of the architecture:

```
     +--------------------------------------------------+
     |                                                  |
     | +---+          +-------------------------------+ |
     | |   |          |                               | |
     | |   |          |                               | |
     | | * |          |                               | |
     | | F |          |                               | |
     | | I | Wrapped  |        WEB APPLICATION        | |
     | | L | request  |                               | |
HTTP | | T |--------->|                               | |
---->| | E |          |                               | |
HTTPS| | R |          |                               | |
     | | * |          |                               | |
     | |   |          +-------------------------------+ |
     | |   |                          ^                 |
     | |   |      Session interaction |                 |
     | |   |                          v                 |
     | |   +------------------------------------------+ |
     | |          *Session-management*                | |
     | +----------------------------------------------+ |
     |        Container (e.g. JBoss, jetty, tomcat)      |
     +-------------------------------------+------------+
     |            JVM                      |   *Agent*  |
     +-------------------------------------+------------+
```

In the above picture, *Agent*, *Filter* and *Session-management* are modules added to support
session storage in repository.

* The *Filter* wraps all incoming requests to allow session retrieval and commit.
  When using the agent, all filters have code to wrap incoming requests,
  however, only the first one will wrap it.
  The following filter in chain will return the request it received.
  The canonical implementation of  a filter is
  [com.amadeus.session.servlet.SessionFilter](session-replacement/src/main/java/com/amadeus/session/servlet/SessionFilter.java)

* The *Session-management* is intercepting all interactions with sessions
  and communicates with the session repository.
  Normally, unless using container specific interface,
  container should not be aware of the existence of the session.

* The *Agent* performs instrumentation as described below.

### Session management

The general algorithm for managing sessions is independent of the underlying storage.
It has the following characteristics:

* Session retrieval from repository.

* New session creation of session with cryptographically secure session id.
  For details see Session id section.

* Partial and full session updates allows updating all session attributes
  or only those that were changed or touched during session request (if repository supports it).
  For details see Optimized session updates section.

* Support for atomic commit allows updating all attributes at once in one transaction or network exchange (if repository supports it).

* Support of non-cacheable attributes, i.e. attributes that are stored or retrieved from repository on each access to session attribute.
  For details see Non-sticky sessions and concurrent access.

* Support for session encryption when storing sessions into repository.

#### Optimized session updates

The session management keeps track of the attributes that have changed
(including deletion) and only updates those.
This means if an attribute is written once and read many times we only need to
write that attribute once.

Session management can be configured to update all the attributes no matter
what or to update all non-primitive wrappers

#### Session id

A session id is either an UUID generated using type 4 algorithm or a random
sequence of bytes encoded in modified base64 algorithm.
If a request is made for a session with an id that is expired,
not valid or not present in the repository, the id is invalidated and a new one is generated.
This prevents [simple session fixation attack scenario](https://en.wikipedia.org/wiki/Session_fixation).

##### UUID based session id

The UUID based session id is activated by setting servlet or system property
`com.amadeus.session.id` to `uuid`.
UUID based session id is the default mechanism at the moment,
but this may change before a final release.

If the servlet parameter or system property
`com.amadeus.session.noHyphensInId` is set to `true`,
hyphens are removed from UUID.

##### Random session id

The random session id is activated by setting servlet or system property
`com.amadeus.session.id` to `random`.
This may become default strategy before the final release.

Random session id length is specified in bytes using the servlet parameter or system property
`com.amadeus.session.id.length`.
The length of the id as a string will be 4 characters for
each 3 bytes of the id (with padding up to a number that divides by 4).
E.g for 1, 2 or 3 bytes length there will be 4 characters in the id string,
for 4, 5 or 6 there will be 8, etc.

##### Session format

It is possible to tweak the generated session id format using proper configuration parameter.
Parameter `com.amadeus.session.timestamp` can be used to enforce presence of '!xxxxx' at end of generated jsessionid
xxxxx being the number of millis ellapsed since january 1970 and corresponding to UNIX timestamp.

##### Session isolation

Sessions can be isolated per application.
While this is repository dependent, it is expected that repositories support this.
This isolation is done using unique identifier called namespace and it should be part of the key or repository choice.
The namespace is not communicated to the clients and is only known by the server.

Best practice is to have different namespaces for sessions in different applications or webapps.
If applications want to share sessions they can use the same namespace.

In case of webapps, default behavior is that either the namespace is defined using the servlet initialization
parameter `com.amadeus.session.namespace`, or if not present, the context path of the webapp is used.

**NOTE**: When the context path is used as namespace, two different application
servers with different webapps,
having the same context path will share the same session namespace if they use
the same repository.

Outside servlet containers the default namespace name is `default`.

##### Session id propagation between webapps

When propagating a session id from one webapp to another (e.g. using `RequestDispatcher`),
the session id doesn't change.
The first webapp that got the request is the one that controls and sets the session id.

Note however, that by default each webapp will store its sessions in a
different namespace even if they use the same session id.

### Session propagation

Two builtin strategies are available for session propagation.
The first one is based on cookies and is the default one.
The second one is based on URL rewriting where the session is appended at the
end of the path part of URL (preceding the query).

The session propagation can be configured using web.xml (standard Servlet approach)

```xml
<web-app>
...
  <session-config>
    <tracking-mode>URL</tracking-mode>
  </session-config>
</web-app>
```
It can also be configured using system property or servlet initialization parameter
`com.amadeus.session.tracking`. Valid values are `COOKIE`, `URL` or `DEFAULT`
(which is same as `COOKIE`).

The URL rewriting session propagation is not supported on Tomcat 6 based servlet
engines (Tomcat 6.x, JBoss 6.x).

#### Cookie Session Management

The session is stored as a UUID inside a cookie.
The cookie name is one of the following by descending order of priority:

* Using the `com.amadeus.session.sessionName` initialization parameter of the ServletContext.
* Using the `com.amadeus.session.sessionName` system property.
* `JSESSIONID`.

In case of HTTPS requests, cookies can be marked as secure.
This can be configured by setting the `com.amadeus.session.cookie.secure`
initialization parameter or system property to `true`.

For Servlet 3.x and later containers, cookies can be marked as HTTP only.
This can be configured by setting `com.amadeus.session.cookie.httpOnly`
initialization parameter or system property to `true`.

Cookies apply only on the context path of the web app.
I.e. it is only sent for URLs that
are prefixed by context path.

The cookie expiration is set only if the session has expired,
and the value of expiration is 0 (i.e. immediately).

### Session stickiness

There is no specific support for concurrent calls on the session.
Standard approach to simplify concurrent access to session is to use session stickiness.

Session stickiness implies that subsequent requests for the same session arrive
to the same application server node.
Load-balancers and proxy web servers can provide such stickiness features.
Some vendors call this feature "affinity".
The feature is usually based on HTTP cookies.

In general, the support for stickiness depends also on the repository
implementation.
See the details of the implementation for more information.
The default in-memory repository requires session stickiness.
Redis repository can support session stickiness.

The cookie or URL representation of a session id doesn't carry any information
about the node that owns session, so it is entirely up to load-balancer or
proxy web server to handle it.

As per Servlet 3.1 standard, sessions are considered sticky by default.
The support for stickiness can be disabled using the `com.amadeus.session.sticky`
system property or servlet parameter.

#### Non-sticky sessions and concurrent access

When sessions are used with stickiness disabled,
multiple nodes can access and modify the session at the same time.
The last one modifying the session wins.
The concurrency in this case can be supported by declaring some or all
attribute names as non-cacheable.
When attribute names are declared as non-cacheable,
any access to `HttpSession` retrieving, deleting or storing
attributes will trigger retrieval, deletion or storing in remote repository.
This may have a negative impact on latency as each attribute is retrieved at
the access time and should be used with care.
Non-cacheable attributes are specified as comma-separated list using
`com.amadeus.session.non-cacheable` initialization parameter or system property.

## Agent and instrumentation

The project comes with a java agent that is used to instrument the `ServletContext`
implementation of the JEE container as well as any `Filter` used by web applications.
The agent also allows the setting of global defaults for the session management.

### Filter instrumentation

All classes implementing the `Filter` interface are instrumented to allow
session management.
Following modifications are applied:

* Existing `doFilter` method is renamed to `$$renamed_doFilter`.
* A new method `doFilter` is created, that wraps http request and response
  objects into custom wrappers that are responsible for creating the session.
  The filter commits the session if needed at the end of processing.
  The equivalent of the code is as follows:

```java
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    ServletRequest oldRequest = request;
    request = SessionHelpers.prepareRequest(oldRequest, response);
    response = SessionHelpers.prepareResponse(request, response);
    try {
      doFiltеr(request, response, chain);
    } finally {
      SessionHelpers.commitRequest(request, oldRequest);
    }
  }
```

### ServletContext instrumentation

The agent will instrument `ServletContext` to allow registering and
notification of `HttpSessionListeners` and `HttpSessionAttributeListeners`.
This is done by injecting code that registers listeners associated to
`ServletContext` into the `addListener` method.

### Configuration

To use agent, add the following to the options passed to JVM: `-javagent:session-agent.jar=arg,arg,arg`

All agent arguments will be set into `com.amadeus.session.repository.conf`
system property if it has not been set already. Few of the arguments will be
used by agent itself:

* `provider=factory` class for repository or name of the provider.

* `log=debug` activates debug mode. Not active by default.

* `timeout=` default maximum inactive interval

* `distributable=true`: If set to `false` the session distributution is managed via `web.xml` and `<distrubtale/>` tag.
   Default value is `true` meaning that all sessions will be treated as distributable, even when it is not specified via `web.xml`.

* `interceptListeners=true` is used to discover `HttpSessionListeners` and
  `HttpSessionAttributeListeners` for applications servers where the
  base instrumentation doesn't discover them.

The agent will set following system properties:

* `com.amadeus.session.repository.conf` - specifies configuration for repository provider.
  If not set, will be set to the value of the agent argument string.

* `com.amadeus.session.repository.factory` - specifies factory class for repository provider.
  Can be overridden by the agent parameter `provider`.

* `com.amadeus.session.timeout` - default maximum inactive interval.
  Can be overriden by agent parameter `timeout`.

* `com.amadeus.session.distributable` - if set to `false` the session distributution is managed via `web.xml` and `<distrubtale/>` tag.
  Default value is `true` meaning that all sessions will be treated as distributable, even when it is not specified via `web.xml`.

* `com.amadeus.session.intercept.listeners` - `true` if discovery of listeners
  is done by creating intercepting calls to listeners by the applications
  server.

* `com.amadeus.session.debug` - `true` if agent debug level tracing is activated.


## Session repository

The session repository is the storage were sessions are stored between requests.

### In memory

Default implementation of repository is an `in-memory` repository.
This repository stores sessions in JVM's heap using a ConcurrentHashMap.
It doesn't support fail-over or high availability and is primarily meant for test and development.
There are no specific configuration parameters for this repository.

This repository is used by default for non-distributable web-apps.
It is controlled using the initialization parameter or
system property `com.amadeus.session.distributable.force` which is set to `false` by default.
If this setting has the value `true`, all web-apps, including those without distributable marker,
will be stored in the default repository that supports distribution/replication (i.e. Redis repository).
Note that the `com.amadeus.session.distributable` system property also impacts this behavior.
Following table explains interaction between `web.xml` and these two configuration items:

|com.amadeus.session.distributable.force|com.amadeus.session.distributable|web.xml distributale tag|repository used|
| --------------------------------------- | --------------------------------- | ------------------------ | ---------------                    |
| false                                   | false                             | absent                   | In memory                          |
| false                                   | false                             | present                  | Distributable repository           |
| false                                   | true                              | absent                   | In memory                          |
| false                                   | true                              | present                  | Distributable repository           |
| true                                    | false                             | absent                   | In memory                          |
| true                                    | false                             | present                  | Distributable repository           |
| true                                    | true                              | absent                   | Distributable repository + warning |
| true                                    | true                              | present                  | Distributable repository           |

### Redis repository

**NOTE**: This explanation is adapted from Spring Session.

Sessions are stored using equivalent of the Redis [`HMSET` command](http://redis.io/commands/hmset):

```redis
HMSET com.amadeus.session:webapp-namespace:{33fdd1b6-b496-4b33-9f7d-df96679d32fe} #:creationTime 1404360000000 #:maxInactiveInterval 1800 #:lastAccessedTime 1404360000000 attrName someAttrValue attrName2 someAttrValue2
```

In this example, the session following statements are true about the session:

* The session id is 33fdd1b6-b496-4b33-9f7d-df96679d32fe.
* The session was created at 1404360000000 in milliseconds since midnight of 1/1/1970 GMT.
* The session expires in 1800 seconds (30 minutes).
* The session was last accessed at 1404360000000 in milliseconds since midnight of 1/1/1970 GMT.
* The session has two attributes. The first is "attrName" with the value of "someAttrValue". The second session attribute is named "attrName2" with the value of "someAttrValue2".
* The session belongs to namespace called webapp-namespace.

Session-management handles optimized writes, so if in a request we update
only the session attribute "sessionAttr2",
the following would be executed upon saving:

```redis
HMSET com.amadeus.session:webapp-namespace:{33fdd1b6-b496-4b33-9f7d-df96679d32fe} sessionAttr:attrName2 newValue
```

In addition to the special attributes `#:creationTime`, `#:lastAccessedTime`, and `#:maxInactiveInterval`,
an attribute named `#:invalidSession` is put into the set at the start of the delete or invalidation process.

When session stickiness is used another optional special attribute called `#:ownerNode` is stored in session.
It contains the name of the application server node that was the last one to touch the session.
The default behaviour is to store host name of the node,
but it can also be specified using `com.amadeus.session.node` system property.

#### Single instance mode

In single instance mode all containers connect to a single Redis instance.

Single instance mode is activated by specifying `mode=SINGLE` in the provider configuration string,
or by specifying the system property `com.amadeus.session.redis.mode=SINGLE`.

#### Sentinel mode

In sentinel instance mode all containers connect to sentinel nodes and obtain
the current Redis master node from them.

Sentinel mode is activated by specifying `mode=SENTINEL` in the provider
configuration string, or by the specifying system property
`com.amadeus.session.redis.mode=SENTINEL`.
The name of the master can be specified using `com.amadeus.session.redis.master`.

Node addresses are configured by specifying either sentinel node
names or IP addresses.
Any DNS name in the configuration is resolved to all mapped IP addresses.
This, for example, allows using Kubernetes services to get all sentinel nodes.

#### Cluster mode

In cluster instance mode all containers connect to cluster nodes and obtain current the Redis nodes from them.

Cluster mode is activated by specifying `mode=CLUSTER` in provider
configuration string, or by specifying the system property `com.amadeus.session.redis.mode=CLUSTER`.

Cluster mode is configured by specifying cluster node names or IP addresses.
Any DNS name in the configuration is resolved to all mapped IP addresses.
This, for example, allows using Kubernetes services to get all cluster nodes.

The data for a single session is stored on a single Redis node using the hash
tags in the key name (i.e. session is put in braces in key {33fdd1b6-b496-4b33-9f7d-df96679d32fe}).

Due to characteristics of the Redis cluster, the update of data is not done in atomic mode.

#### Redis Configuration

The redis repository can be configured using either a repository configuration
string which is specified using `com.amadeus.session.repository.conf` servlet initialization parameter,
system property or through agent arguments or through the following system properties.

* `com.amadeus.session.redis.host`: The ip address or DNS name of Redis server.
   For Sentinel and Cluster modes it can be slash (`/`) separated list of servers.
   If it is a DNS name, DNS resolution will retrieve all associated IP addresses.
   A port can be provided as part of the address and is separated by commas.
   Default value is `localhost`.

* `com.amadeus.session.redis.port`: The port Redis server(s) listen to.
  May be overridden in host configuration.
  The default value is `6379`.

* `com.amadeus.session.redis.master`: Name of Redis master in Sentinel mode.
  Default is `com.amadeus.session`.

* `com.amadeus.session.redis.mode` or `mode`: Specifies Redis mode.
  Can be one of `SINGLE`, `SENTINEL` or `CLUSTER`. Default value is `SINGLE`.

* Size of Jedis pool.
  As redis connections are blocking, we create a pool of Redis connections to
  allow multiple parallel connections to the Redis server.
  By default set to `100`.
  Defined using the `com.amadeus.session.redis.pool` system property or servlet
  initialization parameter, or using the pool configuration parameter.

* `com.amadeus.session.redis.expiration`: Specifies the expiration strategy.
  Can be `NOTIF` or `ZRANGE`. See below for explanation. Default is `NOTIF`.

#### Session expiration

An expiration is associated with each session using the
[`EXPIRE` command](http://redis.io/commands/expire) based upon the
`maxInactiveInterval` plus 5 minutes.
For example:

```redis
EXPIRE com.amadeus.session:webapp-namespace:{33fdd1b6-b496-4b33-9f7d-df96679d32fe} 2100
```

The expiration that is set is 5 minutes after the session actually expires.
This is necessary so that the value of the session can be accessed when the
session expires.
An expiration is set on the session itself five minutes after it actually
expires to ensure it is cleaned up, but only after we can perform
any necessary processing.
On the other hand, putting expiration assures that data will be removed from
the store even if no clean-up has been performed.

**NOTE:** The session management ensures that expired
sessions are not returned to an application.
This means there is no need to check the expiration before using a session.

**NOTE:** For safety reasons, many session management objects expire 5 minutes
after the time instant when the session expires.
This is a hardcoded value and is meant as a safety margin to complete all necessary processing.

#### Notification expiration strategy

This strategy relies on the expired
[keyspace notifications](http://redis.io/topics/notifications)
from Redis to clean-up and delete expired sessions.

Expiration is not tracked directly on the session key itself since this would
mean the session data would no longer be available.
Instead, a special session expiration key is used. In our example the expiration key is:

```redis
SETEX com.amadeus.session:expire:webapp-namespace:{33fdd1b6-b496-4b33-9f7d-df96679d32fe} 1800 ""
```

When this key expires, a keyspace notification event triggers a
lookup for the actual session and if it exists the session removal
starts.

One problem with relying on Redis expiration exclusively is that Redis makes
no guarantee of when the expired event will be fired if the key has not been
accessed. Specifically the background task that Redis uses to clean up
expired keys is a low priority task and may not trigger the key expiration.
For additional details see the [Timing of expired events](http://redis.io/topics/notifications)
section in the Redis documentation. 

##### Missed expire events

To circumvent the fact that expired events are not guaranteed to happen we
can ensure that each key is accessed when it is expected to expire. This
means that if the TTL is expired on the key, Redis will remove the key and
fire the expired event when we try to access the key.

For this reason, each session expiration is also tracked to the nearest
minute. This allows a background task to access the potentially expired
sessions to ensure that Redis expired events are fired in a more
deterministic fashion.
For example:

```redis
SADD com.amadeus.session:expirations:1439245070 33fdd1b6-b496-4b33-9f7d-df96679d32fe
EXPIREAT com.amadeus.session:expirations1439245070 1439245370
```

The expiration of this key is set 5 minutes after the minute it actually expires.
The background task will then perform following Redis operations:

```redis
SMEMBERS com.amadeus.session:expirations:1439245070
DEL com.amadeus.session:expirations:1439245070
```

From Redis 3.2, a `SPOP` command can be used instead of the above block. When used
in non-cluster mode, the block is wrapped in `MULTI/EXEC` sequence.
In this case, each node retrieves up to 1000 members until all are exhausted, and
then a `DEL` command is issued.

This `SMEMBERS` command returns the list of sessions for which to explicitly
request session expires key (using `EXISTS`). By accessing the key, rather than deleting it,
we ensure that Redis deletes the key for us only if the TTL is expired.

##### Session access

On each access to session, in this strategy we will remove session key from previous access
`expirations` set using a `SREM` command and store it in a new `expirations` set. In our
example if the session is subsequently accessed and its minute of expirations is 1439245080,
we would send the equivalent of the following commands:

```redis
SREM com.amadeus.session:expirations:1439245070 33fdd1b6-b496-4b33-9f7d-df96679d32fe
SADD com.amadeus.session:expirations:1439245080 33fdd1b6-b496-4b33-9f7d-df96679d32fe
EXPIREAT com.amadeus.session:expirations:1439245080 1439245380000

EXPIRE com.amadeus.session:webapp-namespace:{33fdd1b6-b496-4b33-9f7d-df96679d32fe} 2100
SETEX com.amadeus.session:expire:webapp-namespace:{33fdd1b6-b496-4b33-9f7d-df96679d32fe} 1800 ""
```

##### Sequence diagram of the notification expiration strategy

![Notification expiration strategy sequence diagram](https://www.websequencediagrams.com/cgi-bin/cdraw?lz=dGl0bGUgTm90aWZpY2F0aW9uIGV4cGlyAAUGc3RyYXRlZ3kKCkV2ZW50LT5KYXZhQ2xpZW50OiBDcmVhdGUgc2Vzc2lvbgoAEQotPgANB0lkABURYWN0aXYALAtJZAoKbm90ZSByaWdodCBvZiAAVwwKICBtYXhJbgAxBWVJbnRlcnZhbCA9IDE4MDBzICAgIAAMF0RlbGF5ID0AIQYrIDUgbWluID0gMjEwMCAKZW5kIG5vdGUKAIEcGEVYUElSRQCBGQoAMwUAIA4AgiAFZVMAgV4KU0VURVgAgjYGZToADwkAgSQFICIiAIFuCgAqDwCBXx9leHA9dW5peFRpbWUrAIF4Ez0xNDM5MjQ1MDcwACgHAIFzByBleHAgKyA1AIFyBgAfBzM3MCAAgWcYAINsCnMASgo6IFNBREQAhAcLczoAZgsAgzkKAIFQDgA2EABEJACCbQZBVABUGACBOAoKCgCEeRVBY2Nlc3MAhHkVAIFFFlNSRU0AgTgiZGVzdHJveQCBQhcAgiIgODA6AIIoGTgAghkoOABDJQCCJB44AII4CjgAhUgvAIU9OQoKCgphbHQAhHUIMDgwIHRpbWUgaGFzIGJlZW4gcmVhY2hlZAoJAId8BW92ZXIgAIhiBTogCgkJUmVkaXMAJwVub3QgZGV0ZWN0ZWQgdGgAhFkMCgkJV2Ugbm90aWZ5IGhpbSBtYW51YWxseQoJAIdbCgkAiSkTAIEEEACBAgkAgkYjU1BPUACDOxcKCQCDcBUtAIorDgCJcAsAYw0AiHUNSVNUUwCKGgsAghkLAIpSCwCCHwYAghcGcyB0aGF0AIIcBQCLDAcAgmUFAIkjBmQKCQCLCwkAizgOAIk8BwAgCACCRwYAjAQHCgplbHNlAFURAIJ0DgCJew8AMytlbgCLdQgAg3cFAIxPDWxlYW5pbmcgcHJvY2VzcwCLHQ4AgXcMREVMAIccEwCMUwsAixkdREVMAIsjEQCHZg4AiyALQ29udGFjdCBHaXRIdWIg&s=modern-blue)

The diagram displays a case where redis expiration (done with
  `SETEX webapp-namespace:expire:{sessionId} 1800`) was not triggered.

In this case a thread in our Java client periodically polls our expirations
keys mechanism in order to retrieve all the session ids that could have expired.
We then do a touch on these keys, which manually makes Redis aware whether these
keys have expired.
If they have, it will then push this notification to our Java client.

Please note that this diagram is without session stickiness.
Besides, we removed the prefix `com.amadeus.session` for readability.

For diagram source code, see [docs/NotificationExpirationStrategy.md](docs/NotificationExpirationStrategy.md).

#### Session stickiness

Redis session repository doesn't require session sitckiness and it can be
disabled.
Note however, that the standard behavior for Servlet containers is to have
session stickiness.
In particular if an  application handles concurrent
requests accessing same session attributes, or uses stateful session EJBs,
stickiness is needed and implemented as follows.

##### Node stickiness management

In a stickiness scenario only the node owning the session will delete session on
expire event.

When using node stickiness, the `expire` key contains also the node identifier.

```redis
SETEX com.amadeus.session:expire:node123:webapp-namespace:{33fdd1b6-b496-4b33-9f7d-df96679d32fe} 1800 ""
```

When the listener running in the JVM receives the event that this key expires, it
checks if the key prefix matches the nodes one.
If it is the case, the session will be deleted.

We also need to handle the case where the owner node doesn't receive this notification (e.g.
the node is down, there were network issues, Redis servers are busy).
For this reason we add a `forced-expirations` key that is set one minute after the `expirations` key.
It has almost the same semantics and logic, with the only difference being that the key is different
and it is set to expire one minute later.
The first JVM server receiving this message will
force expirations of all sessions present in this key.

In the above example, when the session was created, we would send also the following:

```redis
SADD com.amadeus.session:forced-expirations:1439245080 33fdd1b6-b496-4b33-9f7d-df96679d32fe
EXPIREAT com.amadeus.session:forced-expirations1439245080 1439245380000
```

When the session is subsequently accessed, again, in the above example, the following sequence is sent.

```redis
SREM com.amadeus.session:forced-expirations:1439245080 33fdd1b6-b496-4b33-9f7d-df96679d32fe
SADD com.amadeus.session:forced-expirations:1439245080000 33fdd1b6-b496-4b33-9f7d-df96679d32fe
EXPIREAT com.amadeus.session:forced-expirations:1439245080 1439245390
```

When a node gets the request for a session, if it is not the owner of that session,
it will additionally delete the old expire key and create new one with its own id.
This is expected to happen if a failover occurs at load-balancer in front of JEE servers.
E.g. load-balancer detects that old node is not responding and decides to send session to new one.

The session will still be valid, but it will have a new owner node.
It would look as follows:

```redis
DEL com.amadeus.session:expire:OLDNODEID:webapp-namespace:{33fdd1b6-b496-4b33-9f7d-df96679d32fe}
SETEX com.amadeus.session:expire:NEWNODEID:webapp-namespace:{33fdd1b6-b496-4b33-9f7d-df96679d32fe} 1800 ""
```

##### Sessions that do not expire

If the session never expires, there is no corresponding `expire` key and it is not stored in
`expirations` or `forced-expirations` sets. The primary key of the session is marked as persisted
using [`PERSIST` command](http://redis.io/commands/persist):

```redis
PERSIST com.amadeus.session:{33fdd1b6-b496-4b33-9f7d-df96679d32fe}
```

##### Network traffic and JVM processing remarks

Each session that expires will geneate message to every JVM running the web application
(JVM node). Each node will check the incoming message, and in case of sticky
sessions, only the nodes owner of the session reacts to this mesage. When using
non-sticky sessions, all JVM nodes will try to get information about the session,
but only the first JVM node that gets the message will process it.

Each clean-up key (i.e. `expirations` or `forced-expirations`) will generate one
message to each JVM node every minute when there are sessions that expire in
the previous minute.
Each JVM node will try to process the message, but only the first
one will actually get the content and process it.

#### ZRange expiration strategy

This strategy relies on the [Redis Sorted Set](http://redis.io/topics/data-types) aka [`ZRANGE`](http://redis.io/commands/ZRANGE).
In this case there is a single Sorted Set with a key `com.amadeus.session:all-sessions-set`. Each session id
is stored in this sorted set using its expiration instant as the score.

The background task will then perform following Redis `ZRANGEBYSCORE` operation to retrieve all expired session and
initiate deletion. For example, at the instant 1439246090000, we would use

```redis
ZRANGEBYSCORE com.amadeus.session:all-sessions-set 0 1439246090000
```

##### Session stickiness

When using ZRANGE strategy with session stickiness, we store owner node of each session.
The background task will first retreive all sessions from last 5 minutes and expire only ones
belong to the node in question. 

After this, the background task will load all sessions that expired until 5 
minutes before now. It is assumed that all of those sessions no longer have
legitimate owner, as otherwise, the owner node would have time to expire them. 
Consequently, they can be removed by any node, and first node that removes
their key from sorted set will be the one that will exipre them.

![Sorted set expiration strategy sequence diagram](https://www.websequencediagrams.com/cgi-bin/cdraw?lz=dGl0bGUgU29ydGVkIHNldCBleHBpcmF0aW9uIHN0cmF0ZWd5CgpFdmVudC0-SmF2YUNsaWVudDogQ3JlYXRlIHNlc3Npb24KABEKLT4ADQdJZAAVEWFjdGl2ACwLSWQKCm5vdGUgcmlnaHQgb2YgAFcMCiAgbWF4SW4AMQVlSW50ZXJ2YWwgPSAxODAwcwAIFkRlbGF5ID0AHQUgKyA1IG1pbiA9IDIxMDBzIAplbmQgbm90ZQoAgRgYRVhQSVJFAIEVCgA0BQAgDmFsbC0AgW8Hcy1zZXQ6IFpBREQgAAcQAIFZCjpub2RlIG5vdygpKwCBPRMAggsKAEoQCgoAgmUVQWNjZXNzAIJxCQA_XgCBei4KCmFsdCBldmVyeSA2MCBzCgoJAIQoE3J1bgCEYQZlIHRhc2sKCQCCQB9SQU5HRUJZU0NPUkUAglYSAIJUBS01bWluAIJeBgoJAIRNBW92ZXIAhEENSWYgAIMEBT09IHRoaXMACAUAWSJFTQCDPxsKAFEbADYFd2FzIHN1AIMrBWZ1bACBWw4AhkEMZGVsZQCGCQ0AgS0XUmV0cmlldmUgb2xkAIZzCHMAggE_MACCNwsAd4EZCmVuAIgjCACDSBEAgi0QIHByb2Nlc3MAh1cOAIcFCyBERUwAiHcLZGVzdHJveQCJCAwAiUIMAIUkBgCDayE&s=modern-blue)

For diagram source code, see [docs/SortedSetExpirationStrategy.md](docs/SortedSetExpirationStrategy.md).

## Session Encryption

See [docs/ENCRYPTION.md](docs/ENCRYPTION.md).

## Thread pools

The session support system manages two thread pools:

* one for executing long running or blocking tasks.
* one for executing scheduled tasks (at a given moment in future).

If the JEE container supports it, threads are obtained using the managed thread factory using the default JNDI
name `java:comp/DefaultManagedThreadFactory`). If the application is not running in container, or if the JEE container
doesn't support managed thread factories, threads are created using `Executors.defaultThreadFactory()`.

## Logging and Monitoring

### Logging

The logging is based on slf4j framework. Like other dependencies, this one is
shaded and doesn't interfere with applications that use slf4j themselves.

The implementation behaves differently from standard slf4j framework, as it is
able to delegate logging to any one of the following frameworks in order
of precedence:

* log4j (if `org.apache.log4j.Logger` class is present in class loader)
* commons logging (if `org.apache.commons.logging.Log` class is present in class loader)
* JDK4 logging
* simple logging to `stderr`

In case of log4j logging, the logging mechanism also adds a field containing
session id to the Mapped Diagnostic Contex (MDC) implementation. The added field
can be added to the output of the logger. By default, the name of the field is
`JSESSIONID`, but it can be configured using system properties:

* `com.amadeus.session.logging.mdc.enabled` activates session id to MDC. Default value is true.
* `com.amadeus.session.logging.mdc.name` specifies the name of MDC field. Default value is `JSESSIONID`.

### Monitoring via JMX

All metrics are exposed as JMX beans under `metrics.session.NAMESPACE` where
NAMESPACE is either the context path of the JEE web application or the namespace
configured via `com.amadeus.session.namespace`.

#### Session metrics

* `com.amadeus.session.created` measures the total number of created sessions as well as rate of sessions created in last 1, 5 and 15 minutes.
* `com.amadeus.session.deleted` measures the total number of deleted sessions as well as rate of sessions measures rate of sessions deleted in the last 1, 5 and 15 minutes.
* `com.amadeus.session.missing` measures the total number of session which were not found in repository and also measures rate of such occurrences in last 1, 5 and 15 minutes.
* `com.amadeus.session.retrieved` measures the total number of session retrievals as well as the rate of sessions retrieval from store in last 1, 5 and 15 minutes.
* `com.amadeus.session.timer.commit` measures the histogram (distribution) of the  elapsed time during commit as well as the total number of commits and rate of commits over the last 1, 5 and 15 minutes.
* `com.amadeus.session.timer.fetch` measures the histogram (distribution) of elapsed time during fetches of session data from the repository as well as the total number of fetch requests and rate of fetch requests over the last 1, 5 and 15 minutes.
* `com.amadeus.session.serialized.bytes` measures the amount of data that was serialized to be sent.
* `com.amadeus.session.serialized.distribution` measures the statistical information about data that was serialized in last 5 minutes.
* `com.amadeus.session.deserialized.bytes` measures the amount of data that was deserialized when received.
* `com.amadeus.session.deserialized.distribution` measures the statistical information about data that was deserialized in last 5 minutes.
* `com.amadeus.session.invalidation.errors.expiry` measures the total number of invalidation errors.
* `com.amadeus.session.invalidation.errors` measures the total number of invalidation on expiry errors.

Total number of active sessions is the total number of created sessions on all
nodes minus total number of deleted sessions on all nodes.

#### Timings

* `com.amadeus.session.timers.delete-async` measures the histogram (distribution) of elapsed time during deletes of sessions as well as the total number of deletions and the rate of deletions over the last 1, 5 and 15 minutes.
* `com.amadeus.session.timers.redis.expiration-cleanup` measures the histogram (distribution) of elapsed time during expiration cleanup of sessions stored in Redis as well as the total number of expiration cleanup invocations and the rate over the last 1, 5 and 15 minutes.
* `com.amadeus.session.timers.redis.forced-cleanup` measures the histogram (distribution) of elapsed time during forced cleanup of sessions stored in redis as well as the total number of expiration cleanup invocations and the rate over the last 1, 5 and 15 minutes. Forced cleanup is used with session stickiness.
* `com.amadeus.session.timers.in-memory-cleanup` measures the histogram (distribution) of elapsed time during expiration cleanup of sessions stored in memory as well as the total number of  expiration cleanup invocations and the rate over the last 1, 5 and 15 minutes.

#### Thread pool monitoring

For thread pools of blocking/long running tasks the library exposes following metrics:

* `com.amadeus.session.threads.active`: Number of running tasks.
* `com.amadeus.session.threads.largest`: The largest recorded size of pool.
* `com.amadeus.session.threads.pool`: Current size of pool.
* `com.amadeus.session.threads.waiting`: Number of tasks waiting in queue.

For thread pools of scheduled tasks the library exposes following metrics:

* `com.amadeus.session.scheduled-threads.active`: Number of running tasks.
* `com.amadeus.session.scheduled-threads.largest`: The largest recorded size of pool.
* `com.amadeus.session.scheduled-threads.pool`: Current size of pool.
* `com.amadeus.session.scheduled-threads.waiting`: Number of tasks waiting in the queue.
* `com.amadeus.session.scheduled-threads.tasks`: Approximate total number of tasks that have been scheduled.

#### Redis monitoring

In the following metrics, HOST corresponds to Redis host.

* `com.amadeus.session.redis.HOST.active`: Number of active Redis connections.
* `com.amadeus.session.redis.HOST.idle`: Number of idle Redis connections.
* `com.amadeus.session.redis.HOST.waiting`: Number of Redis requests waiting for connection.
* `com.amadeus.session.redis.failover`: When using sticky sessions, number of failovers (session retrieval from different node) that occurred.

## Classpath and dependency notes

The project depends on following external projects

* [ASM](http://asm.ow2.org/)
* [Jedis](https://github.com/xetorthio/jedis)
* [Codahale Metrics](http://metrics.dropwizard.io)
* [slf4j](http://www.slf4j.org)

During build, the dependencies are 'shaded' using the maven shade plugin. The packages are moved
into `com.amadeus.session.shaded` hierarchy and as such are not visible under their default
packages.
