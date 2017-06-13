package com.amadeus.session.servlet;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import com.amadeus.session.ResponseWithSessionId;

/**
 * Allows ensuring that the session is propagated if the response is committed. Each of the methods that can commit
 * response will invoke {@link #propagateSession()} method to insure that session id has been propagated.
 */
class HttpResponseWrapper extends HttpServletResponseWrapper implements ResponseWithSessionId {
  private static final int LN_LENGTH = System.getProperty("line.separator").length();
  private static final byte[] CRLF = new byte[] { '\r', '\n' };

  private final HttpRequestWrapper request;
  private PrintWriter writer;
  private long contentWritten;
  protected long contentLength;
  private SaveSessionServletOutputStream outputStream;
  private final boolean delegatePrintWriter;

  /**
   * Default constructor
   *
   * @param request
   *          the wrapped request
   * @param response
   *          the response to be wrapped
   */
  HttpResponseWrapper(HttpRequestWrapper request, HttpServletResponse response) {
    super(response);
    delegatePrintWriter = request.getManager().getConfiguration().isDelegateWriter();

    this.request = request;
  }

  @Override
  public final void sendError(int sc) throws IOException {
    request.propagateSession();
    super.sendError(sc);
  }

  @Override
  public final void sendError(int sc, String msg) throws IOException {
    request.propagateSession();
    super.sendError(sc, msg);
    closeOutput();
  }

  @Override
  public final void sendRedirect(String location) throws IOException {
    request.propagateSession();
    super.sendRedirect(location);
    closeOutput();
  }

  @Override
  public SaveSessionServletOutputStream getOutputStream() throws IOException {
    if (writer != null) {
      throw new IllegalStateException("Only one of getWriter()/getOutputStream() can be called, and writer is already used.");
    }
    if (outputStream == null) {
      outputStream = wrapOutputStream(super.getOutputStream());
    }
    return outputStream;
  }

  @Override
  public PrintWriter getWriter() throws IOException {
    if (outputStream != null) {
      throw new IllegalStateException("Only one of getWriter()/getOutputStream() can be called, and output stream is already used.");
    }
    if (writer == null) {
      writer = wrapPrintWriter();
    }
    return writer;
  }

  /**
   * Wraps output stream into one that is capable of tracking when we need to write headers and commit session.
   *
   * @param servletOutputStream
   * @return
   * @throws IOException
   */
  protected SaveSessionServletOutputStream wrapOutputStream(ServletOutputStream servletOutputStream) throws IOException {
    return new SaveSessionServletOutputStream(servletOutputStream);
  }

  /**
   * Creates print writer that is capable of tracking when we need to write
   * headers and commit session.
   * 
   * If system property <code>com.amadeus.session.delegate.printwriter</code> is set
   * to <code>true</code>, we rely on containers implementation of PrintWriter.
   *
   * @return writer that tracks when we need to write headers and commit session
   * @throws IOException
   *           if an exception occurred during stream or writer creation
   */
  private PrintWriter wrapPrintWriter() throws IOException {
    String encoding = getCharacterEncoding();
    if (encoding == null) {
      // Using default coding as per Servlet standard
      encoding = "ISO-8859-1";
      setCharacterEncoding(encoding);
    }
    if (delegatePrintWriter) {
      return new DelegateServletPrintWriter(super.getWriter());
    }
    SaveSessionServletOutputStream wrappedStream = wrapOutputStream(super.getOutputStream());
    OutputStreamWriter osw = new OutputStreamWriter(wrappedStream, encoding);
    SimplestServletPrintWriter myWriter = new SimplestServletPrintWriter(osw);
    wrappedStream.setAssociated(myWriter);
    return myWriter;
  }

  @Override
  public void addHeader(String name, String value) {
    checkContentLenghtHeader(name, value);
    super.addHeader(name, value);
  }

