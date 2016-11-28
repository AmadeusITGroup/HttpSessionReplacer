package com.amadeus.session.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.amadeus.session.servlet.HttpResponseWrapper.SaveSessionServletOutputStream;
import com.amadeus.session.servlet.HttpResponseWrapper.ServletPrintWriter;

@SuppressWarnings("javadoc")
public class TestHttpResponseWrapper {

  private HttpRequestWrapper requestWrapper;
  private HttpServletResponse response;
  private HttpResponseWrapper responseWrapper;
  private ByteArrayOutputStream outputStream;

  @Before
  public void setupWrapper() throws IOException {
    requestWrapper = mock(HttpRequestWrapper.class);
    response = mock(HttpServletResponse.class);
    responseWrapper = new HttpResponseWrapper31(requestWrapper, response);
    outputStream = new ByteArrayOutputStream();
    when(response.getOutputStream()).thenReturn(new ServletOutputStream() {
      @Override
      public void write(int b) throws IOException {
        outputStream.write(b);
      }

      @Override
      public void setWriteListener(WriteListener writeListener) {
      }

      @Override
      public boolean isReady() {
        return false;
      }
    });
  }

  @Test
  public void testPropagateOnFlushBuffer() throws IOException {
    responseWrapper.flushBuffer();
    verify(response).flushBuffer();
    verify(requestWrapper).propagateSession();
  }

  @Test
  public void testPropagateOnFlushBufferForWriter() throws IOException {
    responseWrapper.getWriter();
    responseWrapper.flushBuffer();
    verify(response).flushBuffer();
    verify(requestWrapper).propagateSession();
  }

  @Test
  public void testPropagateOnFlushBufferForStream() throws IOException {
    responseWrapper.getOutputStream();
    responseWrapper.flushBuffer();
    verify(response).flushBuffer();
    verify(requestWrapper).propagateSession();
  }

  @Test
  public void testClearResetOnReset() {
    responseWrapper.reset();
    verify(response).reset();
    verify(requestWrapper).removeAttribute(Attributes.SESSION_PROPAGATED);
  }

  @Test
  public void testPropagateOnSendErrorInt() throws IOException {
    responseWrapper.sendError(404);
    verify(response).sendError(404);
    verify(requestWrapper).propagateSession();
  }

  @Test
  public void testPropagateOnSendErrorIntString() throws IOException {
    responseWrapper.sendError(404, "test");
    verify(response).sendError(404, "test");
    verify(requestWrapper).propagateSession();
  }

  @Test
  public void testPropagateOnSendErrorAfterWriter() throws IOException {
    ServletPrintWriter writer = responseWrapper.getWriter();
    responseWrapper.sendError(404, "test");
    assertTrue(writer.closing);
  }

  @Test
  public void testPropagateOnSendErrorAfterStream() throws IOException {
    SaveSessionServletOutputStream stream = responseWrapper.getOutputStream();
    responseWrapper.sendError(404, "test");
    assertTrue(stream.closing);
  }

  @Test
  public void testPropagateOnSendRedirectString() throws IOException {
    responseWrapper.sendRedirect("test");
    verify(response).sendRedirect("test");
    verify(requestWrapper).propagateSession();
  }

  @Test
  public void testPropagateOnOutputStreamFlush() throws IOException {
    ServletOutputStream os = responseWrapper.getOutputStream();
    verify(response).getOutputStream();
    verify(requestWrapper, never()).propagateSession();
    os.flush();
    verify(requestWrapper).propagateSession();
  }

  @Test
  public void testPropagateWriterFlush() throws IOException {
    PrintWriter w = responseWrapper.getWriter();
    verify(response, never()).getWriter();
    verify(requestWrapper, never()).propagateSession();
    w.flush();
    verify(requestWrapper).propagateSession();
  }

  @Test
  public void testPropagateContentLength() throws IOException {
    responseWrapper.setContentLength(10);
    ServletOutputStream os = responseWrapper.getOutputStream();
    verify(response).getOutputStream();
    verify(requestWrapper, never()).propagateSession();
    os.println();
    verify(requestWrapper, never()).propagateSession();
    os.println("1234567890");
    verify(requestWrapper).propagateSession();
  }

