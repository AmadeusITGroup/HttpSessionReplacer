package com.amadeus.session.agent;

import javax.annotation.Generated;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

@Generated("manual from HelpersToInject using Bytecode plugin for Eclipse")
class FilterHelpers implements Opcodes {

  public static void methods(String className, ClassVisitor cw, boolean addStaticInit) {

    CommonHelpers.addIsServlet3(cw);

    cw.visitInnerClass("java/lang/invoke/MethodHandles$Lookup", "java/lang/invoke/MethodHandles", "Lookup",
        ACC_PUBLIC + ACC_FINAL + ACC_STATIC);

    MethodVisitor mv;
    {
      FieldVisitor fv = cw.visitField(ACC_PUBLIC + ACC_STATIC, "$$debugMode", "Z", null, null);
      fv.visitEnd();
    }

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
      mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "$$prepareRequest",
          "(Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletResponse;Ljavax/servlet/ServletContext;)Ljavax/servlet/ServletRequest;",
          null, null);
      mv.visitCode();
      Label l0 = new Label();
      mv.visitLabel(l0);
      mv.visitLineNumber(1, l0);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitVarInsn(ALOAD, 2);
      mv.visitMethodInsn(INVOKESTATIC, className, "$$context",
          "(Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletContext;)Ljavax/servlet/ServletContext;", false);
      mv.visitInsn(ICONST_4);
      mv.visitInsn(ICONST_3);
      mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
      mv.visitInsn(DUP);
      mv.visitInsn(ICONST_0);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitInsn(AASTORE);
      mv.visitInsn(DUP);
      mv.visitInsn(ICONST_1);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitInsn(AASTORE);
      mv.visitInsn(DUP);
      mv.visitInsn(ICONST_2);
      Label l1 = new Label();
      mv.visitLabel(l1);
      mv.visitLineNumber(1, l1);
      mv.visitVarInsn(ALOAD, 2);
      mv.visitInsn(AASTORE);
      Label l2 = new Label();
      mv.visitLabel(l2);
      mv.visitLineNumber(1, l2);
      mv.visitMethodInsn(INVOKESTATIC, className, "$$call",
          "(Ljavax/servlet/ServletContext;I[Ljava/lang/Object;)Ljava/lang/Object;", false);
      mv.visitTypeInsn(CHECKCAST, "javax/servlet/ServletRequest");
      mv.visitInsn(ARETURN);
      Label l3 = new Label();
      mv.visitLabel(l3);
      mv.visitLocalVariable("request", "Ljavax/servlet/ServletRequest;", null, l0, l3, 0);
      mv.visitLocalVariable("response", "Ljavax/servlet/ServletResponse;", null, l0, l3, 1);
      mv.visitLocalVariable("filterContext", "Ljavax/servlet/ServletContext;", null, l0, l3, 2);
      mv.visitMaxs(6, 3);
      mv.visitEnd();
    }
    {
      mv = cw.visitMethod(ACC_PRIVATE + ACC_STATIC, "$$context",
          "(Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletContext;)Ljavax/servlet/ServletContext;", null, null);
      mv.visitCode();
      Label l0 = new Label();
      mv.visitLabel(l0);
      mv.visitLineNumber(1, l0);
      mv.visitFieldInsn(GETSTATIC, className, "$$isServlet3", "Z");
      Label l1 = new Label();
      mv.visitJumpInsn(IFEQ, l1);
      Label l2 = new Label();
      mv.visitLabel(l2);
      mv.visitLineNumber(1, l2);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitMethodInsn(INVOKEINTERFACE, "javax/servlet/ServletRequest", "getServletContext",
          "()Ljavax/servlet/ServletContext;", true);
      mv.visitVarInsn(ASTORE, 2);
      Label l3 = new Label();
      mv.visitLabel(l3);
      mv.visitLineNumber(1, l3);
      mv.visitVarInsn(ALOAD, 2);
      Label l4 = new Label();
      mv.visitJumpInsn(IFNONNULL, l4);
      mv.visitVarInsn(ALOAD, 1);
      Label l5 = new Label();
      mv.visitJumpInsn(GOTO, l5);
      mv.visitLabel(l4);
      mv.visitFrame(Opcodes.F_APPEND, 1, new Object[] { "javax/servlet/ServletContext" }, 0, null);
      mv.visitVarInsn(ALOAD, 2);
      mv.visitLabel(l5);
      mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] { "javax/servlet/ServletContext" });
      mv.visitInsn(ARETURN);
      mv.visitLabel(l1);
      mv.visitLineNumber(1, l1);
      mv.visitFrame(Opcodes.F_CHOP, 1, null, 0, null);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitInsn(ARETURN);
      Label l6 = new Label();
      mv.visitLabel(l6);
      mv.visitLocalVariable("request", "Ljavax/servlet/ServletRequest;", null, l0, l6, 0);
      mv.visitLocalVariable("context", "Ljavax/servlet/ServletContext;", null, l0, l6, 1);
      mv.visitLocalVariable("sc", "Ljavax/servlet/ServletContext;", null, l3, l1, 2);
      mv.visitMaxs(1, 3);
      mv.visitEnd();
    }
    {
      mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "$$prepareResponse",
          "(Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletResponse;Ljavax/servlet/ServletContext;)Ljavax/servlet/ServletResponse;",
          null, null);
      mv.visitCode();
      Label l0 = new Label();
      mv.visitLabel(l0);
      mv.visitLineNumber(1, l0);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitVarInsn(ALOAD, 2);
      mv.visitMethodInsn(INVOKESTATIC, className, "$$context",
          "(Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletContext;)Ljavax/servlet/ServletContext;", false);
      mv.visitInsn(ICONST_3);
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
      mv.visitTypeInsn(CHECKCAST, "javax/servlet/ServletResponse");
      mv.visitInsn(ARETURN);
      Label l1 = new Label();
      mv.visitLabel(l1);
      mv.visitLocalVariable("request", "Ljavax/servlet/ServletRequest;", null, l0, l1, 0);
      mv.visitLocalVariable("response", "Ljavax/servlet/ServletResponse;", null, l0, l1, 1);
      mv.visitLocalVariable("filterContext", "Ljavax/servlet/ServletContext;", null, l0, l1, 2);
      mv.visitMaxs(6, 3);
      mv.visitEnd();
    }
    {
      mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "$$initSessionManagement", "(Ljavax/servlet/ServletContext;)V", null, null);
      mv.visitCode();
      Label l0 = new Label();
      mv.visitLabel(l0);
      mv.visitLineNumber(1, l0);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitInsn(ICONST_M1);
      mv.visitInsn(ICONST_1);
      mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
      mv.visitInsn(DUP);
      mv.visitInsn(ICONST_0);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitInsn(AASTORE);
      mv.visitMethodInsn(INVOKESTATIC, className, "$$call", "(Ljavax/servlet/ServletContext;I[Ljava/lang/Object;)Ljava/lang/Object;", false);
      mv.visitInsn(POP);
      Label l1 = new Label();
      mv.visitLabel(l1);
      mv.visitLineNumber(1, l1);
      mv.visitInsn(RETURN);
      Label l2 = new Label();
      mv.visitLabel(l2);
      mv.visitLocalVariable("servletContext", "Ljavax/servlet/ServletContext;", null, l0, l2, 0);
      mv.visitMaxs(6, 1);
      mv.visitEnd();
    }
    {
      mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "$$commitRequest",
          "(Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletContext;)V", null, null);
      mv.visitCode();
      Label l0 = new Label();
      mv.visitLabel(l0);
      mv.visitLineNumber(1, l0);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitVarInsn(ALOAD, 2);
      mv.visitMethodInsn(INVOKESTATIC, className, "$$context",
          "(Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletContext;)Ljavax/servlet/ServletContext;", false);
      mv.visitInsn(ICONST_5);
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
      Label l1 = new Label();
      mv.visitLabel(l1);
      mv.visitLineNumber(1, l1);
      mv.visitInsn(RETURN);
      Label l2 = new Label();
      mv.visitLabel(l2);
      mv.visitLocalVariable("request", "Ljavax/servlet/ServletRequest;", null, l0, l2, 0);
      mv.visitLocalVariable("oldRequest", "Ljavax/servlet/ServletRequest;", null, l0, l2, 1);
      mv.visitLocalVariable("filterContext", "Ljavax/servlet/ServletContext;", null, l0, l2, 2);
      mv.visitMaxs(6, 3);
      mv.visitEnd();
    }
    {
      mv = cw.visitMethod(ACC_PRIVATE + ACC_STATIC + ACC_VARARGS, "$$debug", "(Ljava/lang/String;[Ljava/lang/Object;)V",
          null, null);
      mv.visitCode();
      Label l0 = new Label();
      mv.visitLabel(l0);
      mv.visitLineNumber(1, l0);
      mv.visitFieldInsn(GETSTATIC, className, "$$debugMode", "Z");
      Label l1 = new Label();
      mv.visitJumpInsn(IFEQ, l1);
      Label l2 = new Label();
      mv.visitLabel(l2);
      mv.visitLineNumber(1, l2);
      mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
      mv.visitLdcInsn("SessionAgent: %s");
      mv.visitInsn(ICONST_1);
      mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
      mv.visitInsn(DUP);
      mv.visitInsn(ICONST_0);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitMethodInsn(INVOKESTATIC, "java/lang/String", "format",
          "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;", false);
      mv.visitInsn(AASTORE);
      mv.visitMethodInsn(INVOKESTATIC, "java/lang/String", "format",
          "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;", false);
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
      Label l3 = new Label();
      mv.visitLabel(l3);
      mv.visitLineNumber(1, l3);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitJumpInsn(IFNULL, l1);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitInsn(ARRAYLENGTH);
      mv.visitInsn(ICONST_1);
      mv.visitJumpInsn(IF_ICMPLE, l1);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitInsn(ARRAYLENGTH);
      mv.visitInsn(ICONST_1);
      mv.visitInsn(ISUB);
      mv.visitInsn(AALOAD);
      mv.visitTypeInsn(INSTANCEOF, "java/lang/Throwable");
      mv.visitJumpInsn(IFEQ, l1);
      Label l4 = new Label();
      mv.visitLabel(l4);
      mv.visitLineNumber(1, l4);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitInsn(ARRAYLENGTH);
      mv.visitInsn(ICONST_1);
      mv.visitInsn(ISUB);
      mv.visitInsn(AALOAD);
      mv.visitTypeInsn(CHECKCAST, "java/lang/Throwable");
      mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Throwable", "printStackTrace", "(Ljava/io/PrintStream;)V", false);
      mv.visitLabel(l1);
      mv.visitLineNumber(1, l1);
      mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
      mv.visitInsn(RETURN);
      Label l5 = new Label();
      mv.visitLabel(l5);
      mv.visitLocalVariable("format", "Ljava/lang/String;", null, l0, l5, 0);
      mv.visitLocalVariable("args", "[Ljava/lang/Object;", null, l0, l5, 1);
      mv.visitMaxs(7, 2);
      mv.visitEnd();
    }
    {
      mv = cw.visitMethod(ACC_PRIVATE + ACC_STATIC, "$$getPropertySecured", "(Ljava/lang/String;)Ljava/lang/String;",
          null, null);
      mv.visitCode();
      Label l0 = new Label();
      Label l1 = new Label();
      Label l2 = new Label();
      mv.visitTryCatchBlock(l0, l1, l2, "java/lang/SecurityException");
      mv.visitLabel(l0);
      mv.visitLineNumber(1, l0);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitInsn(ACONST_NULL);
      mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "getProperty",
          "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", false);
      mv.visitLabel(l1);
      mv.visitInsn(ARETURN);
      mv.visitLabel(l2);
      mv.visitLineNumber(1, l2);
      mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] { "java/lang/SecurityException" });
      mv.visitVarInsn(ASTORE, 1);
      Label l3 = new Label();
      mv.visitLabel(l3);
      mv.visitLineNumber(1, l3);
      mv.visitLdcInsn("Security exception when trying to get system property");
      mv.visitInsn(ICONST_1);
      mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
      mv.visitInsn(DUP);
      mv.visitInsn(ICONST_0);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitInsn(AASTORE);
      mv.visitMethodInsn(INVOKESTATIC, className, "$$debug", "(Ljava/lang/String;[Ljava/lang/Object;)V", false);
      Label l4 = new Label();
      mv.visitLabel(l4);
      mv.visitLineNumber(1, l4);
      mv.visitInsn(ACONST_NULL);
      mv.visitInsn(ARETURN);
      Label l5 = new Label();
      mv.visitLabel(l5);
      mv.visitLocalVariable("key", "Ljava/lang/String;", null, l0, l5, 0);
      mv.visitLocalVariable("e", "Ljava/lang/SecurityException;", null, l3, l5, 1);
      mv.visitMaxs(5, 2);
      mv.visitEnd();
    }
    CommonHelpers.addCallMethod(className, cw);

    CommonHelpers.addLogError(cw);
  }

  static void staticInit(String className, MethodVisitor mv) {
    mv.visitLdcInsn("com.amadeus.session.debug");
    mv.visitMethodInsn(INVOKESTATIC, className, "$$getPropertySecured", "(Ljava/lang/String;)Ljava/lang/String;",
        false);
    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "parseBoolean", "(Ljava/lang/String;)Z", false);
    mv.visitFieldInsn(PUTSTATIC, className, "$$debugMode", "Z");
    mv.visitMethodInsn(INVOKESTATIC, className, "$$isServlet3", "()Z", false);
    mv.visitFieldInsn(PUTSTATIC, className, "$$isServlet3", "Z");
  }
}
