package com.amadeus.session.agent;

import static com.amadeus.session.agent.SessionAgent.debug;
import static com.amadeus.session.agent.SessionAgent.error;
import static org.objectweb.asm.Opcodes.AASTORE;
import static org.objectweb.asm.Opcodes.ACC_ABSTRACT;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ANEWARRAY;
import static org.objectweb.asm.Opcodes.ASM5;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.ATHROW;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.F_APPEND;
import static org.objectweb.asm.Opcodes.F_CHOP;
import static org.objectweb.asm.Opcodes.F_FULL;
import static org.objectweb.asm.Opcodes.F_SAME;
import static org.objectweb.asm.Opcodes.F_SAME1;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.ICONST_2;
import static org.objectweb.asm.Opcodes.ICONST_3;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.IFNONNULL;
import static org.objectweb.asm.Opcodes.IFNULL;
import static org.objectweb.asm.Opcodes.INSTANCEOF;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;

import javax.annotation.Generated;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

/**
 * This class enables session management by modifying each Filter
 * implementation. Modification involves the following:
 * <ul>
 * <li>Rename existing doFilter methods to $$renamed_doFilter
 *
 * <li>Create new method doFilter equivalent to following code:</li>
 * </ul>
 * <code>
 *   public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
 *     ServletRequest oldRequest = request;
 *     request = SessionHelpers.prepareRequest(oldRequest, response, $$injected_servletContext);
 *     response = SessionHelpers.prepareResponse(request, response, $$injected_servletContext);
 *     try {
 *       $$renamed_doFilter(request, response, chain);
 *     } finally {
 *       SessionHelpers.commitRequest(request, oldRequest, $$injected_servletContext);
 *     }
 *   }
 * </code>
 */
@Generated("manual using Bytecode plugin for Eclipse")
public class FilterAdapter extends ClassVisitor {
  private boolean abstractClass;
  private String filterClass;
  private String superClass;
  private boolean gotInitMethod;
  private boolean gotDoFilterMethod;
  private boolean addedStaticInit;

  public FilterAdapter(ClassVisitor cv) {
    super(ASM5, cv);
  }

