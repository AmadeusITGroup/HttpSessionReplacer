# Modifying Agent Byte Code Injection

Some of the more complex byte code injected by agent corresponds to methods 
present in file HelpersToInject. Notably following table shows mapping between injector class
and equivalent methods being injected.

* Class CommonHelpers injects into each filter the methods present in HelpersToInject outer class. These
methods are used by other injectors
* Class FilterHelpers injects into each filter the methods present in HelpersToInject.FilterHelpers inner class. These
methods are used to prepare request and return current ServletContext 
* Class ListenerHelpers injects into each filter the methods present in HelpersToInject.ListenerHelpers inner class. These
methods are used to detect different session listeners in Servlet 2.5 application containers 
* Class ServletContextHelpers injects into each filter the methods present in HelpersToInject.ServletContextHelpers inner class. These
methods are used to detect different session listeners in Servlet 3+ application containers 

In addition to this, agent renames doFilter in all servlet Filters and injects following method into Filters 

```java
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
      ServletRequest oldRequest = request;
      request = SessionHelpers.prepareRequest(oldRequest, response, $$injected_servletContext);
      response = SessionHelpers.prepareResponse(request, response, $$injected_servletContext);
      try {
        $$renamed_doFilter(request, response, chain);
      } finally {
        SessionHelpers.commitRequest(request, oldRequest, $$injected_servletContext);
      }
    }

```

TODO explain logic to invoke super class filter