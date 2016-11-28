package com.amadeus.session.agent;

import javax.annotation.Generated;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;


@Generated("manual from HelpersToInject using Bytecode plugin for Eclipse")
class ListenerHelpers implements Opcodes {

  public static void methods(String className, ClassVisitor cw, boolean addStaticInit) {

    cw.visitInnerClass("java/lang/invoke/MethodHandles$Lookup", "java/lang/invoke/MethodHandles", "Lookup",
        ACC_PUBLIC + ACC_FINAL + ACC_STATIC);

    CommonHelpers.addIsServlet3(cw);

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
      mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "$$interceptHttpListener",
          "(Ljava/lang/Object;Ljavax/servlet/http/HttpSessionEvent;)V", null, null);
      mv.visitCode();
      Label l0 = new Label();
      mv.visitLabel(l0);
      mv.visitLineNumber(1, l0);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitTypeInsn(INSTANCEOF, "java/util/EventListener");
      Label l1 = new Label();
      mv.visitJumpInsn(IFNE, l1);
      Label l2 = new Label();
      mv.visitLabel(l2);
      mv.visitLineNumber(1, l2);
      mv.visitLdcInsn("Tried registering listener %s but it was not EventListener");
      mv.visitInsn(ICONST_1);
      mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
      mv.visitInsn(DUP);
      mv.visitInsn(ICONST_0);
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
      mv.visitVarInsn(ALOAD, 1);
      mv.visitMethodInsn(INVOKEVIRTUAL, "javax/servlet/http/HttpSessionEvent", "getSession",
          "()Ljavax/servlet/http/HttpSession;", false);
      mv.visitMethodInsn(INVOKEINTERFACE, "javax/servlet/http/HttpSession", "getServletContext",
          "()Ljavax/servlet/ServletContext;", true);
      mv.visitInsn(ICONST_1);
      mv.visitInsn(ICONST_2);
      mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
      mv.visitInsn(DUP);
      mv.visitInsn(ICONST_0);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitInsn(AASTORE);
      mv.visitInsn(DUP);
      mv.visitInsn(ICONST_1);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitInsn(AASTORE);
      mv.visitMethodInsn(INVOKESTATIC, className, "$$call",
          "(Ljavax/servlet/ServletContext;I[Ljava/lang/Object;)Ljava/lang/Object;", false);
      mv.visitInsn(POP);
      Label l4 = new Label();
      mv.visitLabel(l4);
      mv.visitLineNumber(1, l4);
      mv.visitInsn(RETURN);
      Label l5 = new Label();
      mv.visitLabel(l5);
      mv.visitLocalVariable("listnerer", "Ljava/lang/Object;", null, l0, l5, 0);
      mv.visitLocalVariable("event", "Ljavax/servlet/http/HttpSessionEvent;", null, l0, l5, 1);
      mv.visitMaxs(6, 2);
      mv.visitEnd();
    }

    CommonHelpers.addCallMethod(className, cw);

    CommonHelpers.addLogError(cw);
}

  static void staticInit(String className, MethodVisitor mv) {
    mv.visitMethodInsn(INVOKESTATIC, className,
        "$$isServlet3", "()Z", false);
    mv.visitFieldInsn(PUTSTATIC, className, "$$isServlet3",
        "Z");
  }
}