  @Override
  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    super.visit(version, access, name, signature, superName, interfaces);
    abstractClass = (access & ACC_ABSTRACT) != 0;
    filterClass = name;
    superClass = superName;
    FieldVisitor fv = cv.visitField(ACC_PUBLIC, "$$injected_servletContext", "Ljavax/servlet/ServletContext;", null,
        null);
    fv.visitEnd();
    fv = cv.visitField(ACC_PUBLIC, "$$injected_superDoFilter", "Ljava/lang/invoke/MethodHandle;", null, null);
    fv.visitEnd();
    fv = cv.visitField(ACC_PUBLIC, "$$injected_superInit", "Ljava/lang/invoke/MethodHandle;", null, null);
    fv.visitEnd();
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    if ("init".equals(name) && "(Ljavax/servlet/FilterConfig;)V".equals(desc)) {
      debug("Enhancing existing init(...) method in class %s", filterClass);
      gotInitMethod = true;
      return new EnahceInitFilter(super.visitMethod(access & (~ACC_FINAL), name, desc, signature, exceptions));
    } else if ("doFilter".equals(name)) {
      if ("(Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletResponse;Ljavax/servlet/FilterChain;)V".equals(desc)) {
        debug("Enhancing doFilter in class %s", filterClass);
        gotDoFilterMethod = true;
        // Add new doFilter method. This method will delegate call to the hidden
        // old method.
        addDoFilter();
        // Rename doFilter
        MethodVisitor mv = cv.visitMethod(access & (~ACC_FINAL), "$$renamed_doFilter", desc, signature, exceptions);
        // MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
        return mv;
      } else {
        debug("Non-instrumented doFilter in class %s %s", filterClass, desc);
      }
    }
    if ("<clinit>".equals(name) && !addedStaticInit) {
      MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
      return new EnhanceStaticInit(mv);
    }
    return super.visitMethod(access, name, desc, signature, exceptions);
  }

  class EnhanceStaticInit extends MethodVisitor {

    EnhanceStaticInit(MethodVisitor mv) {
      super(ASM5, mv);
    }

    @Override
    public void visitCode() {
      FilterHelpers.staticInit(filterClass, mv);
      addedStaticInit = true;
      super.visitCode();
    }
  }

  @Override
  public void visitEnd() {
    addInitForSessionMethod();
    addInvokeInSuperMethod();
    if (!gotInitMethod && !abstractClass) {
      debug("Adding init(...) method in filter class %s", filterClass);
      addInitMethod();
    }
    if (!gotDoFilterMethod && !abstractClass) {
      if (superClass == null) {
        error(
            "No doFilter(...) method in filter class %s "
                + "and no super class found. This is not normal behavior, something is wrong with bytecode.",
            filterClass);

      } else {
        debug("Add doFilter(...) method in filter class %s"
            + " that will call super method doFilter() in parent class %s", filterClass, superClass);
        addDoFilterWithSuper();
      }
    }
    debug("Adding injected methods in filter class %s", filterClass);
    FilterHelpers.methods(filterClass, cv, !addedStaticInit);
    super.visitEnd();
  }

  private void addInitForSessionMethod() {
    MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "$$injected_initForSession", "(Ljavax/servlet/FilterConfig;)V", null, null);
    mv.visitCode();
    Label l0 = new Label();
    Label l1 = new Label();
    Label l2 = new Label();
    mv.visitTryCatchBlock(l0, l1, l2, "java/lang/NoSuchMethodException");
    Label l3 = new Label();
    mv.visitTryCatchBlock(l0, l1, l3, "java/lang/IllegalAccessException");
    Label l4 = new Label();
    mv.visitLabel(l4);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, filterClass, "$$injected_servletContext", "Ljavax/servlet/ServletContext;");
    Label l5 = new Label();
    mv.visitJumpInsn(IFNONNULL, l5);
    Label l6 = new Label();
    mv.visitLabel(l6);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKEINTERFACE, "javax/servlet/FilterConfig", "getServletContext",
        "()Ljavax/servlet/ServletContext;", true);
    mv.visitFieldInsn(PUTFIELD, filterClass, "$$injected_servletContext", "Ljavax/servlet/ServletContext;");
    Label l7 = new Label();
    mv.visitLabel(l7);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, filterClass, "$$injected_servletContext", "Ljavax/servlet/ServletContext;");
    mv.visitMethodInsn(INVOKESTATIC, filterClass, "$$initSessionManagement",
        "(Ljavax/servlet/ServletContext;)V", false);
    Label l8 = new Label();
    mv.visitLabel(l8);
    mv.visitLdcInsn(Type.getType("L" + filterClass + ";"));
    mv.visitVarInsn(ASTORE, 2);
    Label l9 = new Label();
    mv.visitLabel(l9);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getSuperclass", "()Ljava/lang/Class;", false);
    mv.visitVarInsn(ASTORE, 3);
    Label l10 = new Label();
    mv.visitLabel(l10);
    mv.visitLdcInsn(Type.getType("Ljavax/servlet/Filter;"));
    mv.visitVarInsn(ALOAD, 3);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "isAssignableFrom", "(Ljava/lang/Class;)Z", false);
    mv.visitJumpInsn(IFEQ, l5);
    Label l11 = new Label();
    mv.visitLabel(l11);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKEVIRTUAL, filterClass, "$$invokeInSuper", "(Ljavax/servlet/FilterConfig;)V", false);
    Label l12 = new Label();
    mv.visitLabel(l12);
    mv.visitLineNumber(1, l12);
    mv.visitFieldInsn(GETSTATIC, "java/lang/Void", "TYPE", "Ljava/lang/Class;");
    mv.visitLdcInsn(Type.getType("Ljavax/servlet/ServletRequest;"));
    mv.visitInsn(ICONST_2);
    mv.visitTypeInsn(ANEWARRAY, "java/lang/Class");
    mv.visitInsn(DUP);
    mv.visitInsn(ICONST_0);
    mv.visitLdcInsn(Type.getType("Ljavax/servlet/ServletResponse;"));
    mv.visitInsn(AASTORE);
    mv.visitInsn(DUP);
    mv.visitInsn(ICONST_1);
    mv.visitLdcInsn(Type.getType("Ljavax/servlet/FilterChain;"));
    mv.visitInsn(AASTORE);
    mv.visitMethodInsn(INVOKESTATIC, "java/lang/invoke/MethodType", "methodType",
        "(Ljava/lang/Class;Ljava/lang/Class;[Ljava/lang/Class;)Ljava/lang/invoke/MethodType;", false);
    mv.visitVarInsn(ASTORE, 4);
    mv.visitLabel(l0);
    mv.visitLineNumber(1, l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESTATIC, "java/lang/invoke/MethodHandles", "lookup",
        "()Ljava/lang/invoke/MethodHandles$Lookup;", false);
    mv.visitVarInsn(ALOAD, 3);
    mv.visitLdcInsn("doFilter");
    mv.visitVarInsn(ALOAD, 4);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findSpecial",
        "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;",
        false);
    mv.visitFieldInsn(PUTFIELD, filterClass, "$$injected_superDoFilter", "Ljava/lang/invoke/MethodHandle;");
    Label l13 = new Label();
    mv.visitLabel(l13);
    mv.visitFieldInsn(GETSTATIC, "java/lang/Void", "TYPE", "Ljava/lang/Class;");
    mv.visitLdcInsn(Type.getType("Ljavax/servlet/FilterConfig;"));
    mv.visitMethodInsn(INVOKESTATIC, "java/lang/invoke/MethodType", "methodType",
        "(Ljava/lang/Class;Ljava/lang/Class;)Ljava/lang/invoke/MethodType;", false);
    mv.visitVarInsn(ASTORE, 4);
    Label l14 = new Label();
    mv.visitLabel(l14);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESTATIC, "java/lang/invoke/MethodHandles", "lookup",
        "()Ljava/lang/invoke/MethodHandles$Lookup;", false);
    mv.visitVarInsn(ALOAD, 3);
    mv.visitLdcInsn("init");
    mv.visitVarInsn(ALOAD, 4);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findSpecial",
        "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;",
        false);
    mv.visitFieldInsn(PUTFIELD, filterClass, "$$injected_superInit", "Ljava/lang/invoke/MethodHandle;");
    mv.visitLabel(l1);
    mv.visitJumpInsn(GOTO, l5);
    mv.visitLabel(l2);
    mv.visitFrame(F_FULL, 5, new Object[] { filterClass, "javax/servlet/FilterConfig", "java/lang/Class",
        "java/lang/Class", "java/lang/invoke/MethodType" }, 1, new Object[] { "java/lang/NoSuchMethodException" });
    mv.visitVarInsn(ASTORE, 5);
    Label l15 = new Label();
    mv.visitLabel(l15);
    mv.visitLdcInsn("There is no %s element in parent class %s of filter %s ");
    mv.visitInsn(ICONST_3);
    mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
    mv.visitInsn(DUP);
    mv.visitInsn(ICONST_0);
    mv.visitVarInsn(ALOAD, 5);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/NoSuchMethodException", "getMessage", "()Ljava/lang/String;", false);
    mv.visitInsn(AASTORE);
    mv.visitInsn(DUP);
    mv.visitInsn(ICONST_1);
    mv.visitVarInsn(ALOAD, 3);
    mv.visitInsn(AASTORE);
    mv.visitInsn(DUP);
    mv.visitInsn(ICONST_2);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitInsn(AASTORE);
    mv.visitMethodInsn(INVOKESTATIC, filterClass, "$$debug",
        "(Ljava/lang/String;[Ljava/lang/Object;)V", false);
    Label l16 = new Label();
    mv.visitLabel(l16);
    mv.visitJumpInsn(GOTO, l5);
    mv.visitLabel(l3);
    mv.visitFrame(F_SAME1, 0, null, 1, new Object[] { "java/lang/IllegalAccessException" });
    mv.visitVarInsn(ASTORE, 5);
    Label l17 = new Label();
    mv.visitLabel(l17);
    mv.visitLdcInsn("Unable to access element in parent class %s of filter %s. Cause %s");
    mv.visitInsn(ICONST_3);
    mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
    mv.visitInsn(DUP);
    mv.visitInsn(ICONST_0);
    mv.visitVarInsn(ALOAD, 3);
    mv.visitInsn(AASTORE);
    mv.visitInsn(DUP);
    mv.visitInsn(ICONST_1);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitInsn(AASTORE);
    mv.visitInsn(DUP);
    mv.visitInsn(ICONST_2);
    mv.visitVarInsn(ALOAD, 5);
    mv.visitInsn(AASTORE);
    mv.visitMethodInsn(INVOKESTATIC, filterClass, "$$debug",
        "(Ljava/lang/String;[Ljava/lang/Object;)V", false);
    mv.visitLabel(l5);
    mv.visitFrame(F_CHOP, 3, null, 0, null);
    mv.visitInsn(RETURN);
    Label l18 = new Label();
    mv.visitLabel(l18);
    mv.visitLocalVariable("e", "Ljava/lang/NoSuchMethodException;", null, l15, l16, 5);
    mv.visitLocalVariable("e", "Ljava/lang/IllegalAccessException;", null, l17, l5, 5);
    mv.visitLocalVariable("type", "Ljava/lang/invoke/MethodType;", null, l0, l5, 4);
    mv.visitLocalVariable("thisClass", "Ljava/lang/Class;", "Ljava/lang/Class<*>;", l9, l5, 2);
    mv.visitLocalVariable("superClass", "Ljava/lang/Class;", "Ljava/lang/Class<*>;", l10, l5, 3);
    mv.visitLocalVariable("this", "L" + filterClass + ";", null, l4, l18, 0);
    mv.visitLocalVariable("config", "Ljavax/servlet/FilterConfig;", null, l4, l18, 1);
    mv.visitMaxs(6, 6);
    mv.visitEnd();
  }

  private void addInvokeInSuperMethod() {
    MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "$$invokeInSuper", "(Ljavax/servlet/FilterConfig;)V", null, null);
    mv.visitCode();
    Label l0 = new Label();
    Label l1 = new Label();
    Label l2 = new Label();
    mv.visitTryCatchBlock(l0, l1, l2, "java/lang/NoSuchMethodException");
    Label l3 = new Label();
    mv.visitTryCatchBlock(l0, l1, l3, "java/lang/IllegalAccessException");
    Label l4 = new Label();
    mv.visitTryCatchBlock(l0, l1, l4, "java/lang/Throwable");
    Label l5 = new Label();
    mv.visitLabel(l5);
    mv.visitLdcInsn(Type.getType("L" + filterClass + ";"));
    mv.visitVarInsn(ASTORE, 2);
    Label l6 = new Label();
    mv.visitLabel(l6);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getSuperclass", "()Ljava/lang/Class;", false);
    mv.visitVarInsn(ASTORE, 3);
    mv.visitLabel(l0);
    mv.visitFieldInsn(GETSTATIC, "java/lang/Void", "TYPE", "Ljava/lang/Class;");
    mv.visitLdcInsn(Type.getType("Ljavax/servlet/FilterConfig;"));
    mv.visitMethodInsn(INVOKESTATIC, "java/lang/invoke/MethodType", "methodType",
        "(Ljava/lang/Class;Ljava/lang/Class;)Ljava/lang/invoke/MethodType;", false);
    mv.visitVarInsn(ASTORE, 4);
    Label l7 = new Label();
    mv.visitLabel(l7);
    mv.visitLineNumber(1, l7);
    mv.visitMethodInsn(INVOKESTATIC, "java/lang/invoke/MethodHandles", "lookup",
        "()Ljava/lang/invoke/MethodHandles$Lookup;", false);
    mv.visitVarInsn(ALOAD, 3);
    mv.visitLdcInsn("$$injected_initForSession");
    mv.visitVarInsn(ALOAD, 4);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findSpecial",
        "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;",
        false);
    mv.visitVarInsn(ASTORE, 5);
    Label l8 = new Label();
    mv.visitLabel(l8);
    mv.visitLineNumber(1, l8);
    mv.visitVarInsn(ALOAD, 5);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact",
        "(L" + filterClass + ";Ljavax/servlet/FilterConfig;)V", false);
    mv.visitLabel(l1);
    mv.visitLineNumber(1, l1);
    Label l9 = new Label();
    mv.visitJumpInsn(GOTO, l9);
    mv.visitLabel(l2);
    mv.visitFrame(F_FULL, 4,
        new Object[] { filterClass, "javax/servlet/FilterConfig", "java/lang/Class", "java/lang/Class" }, 1,
        new Object[] { "java/lang/NoSuchMethodException" });
    mv.visitVarInsn(ASTORE, 4);
    Label l10 = new Label();
    mv.visitLabel(l10);
    mv.visitLineNumber(1, l10);
    mv.visitLdcInsn("There is no $$injected_initForSession element in parent class %s of filter %s");
    mv.visitInsn(ICONST_2);
    mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
    mv.visitInsn(DUP);
    mv.visitInsn(ICONST_0);
    mv.visitVarInsn(ALOAD, 3);
    mv.visitInsn(AASTORE);
    mv.visitInsn(DUP);
    mv.visitInsn(ICONST_1);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitInsn(AASTORE);
    mv.visitMethodInsn(INVOKESTATIC, filterClass, "$$debug",
        "(Ljava/lang/String;[Ljava/lang/Object;)V", false);
    Label l11 = new Label();
    mv.visitLabel(l11);
    mv.visitJumpInsn(GOTO, l9);
    mv.visitLabel(l3);
    mv.visitLineNumber(1, l3);
    mv.visitFrame(F_SAME1, 0, null, 1, new Object[] { "java/lang/IllegalAccessException" });
    mv.visitVarInsn(ASTORE, 4);
    Label l12 = new Label();
    mv.visitLabel(l12);
    mv.visitLineNumber(1, l12);
    mv.visitLdcInsn("Unable to access $$injected_initForSession element in parent class %s of filter %s. Cause %s");
    mv.visitInsn(ICONST_3);
    mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
    mv.visitInsn(DUP);
    mv.visitInsn(ICONST_0);
    mv.visitVarInsn(ALOAD, 3);
    mv.visitInsn(AASTORE);
    mv.visitInsn(DUP);
    mv.visitInsn(ICONST_1);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitInsn(AASTORE);
    mv.visitInsn(DUP);
    mv.visitInsn(ICONST_2);
    mv.visitVarInsn(ALOAD, 4);
    mv.visitInsn(AASTORE);
    mv.visitMethodInsn(INVOKESTATIC, filterClass, "$$error",
        "(Ljava/lang/String;[Ljava/lang/Object;)V", false);
    Label l13 = new Label();
    mv.visitLabel(l13);
    mv.visitJumpInsn(GOTO, l9);
    mv.visitLabel(l4);
    mv.visitLineNumber(1, l4);
    mv.visitFrame(F_SAME1, 0, null, 1, new Object[] { "java/lang/Throwable" });
    mv.visitVarInsn(ASTORE, 4);
    Label l14 = new Label();
    mv.visitLabel(l14);
    mv.visitLineNumber(1, l14);
    mv.visitVarInsn(ALOAD, 4);
    mv.visitTypeInsn(INSTANCEOF, "java/lang/Error");
    Label l15 = new Label();
    mv.visitJumpInsn(IFEQ, l15);
    Label l16 = new Label();
    mv.visitLabel(l16);
    mv.visitLineNumber(1, l16);
    mv.visitVarInsn(ALOAD, 4);
    mv.visitTypeInsn(CHECKCAST, "java/lang/Error");
    mv.visitInsn(ATHROW);
    mv.visitLabel(l15);
    mv.visitLineNumber(1, l15);
    mv.visitFrame(F_APPEND, 1, new Object[] { "java/lang/Throwable" }, 0, null);
    mv.visitVarInsn(ALOAD, 4);
    mv.visitTypeInsn(INSTANCEOF, "java/lang/RuntimeException");
    Label l17 = new Label();
    mv.visitJumpInsn(IFEQ, l17);
    Label l18 = new Label();
    mv.visitLabel(l18);
    mv.visitLineNumber(1, l18);
    mv.visitVarInsn(ALOAD, 4);
    mv.visitTypeInsn(CHECKCAST, "java/lang/RuntimeException");
    mv.visitInsn(ATHROW);
    mv.visitLabel(l17);
    mv.visitLineNumber(1, l17);
    mv.visitFrame(F_SAME, 0, null, 0, null);
    mv.visitTypeInsn(NEW, "java/lang/IllegalStateException");
    mv.visitInsn(DUP);
    mv.visitLdcInsn("An exception occured while invoking super-class method $$injected_initForSession");
    mv.visitVarInsn(ALOAD, 4);
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/IllegalStateException", "<init>",
        "(Ljava/lang/String;Ljava/lang/Throwable;)V", false);
    mv.visitInsn(ATHROW);
    mv.visitLabel(l9);
    mv.visitLineNumber(1, l9);
    mv.visitFrame(F_CHOP, 1, null, 0, null);
    mv.visitInsn(RETURN);
    Label l19 = new Label();
    mv.visitLabel(l19);
    mv.visitLocalVariable("this", "L" + filterClass + ";", null, l5, l19, 0);
    mv.visitLocalVariable("config", "Ljavax/servlet/FilterConfig;", null, l5, l19, 1);
    mv.visitLocalVariable("thisClass", "Ljava/lang/Class;", "Ljava/lang/Class<*>;", l6, l19, 2);
    mv.visitLocalVariable("superClass", "Ljava/lang/Class;", "Ljava/lang/Class<*>;", l0, l19, 3);
    mv.visitLocalVariable("type", "Ljava/lang/invoke/MethodType;", null, l7, l1, 4);
    mv.visitLocalVariable("method", "Ljava/lang/invoke/MethodHandle;", null, l8, l1, 5);
    mv.visitLocalVariable("e", "Ljava/lang/NoSuchMethodException;", null, l10, l11, 4);
    mv.visitLocalVariable("e", "Ljava/lang/IllegalAccessException;", null, l12, l13, 4);
    mv.visitLocalVariable("e", "Ljava/lang/Throwable;", null, l14, l9, 4);
    mv.visitMaxs(5, 6);
    mv.visitEnd();
  }

  // adds init(FilterConfig) filter method for classes that don't have it
  private void addInitMethod() {
    MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "init", "(Ljavax/servlet/FilterConfig;)V", null,
        new String[] { "javax/servlet/ServletException" });
    mv.visitCode();
    Label l0 = new Label();
    Label l1 = new Label();
    Label l2 = new Label();
    mv.visitTryCatchBlock(l0, l1, l2, "javax/servlet/ServletException");
    Label l3 = new Label();
    mv.visitTryCatchBlock(l0, l1, l3, "java/lang/Throwable");
    Label l4 = new Label();
    mv.visitLabel(l4);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKEVIRTUAL, filterClass, "$$injected_initForSession", "(Ljavax/servlet/FilterConfig;)V", false);
    Label l5 = new Label();
    mv.visitLabel(l5);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, filterClass, "$$injected_superInit", "Ljava/lang/invoke/MethodHandle;");
    Label l6 = new Label();
    mv.visitJumpInsn(IFNULL, l6);
    mv.visitLabel(l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, filterClass, "$$injected_superInit", "Ljava/lang/invoke/MethodHandle;");
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact",
        "(L" + filterClass + ";Ljavax/servlet/FilterConfig;)V", false);
    mv.visitLabel(l1);
    mv.visitJumpInsn(GOTO, l6);
    mv.visitLabel(l2);
    mv.visitFrame(F_SAME1, 0, null, 1, new Object[] { "javax/servlet/ServletException" });
    mv.visitVarInsn(ASTORE, 2);
    Label l7 = new Label();
    mv.visitLabel(l7);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitInsn(ATHROW);
    mv.visitLabel(l3);
    mv.visitLineNumber(1, l3);
    mv.visitFrame(F_SAME1, 0, null, 1, new Object[] { "java/lang/Throwable" });
    mv.visitVarInsn(ASTORE, 2);
    Label l8 = new Label();
    mv.visitLabel(l8);
    mv.visitLineNumber(1, l8);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitTypeInsn(INSTANCEOF, "java/lang/Error");
    Label l9 = new Label();
    mv.visitJumpInsn(IFEQ, l9);
    Label l10 = new Label();
    mv.visitLabel(l10);
    mv.visitLineNumber(1, l10);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitTypeInsn(CHECKCAST, "java/lang/Error");
    mv.visitInsn(ATHROW);
    mv.visitLabel(l9);
    mv.visitLineNumber(1, l9);
    mv.visitFrame(F_APPEND, 1, new Object[] { "java/lang/Throwable" }, 0, null);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitTypeInsn(INSTANCEOF, "java/lang/RuntimeException");
    Label l11 = new Label();
    mv.visitJumpInsn(IFEQ, l11);
    Label l12 = new Label();
    mv.visitLabel(l12);
    mv.visitLineNumber(1, l12);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitTypeInsn(CHECKCAST, "java/lang/RuntimeException");
    mv.visitInsn(ATHROW);
    mv.visitLabel(l11);
    mv.visitLineNumber(1, l11);
    mv.visitFrame(F_SAME, 0, null, 0, null);
    mv.visitTypeInsn(NEW, "javax/servlet/ServletException");
    mv.visitInsn(DUP);
    mv.visitLdcInsn("An exception occured while invoking super-class method init");
    mv.visitVarInsn(ALOAD, 2);
    mv.visitMethodInsn(INVOKESPECIAL, "javax/servlet/ServletException", "<init>",
        "(Ljava/lang/String;Ljava/lang/Throwable;)V", false);
    mv.visitInsn(ATHROW);
    mv.visitLabel(l6);
    mv.visitLineNumber(1, l6);
    mv.visitFrame(F_CHOP, 1, null, 0, null);
    mv.visitInsn(RETURN);
    Label l13 = new Label();
    mv.visitLabel(l13);
    mv.visitLocalVariable("this", "L" + filterClass + ";", null, l4, l13, 0);
    mv.visitLocalVariable("config", "Ljavax/servlet/FilterConfig;", null, l4, l13, 1);
    mv.visitLocalVariable("e", "Ljavax/servlet/ServletException;", null, l7, l3, 2);
    mv.visitLocalVariable("e", "Ljava/lang/Throwable;", null, l8, l6, 2);
    mv.visitMaxs(4, 3);
    mv.visitEnd();
  }

  private void addDoFilterWithSuper() {
    MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "doFilter",
        "(Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletResponse;Ljavax/servlet/FilterChain;)V", null,
        new String[] { "java/io/IOException", "javax/servlet/ServletException" });
    mv.visitCode();
    Label l0 = new Label();
    Label l1 = new Label();
    Label l2 = new Label();
    mv.visitTryCatchBlock(l0, l1, l2, "javax/servlet/ServletException");
    mv.visitTryCatchBlock(l0, l1, l2, "java/io/IOException");
    Label l3 = new Label();
    mv.visitTryCatchBlock(l0, l1, l3, "java/lang/Throwable");
    Label l4 = new Label();
    Label l5 = new Label();
    mv.visitTryCatchBlock(l4, l5, l5, null);
    Label l6 = new Label();
    mv.visitLabel(l6);
    mv.visitLineNumber(1, l6);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitVarInsn(ASTORE, 4);
    Label l7 = new Label();
    mv.visitLabel(l7);
    mv.visitLineNumber(1, l7);
    mv.visitVarInsn(ALOAD, 4);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, filterClass, "$$injected_servletContext", "Ljavax/servlet/ServletContext;");
    mv.visitMethodInsn(INVOKESTATIC, filterClass, "$$prepareRequest",
        "(Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletResponse;Ljavax/servlet/ServletContext;)Ljavax/servlet/ServletRequest;",
        false);
    mv.visitVarInsn(ASTORE, 1);
    Label l8 = new Label();
    mv.visitLabel(l8);
    mv.visitLineNumber(1, l8);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, filterClass, "$$injected_servletContext", "Ljavax/servlet/ServletContext;");
    mv.visitMethodInsn(INVOKESTATIC, filterClass, "$$prepareResponse",
        "(Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletResponse;Ljavax/servlet/ServletContext;)Ljavax/servlet/ServletResponse;", false);
    mv.visitVarInsn(ASTORE, 2);
    mv.visitLabel(l4);
    mv.visitLineNumber(1, l4);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, filterClass, "$$injected_superDoFilter", "Ljava/lang/invoke/MethodHandle;");
    Label l9 = new Label();
    mv.visitJumpInsn(IFNULL, l9);
    mv.visitLabel(l0);
    mv.visitLineNumber(1, l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, filterClass, "$$injected_superDoFilter", "Ljava/lang/invoke/MethodHandle;");
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitVarInsn(ALOAD, 3);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact", "(L" + filterClass
        + ";Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletResponse;Ljavax/servlet/FilterChain;)V", false);
    mv.visitLabel(l1);
    mv.visitLineNumber(1, l1);
    mv.visitJumpInsn(GOTO, l9);
    mv.visitLabel(l2);
    mv.visitFrame(F_FULL, 5, new Object[] { filterClass, "javax/servlet/ServletRequest",
        "javax/servlet/ServletResponse", "javax/servlet/FilterChain", "javax/servlet/ServletRequest" }, 1,
        new Object[] { "java/lang/Exception" });
    mv.visitVarInsn(ASTORE, 5);
    Label l10 = new Label();
    mv.visitLabel(l10);
    mv.visitLineNumber(1, l10);
    mv.visitVarInsn(ALOAD, 5);
    mv.visitInsn(ATHROW);
    mv.visitLabel(l3);
    mv.visitLineNumber(1, l3);
    mv.visitFrame(F_SAME1, 0, null, 1, new Object[] { "java/lang/Throwable" });
    mv.visitVarInsn(ASTORE, 5);
    Label l11 = new Label();
    mv.visitLabel(l11);
    mv.visitLineNumber(1, l11);
    mv.visitVarInsn(ALOAD, 5);
    mv.visitTypeInsn(INSTANCEOF, "java/lang/Error");
    Label l12 = new Label();
    mv.visitJumpInsn(IFEQ, l12);
    Label l13 = new Label();
    mv.visitLabel(l13);
    mv.visitLineNumber(1, l13);
    mv.visitVarInsn(ALOAD, 5);
    mv.visitTypeInsn(CHECKCAST, "java/lang/Error");
    mv.visitInsn(ATHROW);
    mv.visitLabel(l12);
    mv.visitLineNumber(1, l12);
    mv.visitFrame(F_APPEND, 1, new Object[] { "java/lang/Throwable" }, 0, null);
    mv.visitVarInsn(ALOAD, 5);
    mv.visitTypeInsn(INSTANCEOF, "java/lang/RuntimeException");
    Label l14 = new Label();
    mv.visitJumpInsn(IFEQ, l14);
    Label l15 = new Label();
    mv.visitLabel(l15);
    mv.visitLineNumber(1, l15);
    mv.visitVarInsn(ALOAD, 5);
    mv.visitTypeInsn(CHECKCAST, "java/lang/RuntimeException");
    mv.visitInsn(ATHROW);
    mv.visitLabel(l14);
    mv.visitLineNumber(1, l14);
    mv.visitFrame(F_SAME, 0, null, 0, null);
    mv.visitTypeInsn(NEW, "javax/servlet/ServletException");
    mv.visitInsn(DUP);
    mv.visitLdcInsn("An exception occured while invoking super-class method doFilter");
    mv.visitVarInsn(ALOAD, 5);
    mv.visitMethodInsn(INVOKESPECIAL, "javax/servlet/ServletException", "<init>",
        "(Ljava/lang/String;Ljava/lang/Throwable;)V", false);
    mv.visitInsn(ATHROW);
    mv.visitLabel(l5);
    mv.visitLineNumber(1, l5);
    mv.visitFrame(F_FULL, 5, new Object[] { filterClass, "javax/servlet/ServletRequest",
        "javax/servlet/ServletResponse", "javax/servlet/FilterChain", "javax/servlet/ServletRequest" }, 1,
        new Object[] { "java/lang/Throwable" });
    mv.visitVarInsn(ASTORE, 6);
    Label l16 = new Label();
    mv.visitLabel(l16);
    mv.visitLineNumber(1, l16);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitVarInsn(ALOAD, 4);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, filterClass, "$$injected_servletContext", "Ljavax/servlet/ServletContext;");
    mv.visitMethodInsn(INVOKESTATIC, filterClass, "$$commitRequest",
        "(Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletContext;)V", false);
    Label l17 = new Label();
    mv.visitLabel(l17);
    mv.visitLineNumber(1, l17);
    mv.visitVarInsn(ALOAD, 6);
    mv.visitInsn(ATHROW);
    mv.visitLabel(l9);
    mv.visitLineNumber(1, l9);
    mv.visitFrame(F_SAME, 0, null, 0, null);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitVarInsn(ALOAD, 4);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, filterClass, "$$injected_servletContext", "Ljavax/servlet/ServletContext;");
    mv.visitMethodInsn(INVOKESTATIC, filterClass, "$$commitRequest",
        "(Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletContext;)V", false);
    Label l18 = new Label();
    mv.visitLabel(l18);
    mv.visitLineNumber(1, l18);
    mv.visitInsn(RETURN);
    Label l19 = new Label();
    mv.visitLabel(l19);
    mv.visitLocalVariable("this", "L" + filterClass + ";", null, l6, l19, 0);
    mv.visitLocalVariable("request", "Ljavax/servlet/ServletRequest;", null, l6, l19, 1);
    mv.visitLocalVariable("response", "Ljavax/servlet/ServletResponse;", null, l6, l19, 2);
    mv.visitLocalVariable("chain", "Ljavax/servlet/FilterChain;", null, l6, l19, 3);
    mv.visitLocalVariable("oldRequest", "Ljavax/servlet/ServletRequest;", null, l7, l19, 4);
    mv.visitLocalVariable("e", "Ljava/lang/Exception;", null, l10, l3, 5);
    mv.visitLocalVariable("e", "Ljava/lang/Throwable;", null, l11, l5, 5);
    mv.visitMaxs(5, 7);
    mv.visitEnd();
  }

  /**
   * This method injects new doFilter method into the class.
   */
  private void addDoFilter() {
    MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "doFilter",
        "(Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletResponse;Ljavax/servlet/FilterChain;)V", null,
        new String[] { "java/io/IOException", "javax/servlet/ServletException" });
    mv.visitCode();
    Label l0 = new Label();
    Label l1 = new Label();
    mv.visitTryCatchBlock(l0, l1, l1, null);
    Label l2 = new Label();
    mv.visitLabel(l2);
    mv.visitLineNumber(1, l2);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitVarInsn(ASTORE, 4);
    Label l3 = new Label();
    mv.visitLabel(l3);
    mv.visitLineNumber(1, l3);
    mv.visitVarInsn(ALOAD, 4);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, filterClass, "$$injected_servletContext", "Ljavax/servlet/ServletContext;");
    mv.visitMethodInsn(INVOKESTATIC, filterClass, "$$prepareRequest",
        "(Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletResponse;Ljavax/servlet/ServletContext;)Ljavax/servlet/ServletRequest;",
        false);
    mv.visitVarInsn(ASTORE, 1);
    Label l4 = new Label();
    mv.visitLabel(l4);
    mv.visitLineNumber(1, l4);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, filterClass, "$$injected_servletContext", "Ljavax/servlet/ServletContext;");
    mv.visitMethodInsn(INVOKESTATIC, filterClass, "$$prepareResponse",
        "(Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletResponse;Ljavax/servlet/ServletContext;)Ljavax/servlet/ServletResponse;", false);
    mv.visitVarInsn(ASTORE, 2);
    mv.visitLabel(l0);
    mv.visitLineNumber(1, l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitVarInsn(ALOAD, 3);
    mv.visitMethodInsn(INVOKEVIRTUAL, filterClass, "$$renamed_doFilter",
        "(Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletResponse;Ljavax/servlet/FilterChain;)V", false);
    Label l5 = new Label();
    mv.visitLabel(l5);
    mv.visitLineNumber(1, l5);
    Label l6 = new Label();
    mv.visitJumpInsn(GOTO, l6);
    mv.visitLabel(l1);
    mv.visitFrame(F_FULL, 5, new Object[] { filterClass, "javax/servlet/ServletRequest",
        "javax/servlet/ServletResponse", "javax/servlet/FilterChain", "javax/servlet/ServletRequest" }, 1,
        new Object[] { "java/lang/Throwable" });
    mv.visitVarInsn(ASTORE, 5);
    Label l7 = new Label();
    mv.visitLabel(l7);
    mv.visitLineNumber(1, l7);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitVarInsn(ALOAD, 4);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, filterClass, "$$injected_servletContext", "Ljavax/servlet/ServletContext;");
    mv.visitMethodInsn(INVOKESTATIC, filterClass, "$$commitRequest",
        "(Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletContext;)V", false);
    Label l8 = new Label();
    mv.visitLabel(l8);
    mv.visitLineNumber(1, l8);
    mv.visitVarInsn(ALOAD, 5);
    mv.visitInsn(ATHROW);
    mv.visitLabel(l6);
    mv.visitLineNumber(1, l6);
    mv.visitFrame(F_SAME, 0, null, 0, null);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitVarInsn(ALOAD, 4);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, filterClass, "$$injected_servletContext", "Ljavax/servlet/ServletContext;");
    mv.visitMethodInsn(INVOKESTATIC, filterClass, "$$commitRequest",
        "(Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletContext;)V", false);
    Label l9 = new Label();
    mv.visitLabel(l9);
    mv.visitLineNumber(1, l9);
    mv.visitInsn(RETURN);
    Label l10 = new Label();
    mv.visitLabel(l10);
    mv.visitLocalVariable("this", "L" + filterClass + ";", null, l2, l10, 0);
    mv.visitLocalVariable("request", "Ljavax/servlet/ServletRequest;", null, l2, l10, 1);
    mv.visitLocalVariable("response", "Ljavax/servlet/ServletResponse;", null, l2, l10, 2);
    mv.visitLocalVariable("chain", "Ljavax/servlet/FilterChain;", null, l2, l10, 3);
    mv.visitLocalVariable("oldRequest", "Ljavax/servlet/ServletRequest;", null, l3, l10, 4);
    mv.visitMaxs(4, 6);
    mv.visitEnd();
  }

  class EnahceInitFilter extends MethodVisitor {

    EnahceInitFilter(MethodVisitor mv) {
      super(ASM5, mv);
    }

    /**
     * Inject call to initForSеssion(config).
     */
    @Override
    public void visitCode() {
      mv.visitVarInsn(ALOAD, 0);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitMethodInsn(INVOKEVIRTUAL, filterClass, "$$injected_initForSession", "(Ljavax/servlet/FilterConfig;)V", false);

      super.visitCode();
    }
  }
}