  void checkContentLenghtHeader(String name, String value) {
    // If added header is Content-Length, we need to use its value
    // as new contentLength.
    if ("content-length".equalsIgnoreCase(name)) {
      contentLength = Long.parseLong(value);
    }
  }

  @Override
  public void setHeader(String name, String value) {
    checkContentLenghtHeader(name, value);
    super.setHeader(name, value);
  }

  @Override
  public void setContentLength(int len) {
    contentLength = len;
    super.setContentLength(len);
  }

  /**
   * Adds the contentLengthToWrite to the total contentWritten size and checks to see if the response should be written.
   *
   * @param contentLengthToWrite
   *          the size of the content that is about to be written.
   * @throws IOException
   *           if there was an error during flush
   */
  private void checkContentLength(int contentLengthToWrite) {
    this.contentWritten += contentLengthToWrite;
    boolean isBodyFullyWritten = this.contentLength > 0 && this.contentWritten >= this.contentLength;
    int bufferSize = getBufferSize();
    boolean requiresFlush = bufferSize > 0 && this.contentWritten >= bufferSize;
    if (isBodyFullyWritten || requiresFlush) {
      try {
        flushAndPropagate();
      } catch (IOException ex) {
        throw new IllegalStateException(ex);
      }
    }
  }

  @Override
  public void reset() {
    super.reset();
    // If we called reset, we shouldn't assume session was propagated.
    request.removeAttribute(Attributes.SESSION_PROPAGATED);
  }
  
  @Override
  public String encodeURL(String url) {
    return request.encodeURL(url);
  }

  @Override
  public void flushBuffer() throws IOException {
    // On flush, we propagate session, then flush all buffers.
    flushAndPropagate();
    super.flushBuffer();
  }

  private void flushAndPropagate() throws IOException {
    if (outputStream != null) {
      outputStream.flush();
    } else if (writer != null) {
      writer.flush();
    } else {
      request.propagateSession();
    }
  }

  /**
   * Ensures that session is indeed committed when calling methods that commit the response. We delegate all methods to
   * the original {@link javax.servlet.ServletOutputStream} to ensure that the behavior is as close as possible to the
   * original one. To check if session needs to be committed, we are counting number of bytes written out.
   *
   * Based on Spring Session code.
   */
  class SaveSessionServletOutputStream extends ServletOutputStream {

    protected final ServletOutputStream delegate;
    private Closeable associated;
    boolean closing;

    SaveSessionServletOutputStream(ServletOutputStream delegate) {
      this.delegate = delegate;
    }

    /**
     * Sets associated {@link Closeable} object. If stream is be associated to
     * another {@link Closeable} object, when {@link #close()} method is called,
     * the associated object will be closed.
     *
     * @param associated
     *          the object to close when this stream is closed
     */
    void setAssociated(Closeable associated) {
      this.associated = associated;
    }
    
    @Override
    public void write(int b) throws IOException {
      checkContentLength(1);
      delegate.write(b);
    }

    @Override
    public void flush() throws IOException {
      request.propagateSession();
      delegate.flush();
    }

    @Override
    public void close() throws IOException {
      if (closing) {
        return;
      }
      closing = true;
      if (associated != null) {
        associated.close();
      }
      request.propagateSession();
      delegate.flush();
      delegate.close();
    }

    @Override
    public int hashCode() {
      return delegate.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      return delegate.equals(obj);
    }

    public void print(boolean b) throws IOException {
      super.print(b);
    }

    public void print(char c) throws IOException {
      print(String.valueOf(c));
    }

    public void print(int i) throws IOException {
      print(String.valueOf(i));
    }

    public void print(long l) throws IOException {
      print(String.valueOf(l));
    }

    public void print(float f) throws IOException {
      print(String.valueOf(f));
    }

    public void print(double d) throws IOException {
      print(String.valueOf(d));
    }

    public void println() throws IOException {
      write(CRLF);
    }

    public void println(String s) throws IOException {
      print(s);
      println();
    }

    public void println(boolean b) throws IOException {
      print(b);
      println();
    }

