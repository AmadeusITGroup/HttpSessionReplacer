package com.amadeus.session.agent;

import javax.annotation.Generated;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.amadeus.session.servlet.HelpersToInject;

/**
 * This class is manually generated from {@link HelpersToInject} class using
 * Bytecode plugin for Eclipse
 */
@Generated("manual from HelpersToInject using Bytecode plugin for Eclipse")
public class ServletContextHelpers implements Opcodes {

  public static void methods(String className, ClassVisitor cw, boolean addStaticInit) {

    CommonHelpers.addIsServlet3(cw);

    cw.visitInnerClass("java/lang/invoke/MethodHandles$Lookup", "java/lang/invoke/MethodHandles", "Lookup",
        ACC_PUBLIC + ACC_FINAL + ACC_STATIC);

    MethodVisitor mv;

    if (addStaticInit) {
      mv = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
      mv.visitCode();
      Label l0 = new Label();
      mv.visitLabel(l0);
      mv.visitLineNumber(1, l0);
      staticInit(className, mv);
      Label l1 = new Label();
      mv.visitLabel(l1);
      mv.visitLineNumber(1, l1);
      mv.visitInsn(RETURN);
      mv.visitMaxs(1, 0);
      mv.visitEnd();
    }
    {
      mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "$$onAddListener", "(Ljava/lang/Object;Ljava/lang/Object;)V", null,
          null);
      mv.visitCode();
      Label l0 = new Label();
      mv.visitLabel(l0);
      mv.visitLineNumber(1, l0);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitTypeInsn(INSTANCEOF, "javax/servlet/ServletContext");
      Label l1 = new Label();
      mv.visitJumpInsn(IFNE, l1);
      Label l2 = new Label();
      mv.visitLabel(l2);
      mv.visitLineNumber(1, l2);
      mv.visitLdcInsn("Tried registering listener %s for object %s but object was not ServletContext");
      mv.visitInsn(ICONST_2);
      mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
      mv.visitInsn(DUP);
      mv.visitInsn(ICONST_0);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitInsn(AASTORE);
      mv.visitInsn(DUP);
      mv.visitInsn(ICONST_1);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitInsn(AASTORE);
      mv.visitMethodInsn(INVOKESTATIC, className, "$$error",
          "(Ljava/lang/String;[Ljava/lang/Object;)V", false);
      Label l3 = new Label();
      mv.visitLabel(l3);
      mv.visitLineNumber(1, l3);
      mv.visitInsn(RETURN);
      mv.visitLabel(l1);
      mv.visitLineNumber(1, l1);
      mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitTypeInsn(CHECKCAST, "javax/servlet/ServletContext");
      mv.visitVarInsn(ASTORE, 2);
      Label l4 = new Label();
      mv.visitLabel(l4);
      mv.visitLineNumber(1, l4);
      mv.visitVarInsn(ALOAD, 2);
      mv.visitInsn(ICONST_0);
      mv.visitInsn(ICONST_2);
      mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
      mv.visitInsn(DUP);
      mv.visitInsn(ICONST_0);
      mv.visitVarInsn(ALOAD, 2);
      mv.visitInsn(AASTORE);
      mv.visitInsn(DUP);
      mv.visitInsn(ICONST_1);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitInsn(AASTORE);
      mv.visitMethodInsn(INVOKESTATIC, className, "$$call",
          "(Ljavax/servlet/ServletContext;I[Ljava/lang/Object;)Ljava/lang/Object;", false);
      mv.visitInsn(POP);
      Label l5 = new Label();
      mv.visitLabel(l5);
      mv.visitLineNumber(1, l5);
      mv.visitInsn(RETURN);
      Label l6 = new Label();
      mv.visitLabel(l6);
      mv.visitLocalVariable("object", "Ljava/lang/Object;", null, l0, l6, 0);
      mv.visitLocalVariable("listener", "Ljava/lang/Object;", null, l0, l6, 1);
      mv.visitLocalVariable("servletContext", "Ljavax/servlet/ServletContext;", null, l4, l6, 2);
      mv.visitMaxs(6, 3);
      mv.visitEnd();
    }
    CommonHelpers.addCallMethod(className, cw);

    CommonHelpers.addLogError(cw);
  }

  static void staticInit(String className, MethodVisitor mv) {
    mv.visitMethodInsn(INVOKESTATIC, className, "$$isServlet3", "()Z", false);
    mv.visitFieldInsn(PUTSTATIC, className, "$$isServlet3", "Z");
  }
}
