# Building the project

Project metrics are available at:
[SonarQube](http://rndwww.nce.amadeus.net/sonar/dashboard/index?did=1&id=com.amadeus.proto%3Asession%3Ascsat-com.amadeus.session-master)

The project uses maven build and builds simply using:

```sh
mvn package
```

## Integration Tests

Integration tests are placed in classes that start with `IT*` (e.g. `ITSessionExpire`).
Most of the integration tests require activation of specific profiles.

```sh
mvn verify -P...
```

### Agent Tests

Agent integration tests are placed in `agent-tests` project. They are run in separate
JVM with agent activated.

### Container Tests

Container testing is using arquillian framework. Same battery of tests can be run against
several servlet containers. All tests are part of `session-replacement` project. In this
project, coverage report can be activated using `coverage-per-test` profile.
Following paragraphs describe integration testing with specific containers.

#### Jetty 9

The tests run on an embedded container.
The testing is invoked using

```sh
mvn verify -Pjetty-9-embedded-arquillian,arquillian-tests
```

#### Tomcat 7

The tests run on an embedded container.
The testing is invoked using

```sh
mvn verify -Ptomcat-7-embedded-arquillian,arquillian-tests
```

#### Wildfly 10

Wildfly is started as a managed server.
The testing is invoked using

```sh
mvn verify -Pwildfly-10-managed-arquillian,arquillian-tests
```

```sh
mvn verify -Pwildfly-10-managed-arquillian-with-agent,external-arquillian-tests
```

### Redis Tests

Redis tests require docker installation. This can be either native docker on Linux machines
or Docker Toolbox installation on OSX or Windows. The tests use docker-mavan-plugin from
fabric8.io. Redis tests are activated using specific profiles. All tests are
part of `session-replacement` project. In this project, coverage report can be activated
using `coverage-per-test` profile.

By default tests use version 3.2 of Redis running on Alpine Linux.
This can be changed using system properties `redis.version` and `redis.image`.
For example to test with Redis 3.0 you can use any of the following:

```sh
# Tests 3.0 on Alpine Linux
mvn verify -Predis-single, -Dredis.version=3.0
# Tests 3.0 on default image (Ubuntu)
mvn verify -Predis-single, -Dredis.image=readis:3.0
```

By default tests use NOTIF expiration strategy, but this can be configured using
`it.com.amadeus.session.redis.expiration` system property. See expiration
strategies for more information.

```sh
mvn verify -Predis-single, -Dit.com.amadeus.session.redis.expiration=ZRANGE
```

#### Redis Single Server Tests

In this tests, a battery of tests is run against single Redis server. This server is started
as Redis docker container that listens on dynamically assigned port.

```sh
mvn verify -Predis-single
```

#### Redis Sentinel Tests

In this tests, a battery of tests is run against Redis sentinel configuration consisting
of master, slave and three sentinels. This servers are started as Redis docker containers that
listens on predefined ports exposed on as docker host. The machines use docker host networking
to see each other. These tests require setting of a system property `redis.host` that should
point to exposed IP of the docker host.

```sh
mvn verify -Predis-sentinel -Dredis.host=192.168.99.100
```

## Creating a release

Verify that builds and test pass.

Set new version for the release.

```sh
mvn versions:set -DnewVersion=0.4.2
```

Perform build with release profile to add sources and javadoc. Perform deploy to OSS Sonatype Nexus:

```sh
mvn clean install
mvn deploy -Prelease
```

If release is successful, commit the version and commit and tag changes in git.

```sh
mvn versions:commit
git add *
git commit -m "version 0.4.2"
git tag -a v0.4.2 -m "version 0.4.2"
git push origin --tags
```

Set version back to snapshot and commit changes. 

```sh
mvn versions:set -DnewVersion=0.4-SNAPSHOT
```

