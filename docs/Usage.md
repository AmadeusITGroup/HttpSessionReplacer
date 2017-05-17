# Using session replacement in a web application

To use session replacement in a web application, we can add session replacement library in
the `WEB-INF/lib` directory of the web application. In order to be sure that the session
is replaced before any filters are triggered, add `SesssionFilter` as the first one
in pipeline of filters.

Note that when using this method, there is no support for many standard `HttpSession` features.
Most notably, `HttpSessionListener` support is missing.

## Using session replacement in a container

We can add session replacement to all web applications running in a servlet container. To do this,
container JVM has to be started with the session replacement agent, and session replacement library
must be available in the classpath of each web application. Following paragraphs explain
how to do it in many popular application servers.

This method enables all features of session replacement library.

In following examples, it is assumed that SESSION_PATH points to directory where session replacement
distribution archive was unpacked.

## Wildfly 8.x, 9.x and 10.x or JBoss EAP 6.x and 7.x

Agent can be added as via `JAVA_OPTS` environment variable.

```sh
export JAVA_OPTS=-javagent:SESSION_PATH/session-agent.jar
# on Windows: set JAVA_OPTS=-javagent:SESSION_PATH/session-agent.jar
```

Standard way of doing this is to modify the bootstrap script configuration file. When using
standalone script, the default name of that file is `standalone.conf` when using shell, or,
on Windows, it is `standalone.conf.bat` or `standalone.conf.ps1`. Different file can be specified
using `RUN_CONF` environment variable.

In JBoss, the classes loaded in system class path are not available to web application unless
they are explicitly published. For example `java.*` JDK classes are explicitly published, but
`com.amadeus.session` are not. There are several ways of making them available, and the
chosen approach is to use global module called `com.amadeus.session-replacement`.

As the library needs to be installed as a global Wildlfy/JBoss module, we suggest one of
the following options.

The global support is enabled in jboss configuration file:

### Option 1: Install module inside wildfly installation

Unpack the module archive in the root of your Wildfly installation. This will create a module
at following location `modules/system/layers/base/com/amadeus/session`.

Modify `standalone.xml` to load requested module as global module. E.g.

```xml
<subsystem xmlns="urn:jboss:domain:ee:1.2" >
  <global-modules>
    <module name="com.amadeus.session" slot="main" />
  </global-modules>
</subsystem>
```

If you want to activate this feature on per-application basis, you should use
`jboss-deployment-structure.xml` mechanism:

```xml
<jboss-deployment-structure>
  <deployment>
    <dependencies>
      <module name="com.amadeus.session" />
    </dependencies>
  </deployment>
</jboss-deployment-structure>
```

### Option 2: Use module placed in different directory

You may unpack the module in the location of your choice. Assuming `JBOSS_HOME` is set to installation
directory of your Wildfly/JBoss server, and that `SESSION_PATH` is location where you unpacked the module,
start Wildfly/JBoss using

```sh
$JBOSS_HOME/bin/standalone.sh -mp $JBOSS_HOME/modules:$SESSION_PATH/modules

# on Windows: %JBOSS_HOME%\bin\standalone.bat -mp %JBOSS_HOME%\modules;%SESSION_PATH%\modules
# on Windows: $JBOSS_HOME\bin\standalone.ps1 -mp $JBOSS_HOME\modules;$SESSION_PATH\modules
```

### URL session propagation

URL session propagation is _not_ supported in JBoss 6.x. It works in versions 7.x and in all Wildfly verions.

### Session replacement configuration in Wildfly/JBoss configuration file

TODO explain configuration using system properties in standalone.xml

## Jetty 9.x

The session replacement library can be added to classpath using standard `CLASSPATH` environment
variable or it can be added via command line option `--lib=SESSION_PATH/session-replacement-shaded.jar`.

Agent can be added as a command line option or, when using `jetty.sh` script, via `JAVA_OPTS`
environment variable.

```sh
export CLASSPATH=$CLASSPATH:SESSION_PATH/session-replacement-shaded.jar
# on Windows: set CLASSPATH=%CLASSPATH%;SESSION_PATH/session-replacement-shaded.jar

java -javagent:SESSION_PATH/session-agent.jar -jar ../start.jar
java -javagent:SESSION_PATH/session-agent.jar -jar ../start.jar --lib=SESSION_PATH/session-replacement-shaded.jar

export JAVA_OPTS=-javagent:SESSION_PATH/session-agent.jar
bin/jetty.sh --lib=SESSION_PATH/session-replacement-shaded.jar
```

### Note about using `jetty.sh`

