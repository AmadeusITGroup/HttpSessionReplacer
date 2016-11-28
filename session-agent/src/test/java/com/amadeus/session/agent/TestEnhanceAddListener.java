package com.amadeus.session.agent;

import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.mockito.Mockito;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class TestEnhanceAddListener {

  @Test
  public void testInstumentation() {
    ClassVisitor cv = mock(ClassVisitor.class);
    ServletContextAdapter adapter = new ServletContextAdapter(cv );
    adapter.className = "TestClass";
    MethodVisitor mv = mock(MethodVisitor.class);
    ServletContextAdapter.EnahceAddListener eal = adapter.new EnahceAddListener(mv);
    eal.visitCode();
    Mockito.verify(mv).visitVarInsn(Opcodes.ALOAD, 0);
    Mockito.verify(mv).visitVarInsn(Opcodes.ALOAD, 1);
    Mockito.verify(mv).visitMethodInsn(Opcodes.INVOKESTATIC, "TestClass", "$$onAddListener",
        "(Ljava/lang/Object;Ljava/lang/Object;)V", false);
  }
}
