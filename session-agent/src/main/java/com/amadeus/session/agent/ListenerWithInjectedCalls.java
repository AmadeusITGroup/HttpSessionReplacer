package com.amadeus.session.agent;

import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ASM5;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

/**
 * This class enhances listeners so they can intercept calls during events. This
 * is used when servlet container supports only Servlet 2.5 standard.
 * <p>
 * The class instruments only the following methods:
 * <p>
 * <ul>
 * <li><code>HttpSessionListener.sessionCreated(HttpSessionEvent)</code> and
 * <li><code>HttpSessionAttributeListener.attributeAdded(HttpSessionBindingEvent)</code>
 * </ul>
 */
class ListenerWithInjectCalls extends ClassVisitor {
  private String className;
  private boolean addedStaticInit;

  ListenerWithInjectCalls(ClassVisitor cv) {
    super(ASM5, cv);
  }

  @Override
  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    className = name;
    super.visit(version, access, name, signature, superName, interfaces);
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    if (("sessionCreated".equals(name) && "(Ljavax/servlet/http/HttpSessionEvent;)V".equals(desc))
        || ("attributeAdded".equals(name) && "(Ljavax/servlet/http/HttpSessionBindingEvent;)V".equals(desc))) {
      return new ModifyListnerMethod(mv);
    }
    if ("<clinit>".equals(name) && !addedStaticInit) {
      return new EnhanceStaticInit(mv);
    }
    return mv;
  }

  @Override
  public void visitEnd() {
    ListenerHelpers.methods(className, cv, !addedStaticInit);
    super.visitEnd();
  }

  class EnhanceStaticInit extends MethodVisitor {

    EnhanceStaticInit(MethodVisitor mv) {
      super(ASM5, mv);
    }

    @Override
    public void visitCode() {
      ListenerHelpers.staticInit(className, mv);
      addedStaticInit = true;
      super.visitCode();
    }
  }

  /**
   * This class injects call to SessionHelpers.interceptListener(listener,
   * event) into the method.
   */
  class ModifyListnerMethod extends MethodVisitor {

    ModifyListnerMethod(MethodVisitor mv) {
      super(ASM5, mv);
    }

    /**
     * Inject call to interceptHttpListner.
     */
    @Override
    public void visitCode() {
      visitVarInsn(ALOAD, 0);
      visitVarInsn(ALOAD, 1);
      visitMethodInsn(INVOKESTATIC, className, "$$interceptHttpListener",
          "(Ljava/lang/Object;Ljavax/servlet/http/HttpSessionEvent;)V", false);

      super.visitCode();
    }
  }
}