    public void println(char c) throws IOException {
      print(c);
      println();
    }

    public void println(int i) throws IOException {
      print(i);
      println();
    }

    public void println(long l) throws IOException {
      print(l);
      println();
    }

    public void println(float f) throws IOException {
      print(f);
      println();
    }

    @Override
    public void write(byte[] b) throws IOException {
      checkContentLength(b.length);
      delegate.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      checkContentLength(len);
      delegate.write(b, off, len);
    }

    @Override
    public String toString() {
      return getClass().getName() + "[delegate=" + delegate.toString() + "]";
    }

    /**
     * For servlet 3.1. We do nothing in this method as the library may run in servlet 2.x or 3.0 container. See
     * {@link HttpResponseWrapper31.SaveSessionServlet31OutputStream} for logic used in servlet 3.1 containers.
     */
    @Override
    public boolean isReady() {
      // Only for Servlet 3.1
      return true;
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {
      // Only for Servlet 3.1
    }
  }


  /**
   * Wrapper for {@link PrintWriter} that manages re-entrace of close() method.
   */
  static class SimplestServletPrintWriter extends PrintWriter {
    /**
     * Flag that says that close() method has been called.
     */
    boolean closing;

    SimplestServletPrintWriter(Writer out) {
      super(out);
    }
    
    @Override
    public void close() {
      // If close method has already been called, we will not re-enter
      // close() method of the wrapped writer.
      if (!closing) {
        closing = true;
        super.close();
      }
    }
  }

  /**
   * Wrapper for {@link PrintWriter} that relies on underlying container's PrintWriter implementation.
   */
  class DelegateServletPrintWriter extends PrintWriter {
    private final PrintWriter delegate;

    DelegateServletPrintWriter(PrintWriter delegate) {
      super(delegate);
      this.delegate = delegate;
    }

    @Override
    public void flush() {
      request.propagateSession();
      delegate.flush();
    }

    @Override
    public void close() {
      request.propagateSession();
      delegate.close();
    }

    public int hashCode() {
      return delegate.hashCode();
    }

    public boolean equals(Object obj) {
      return delegate.equals(obj);
    }

    public String toString() {
      return getClass().getName() + "[delegate=" + delegate.toString() + "]";
    }

    @Override
    public boolean checkError() {
      return delegate.checkError();
    }

    @Override
    public void write(int c) {
      String s = Character.toString((char) c);
      write(s);
    }

    @Override
    public void write(char[] buf, int off, int len) {
      checkContentLength(len);
      delegate.write(buf, off, len);
    }

    @Override
    public void write(char[] buf) {
      checkContentLength(buf.length);
      delegate.write(buf);
    }

    @Override
    public void write(String s, int off, int len) {
      checkContentLength(len);
      delegate.write(s, off, len);
    }

    public void write(final String s) {
      write(s, 0, s.length());
    }

    public void print(final boolean b) {
      write(Boolean.toString(b));
    }

    public void print(final char c) {
      write(Character.toString(c));
    }

    public void print(final int i) {
      write(Integer.toString(i));
    }

    public void print(final long l) {
      write(Long.toString(l));
    }

    public void print(final float f) {
      write(Float.toString(f));
    }

    public void print(final double d) {
      write(Double.toString(d));
    }

    public void print(final char[] s) {
      write(s);
    }

    public void print(final String s) {
      write(s == null ? "null" : s);
    }

    public void print(final Object obj) {
      write(obj == null ? "null" : obj.toString());
    }

    @Override
    public void println() {
      checkContentLength(LN_LENGTH);
      super.println();
    }
  }

  /**
   * Closes associated servlet {@link PrintWriter} and {@link ServletOutputStream}.
   *
   * @throws IOException
   *           if exception occurred on close.
   */
  private void closeOutput() throws IOException {
    if (writer != null) {
      writer.close();
    }
    if (outputStream != null) {
      outputStream.close();
    }
  }  
}
