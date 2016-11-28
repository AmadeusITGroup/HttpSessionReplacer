# Repository Plug-ins

The session replacement library allows use of different session repository implementations
via a plug-in mechanism.

## Built-in Plugins

The default implementation comes with several built-in plugins.
The built-in plugins can be referenced by their short name. The supported
built-in plugins are:

* in-memory
* redis

## Developing plug-ins

Additional plugins can be developed and used by the session manager.
The extension plug-in classes must be visible in the web application classloader.

### `SessionRepository`

This is the main interface for repository implementation. The implementation
is responsible for all interactions between session managment and the underlying
repository.

### `SessionRepositoryFactory`

Implementations of this class create instances of `SessionRepository` based
on the given configuration.

In addition this class should inform the framework if it enforces distribution
of sessions (i.e. offloading to different processes or machines).

### `SessionConfiguration`

The `SessionConfiguration` class provides the main configuration parameters for
session management. In addition to pre-defined parameters, it allows the use
of custom parameters provided either via system properties, via another
key-value store (e.g. servlet initalization parameters) or by explicitly
setting them using the `setAttribute` method. Default priority is:


* parameter specified using `setAttribute` method
* servlet initialization parameters
* system properties

### Plugin Contract