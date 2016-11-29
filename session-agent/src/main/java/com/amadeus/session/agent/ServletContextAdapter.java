package com.amadeus.session.agent;

import static com.amadeus.session.agent.SessionAgent.debug;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ASM5;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;

import java.util.EventListener;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

/**
 * This class modifies method <code>addListener()</code> of a
 * <code>javax.servlet.ServletContext</code> implementation. The modification
 * allows registering of <code>HttpSessionListeners</code> and
 * <code>HttpSessionAttributeListeners</code> for an instance of ServletContext
 * and, then, the session management can subsequently call the listeners when
 * needed.
 * <p>
 * We only update <code>addListener()</code> that work on {@link EventListener}.
 * There are two other <code>addListener()</code> methods, one taking
 * {@link String} and the other taking {@link Class} as parameter, but as the
 * behavior of the application server is hidden there, we don't intercept them.
 * It is recommended to use intercept listeners functionality to discover
 * listeners registered in that way.
 */
public class ServletContextAdapter extends ClassVisitor {

  String className;
  boolean addedStaticInit;

  /**
   * Standard constructor
   *
   * @param cv
   *          ASM class visitor
   */
  public ServletContextAdapter(ClassVisitor cv) {
    super(ASM5, cv);
  }

  @Override
  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    super.visit(version, access, name, signature, superName, interfaces);
    this.className = name;
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    if ((access & (ACC_STATIC | ACC_PUBLIC)) == ACC_PUBLIC
        && "addListener".equals(name)
        && ("(Ljava/lang/Object;)V".equals(desc) || "(Ljava/util/EventListener;)V".equals(desc))) {
      debug("modifying addListener(...) method for %s", className);
      MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
      return new EnahceAddListener(mv);
    }
    // Enhance static initializer if present
    if ("<clinit>".equals(name) && !addedStaticInit) {
      MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
      return new EnhanceStaticInit(mv);
    }
    return super.visitMethod(access, name, desc, signature, exceptions);
  }

  @Override
  public void visitEnd() {
    ServletContextHelpers.methods(className, cv, !addedStaticInit);
    super.visitEnd();
  }

  class EnhanceStaticInit extends MethodVisitor {

    EnhanceStaticInit(MethodVisitor mv) {
      super(ASM5, mv);
    }

    @Override
    public void visitCode() {
      ServletContextHelpers.staticInit(className, mv);
      addedStaticInit = true;
      super.visitCode();
    }
  }

  /**
   * This class modifies <code>javax.servlet.ServletContext#addListener()</code>
   * method by inserting call to equivalent of
   * <code>com.amadeus.session.servlet.SessionHelpers#onAddListener</code> as
   * the first statement.
   */
  class EnahceAddListener extends MethodVisitor {

    EnahceAddListener(MethodVisitor mv) {
      super(ASM5, mv);
    }

    @Override
    public void visitCode() {
      mv.visitVarInsn(ALOAD, 0); // NOSONAR load argument 0 is more meaningful
                                 // than load arg ZERO
      mv.visitVarInsn(ALOAD, 1); // NOSONAR load argument 1 is more meaningful
                                 // than load arg ONE
      mv.visitMethodInsn(INVOKESTATIC, className, "$$onAddListener", "(Ljava/lang/Object;Ljava/lang/Object;)V", false);

      super.visitCode();
    }
  }
}
