package dev.sobatista.ascent.meter;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Rewrites a class's bytecode to count what it executes — the modern stand-in for the faculty's BIT tool. It inserts
 * a call to {@link Counters#bb()} at the start of every basic block (method entry and each label) and a call to
 * {@link Counters#load()} / {@link Counters#store()} before every local-variable load / store. The inserted calls are
 * stack-neutral, and ASM recomputes the stack-map frames, so the result verifies.
 */
public final class InstructionCounter {
    static final String COUNTERS = "dev/sobatista/ascent/meter/Counters";

    private InstructionCounter() {}

    public static byte[] instrument(byte[] original) {
        ClassReader reader = new ClassReader(original);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (mv == null || (access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0 || "<clinit>".equals(name)) {
                    return mv;
                }
                return new CountingMethodVisitor(mv);
            }
        };
        reader.accept(visitor, ClassReader.SKIP_FRAMES);
        return writer.toByteArray();
    }

    private static final class CountingMethodVisitor extends MethodVisitor {
        CountingMethodVisitor(MethodVisitor mv) {
            super(Opcodes.ASM9, mv);
        }

        @Override
        public void visitCode() {
            super.visitCode();
            count("bb"); // the method-entry basic block
        }

        @Override
        public void visitLabel(Label label) {
            super.visitLabel(label);
            count("bb"); // each label starts a new basic block
        }

        @Override
        public void visitVarInsn(int opcode, int varIndex) {
            if (opcode >= Opcodes.ILOAD && opcode <= Opcodes.ALOAD) {
                count("load");
            } else if (opcode >= Opcodes.ISTORE && opcode <= Opcodes.ASTORE) {
                count("store");
            }
            super.visitVarInsn(opcode, varIndex);
        }

        private void count(String method) {
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, COUNTERS, method, "()V", false);
        }
    }
}