If the `JAVA_OPTS` variable is specified in one of the following locations it will be loaded
automatically by the script: `/etc/default/jetty`, `/etc/jetty` or `~/.jettyrc`.

If the script `jetty.sh` was renamed, the name of configuration files change to file name part without
extension of the renamed script. E.g. if script was renamed to `myjetty.sh`, the name of the
configuration file is `/etc/default/myjetty`, `/etc/myjetty` or `~/.myjettyrc`

## Tomcat 6.x and 7.x

In following instructions `CATALINA_HOME` represents location where Tomcat is installed, and
`CATALINA_BASE` represents base location of your server. By default they are same, but you may
override the values using respective environment variables.

To start agent, set or add to environment variable `CATALINA_OPTS`. For example:

```sh
CATALINA_OPTS=-javagent:SESSION_PATH/session-agent.jar
```

You may add this variable into `setenv.sh` (or *nix) or `setenv.bat` (on Windows) files located either
in `CATALINA_BASE/bin` or `CATALINA_HOME/bin` directories.

You may add session replacement library to class path in several ways.

### Option 1: Copy the library into common directory

Copy the session-replacement-shaded.jar into `CATALINA_HOME/lib` directory. This method assumes
that you haven't modified `common.loader` line in `CATALINA_BASE/conf/catalina.properties`.

### Option 2: Modify conf/catalina.properties to point to library

Copy the library to directory of your choice. Modify configuration of common
class loader (`common.loader`) to point to directory where the library is located.
For example:

```properties
common.loader=${catalina.base}/lib,${catalina.base}/lib/*.jar,${catalina.home}/lib,${catalina.home}/lib/*.jar,SESSION_PATH/*.jar
```

### Having session replacement present in your webapp

If you have session replacement present in your webapp class path (WEB-INF/lib), and if the
common library is present, the one in webapp this library has precedence
over the common one, if the common one is present. This may generate issues at runtime. Usually this
manifested with class cast exceptions. It is recommended to give preference to the common classes. See
[Tomcat Class Loader](https://tomcat.apache.org/tomcat-7.0-doc/class-loader-howto.html) for details.

### Note regarding Tomcat 6.x

Tomcat 6.x supports Servlet 2.5 specification. This environment has few limitations and prerequisites:

* your web application must have at least one `Filter`, otherwise there will be no session replacement.
* if you can modify your web application, add `com.amadeus.session.servlet.SessionFilter` as first filter of your web application.
* to support sending events to `HttpSessionListener` and `HttpSessionAttributeListener`, start the agent with `session=2.x` argument or set system property `com.amadeus.session.servlet.api=2.x`
* URL session propagation is _not_ supported in Tomcat 6.x.

### Usage with Spring library

The library is fully compatible with Spring library, and there is no need additional for any special configuration. Note, however, that you can't use Spring Session and Http Session Replacement at the same time.

## Redis configuration

### Session replacement agent configuration

### Session replacement servlet context configuration

### Using NOTIF expiration strategy

When using NOTIF expiration strategy, redis needs to be started with expiration events activated.
That can be done either in command line:

```sh
redis --notify-keyspace-events Ex
```

or in redis.conf:

```
notify-keyspace-events Ex
```

For more details, see [redis keyspace notifications](http://redis.io/topics/notifications).

### Single Redis Instance

### Redis Sentinel Configuration

See [redis sentinel](http://redis.io/topics/sentinel) for more information. It is recommended to
have at least 3 sentinels and one or two slaves for the master.

### Redis Cluster Configuration

See [redis cluster tutorial](http://redis.io/topics/cluster-tutorial) for more information.

When using redis cluster mode, if some of hash slots are not covered (i.e. when
one master goes down), other masters will be replying with CLUSTERDOWN error 
until all slots are covered (e.g. new master is elected). This will result in 
full outage of the session storage until everything is fine. Fortunately, redis 
cluster can be configured to give results even if there is no full coverage.
When using NOTIF expiration strategy, all the related keys are stored on the 
same Redis instance and we suggest to use this feature as it allows higher 
availability.

```
# By default Redis Cluster nodes stop accepting queries if they detect there
# is at least an hash slot uncovered (no available node is serving it).
# This way if the cluster is partially down (for example a range of hash slots
# are no longer covered) all the cluster becomes, eventually, unavailable.
# It automatically returns available as soon as all the slots are covered again.
#
# However sometimes you want the subset of the cluster which is working,
# to continue to accept queries for the part of the key space that is still
# covered. In order to do so, just set the cluster-require-full-coverage
# option to no.
#
cluster-require-full-coverage no
```

