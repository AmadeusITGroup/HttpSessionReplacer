# Notes Regarding HttpSession API

## Servlet API compatibility

The library supports servlet specification v3.1 and is backward compatible with
3.0 and 2.x versions of specifications with following exceptions:

* Session tracking modes COOKIE and URL are supported. SSL mode is not supported.
* Default cookie name is JSESSION. It can be changed to JSESSIONID.

## switchSessionId() (Servlet 3.1)

At present, for redis implementation, when session is not sticky, the session
switch may result in race conditions. If there are any concurrent requests on
different servers and one server initiates session id switch, the other servers
will not be aware of the change.

Redis repository does provides feature that allows informing other serves that the change
occurred. It will publish an event containing old and new id. However, at the moment, there
are no listeners for this event.
