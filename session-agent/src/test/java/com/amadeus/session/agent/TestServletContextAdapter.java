package com.amadeus.session.agent;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PROTECTED;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;

import org.junit.Test;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import com.amadeus.session.agent.ServletContextAdapter.EnahceAddListener;

public class TestServletContextAdapter {

  @Test
  public void testIgnoreNonPublicMethods() {
    ClassVisitor cv = mock(ClassVisitor.class);
    ServletContextAdapter sca = new ServletContextAdapter(cv);
    MethodVisitor result = sca.visitMethod(ACC_PROTECTED, "addListener", "(Ljava/lang/Object;)V", null, null);
    assertThat(result, not(instanceOf(EnahceAddListener.class)));
    result = sca.visitMethod(ACC_PRIVATE, "addListener", "(Ljava/util/EventListener;)V", null, null);
    assertThat(result, not(instanceOf(EnahceAddListener.class)));
    result = sca.visitMethod(ACC_STATIC | ACC_PUBLIC, "addListener", "(Ljava/util/EventListener;)V", null, null);
    assertThat(result, not(instanceOf(EnahceAddListener.class)));
  }

  @Test
  public void testVisitMethodToIntercept() {
    ClassVisitor cv = mock(ClassVisitor.class);
    ServletContextAdapter sca = new ServletContextAdapter(cv);
    MethodVisitor result = sca.visitMethod(ACC_PUBLIC, "addListener", "(Ljava/lang/Object;)V", null, null);
    assertThat(result, instanceOf(EnahceAddListener.class));
    result = sca.visitMethod(ACC_PUBLIC, "addListener", "(Ljava/util/EventListener;)V", null, null);
    assertThat(result, instanceOf(EnahceAddListener.class));
    result = sca.visitMethod(ACC_PUBLIC, "addListener", "(Ljava/util/EventListener;)V", null, null);
    assertThat(result, instanceOf(EnahceAddListener.class));
  }

  @Test
  public void testVisitMethodNotToIntercept() {
    ClassVisitor cv = mock(ClassVisitor.class);
    ServletContextAdapter sca = new ServletContextAdapter(cv);
    MethodVisitor result = sca.visitMethod(ACC_PUBLIC, "addListeners", "(Ljava/lang/Object;)V", null, null);
    assertThat(result, not(instanceOf(EnahceAddListener.class)));
    result = sca.visitMethod(ACC_PUBLIC, "addListener", "(Ljava/util/List;)V", null, null);
    assertThat(result, not(instanceOf(EnahceAddListener.class)));
    result = sca.visitMethod(ACC_PUBLIC, "addListener", "(Ljava/lang/String;)V", null, null);
    assertThat(result, not(instanceOf(EnahceAddListener.class)));
  }
}