  @Test
  public void testWrite() throws IOException {
    ServletOutputStream os = responseWrapper.getOutputStream();
    verify(response).getOutputStream();
    os.print("1234567890");
    assertEquals("1234567890", outputStream.toString());
    outputStream.reset();
    os.print(true);
    assertEquals("true", outputStream.toString());
    outputStream.reset();
    os.print('A');
    assertEquals("A", outputStream.toString());
    outputStream.reset();
    os.print(12.3);
    assertEquals("12.3", outputStream.toString());
    outputStream.reset();
    os.print(12.3f);
    assertEquals("12.3", outputStream.toString());
    outputStream.reset();
    os.print(12);
    assertEquals("12", outputStream.toString());
    outputStream.reset();
    os.print(12L);
    assertEquals("12", outputStream.toString());

    outputStream.reset();
    os.write(32);
    assertEquals(" ", outputStream.toString());
    outputStream.reset();
    os.write(new byte[] { 65, 66 });
    assertEquals("AB", outputStream.toString());
    outputStream.reset();
    os.write(new byte[] { 65, 66 }, 1, 1);
    assertEquals("B", outputStream.toString());

    // In some strange cases System.lineSeparator() was empty, while println was actually
    // printing something...
    outputStream.reset();
    os.println("");
    String eol = outputStream.toString();
    outputStream.reset();
    os.println("1234567890");
    assertEquals("1234567890" + eol, outputStream.toString());
    outputStream.reset();
    os.println(null);
    assertEquals("null" + eol, outputStream.toString());
    outputStream.reset();
    os.println(true);
    assertEquals("true" + eol, outputStream.toString());
    outputStream.reset();
    os.println('A');
    assertEquals("A" + eol, outputStream.toString());
    outputStream.reset();
    os.println(12.3);
    assertEquals("12.3" + eol, outputStream.toString());
    outputStream.reset();
    os.println(12.3f);
    assertEquals("12.3" + eol, outputStream.toString());
    outputStream.reset();
    os.println(12);
    assertEquals("12" + eol, outputStream.toString());
    outputStream.reset();
    os.println(12L);
    assertEquals("12" + eol, outputStream.toString());
  }

  @Test
  public void testServletPrintWriter() throws IOException {
    Writer wrapped = mock(Writer.class);
    final ServletPrintWriter writer = new HttpResponseWrapper.ServletPrintWriter(wrapped);
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        writer.close();
        return null;
      }

    }).when(wrapped).close();
    writer.close();
    verify(wrapped).close();
  }

  @Test
  public void testStreamClose() throws IOException {
    ServletOutputStream os = responseWrapper.getOutputStream();
    os.close();
    verify(requestWrapper).propagateSession();
    os.close();
    verify(requestWrapper).propagateSession();
  }

  @Test(expected = IllegalStateException.class)
  public void testWriterAndStream() throws IOException {
    responseWrapper.getWriter();
    responseWrapper.getOutputStream();
  }

  @Test(expected = IllegalStateException.class)
  public void testStreamAndWriter() throws IOException {
    responseWrapper.getOutputStream();
    responseWrapper.getWriter();
  }

  @Test
  public void testTwiceStream() throws IOException {
    SaveSessionServletOutputStream stream1 = responseWrapper.getOutputStream();
    SaveSessionServletOutputStream stream2 = responseWrapper.getOutputStream();
    assertSame(stream1, stream2);
  }

  @Test
  public void testTwiceWriter() throws IOException {
    ServletPrintWriter writer1 = responseWrapper.getWriter();
    ServletPrintWriter writer2 = responseWrapper.getWriter();
    assertSame(writer1, writer2);
  }

  @Test
  public void testHeaderContentLength() throws IOException {
    responseWrapper.addHeader("content-length", "10");
    assertEquals(10, responseWrapper.contentLength);
    verify(response).addHeader("content-length", "10");
  }

  @Test
  public void testPropagateHeader() throws IOException {
    responseWrapper.addHeader("some-header", "value");
    verify(response).addHeader("some-header", "value");
  }

  @Test
  public void testSetHeaderContentLength() throws IOException {
    responseWrapper.setHeader("content-length", "10");
    assertEquals(10, responseWrapper.contentLength);
    verify(response).setHeader("content-length", "10");
  }

  @Test
  public void testPropagateSetHeader() throws IOException {
    responseWrapper.setHeader("some-header", "value");
    verify(response).setHeader("some-header", "value");
  }
}
