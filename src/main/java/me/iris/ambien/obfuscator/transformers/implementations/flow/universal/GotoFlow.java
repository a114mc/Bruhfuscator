package me.iris.ambien.obfuscator.transformers.implementations.flow.universal;

import me.iris.ambien.obfuscator.builders.InstructionBuilder;
import me.iris.ambien.obfuscator.builders.InstructionModifier;
import me.iris.ambien.obfuscator.utilities.kek.UnicodeDictionary;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Modifier;
import java.util.concurrent.ThreadLocalRandom;

import static me.iris.ambien.obfuscator.transformers.implementations.flow.Flow.classMethodsMap;

public class GotoFlow {

    public static void gotoFlow() {
        classMethodsMap.forEach((classWrapper, methods) -> {
            transform(classWrapper.getNode());
        });
    }

    private static void transform(ClassNode node) {
        if (Modifier.isInterface(node.access)) return;

        UnicodeDictionary dictionary = new UnicodeDictionary(10);

        for (FieldNode field : node.fields) {
            dictionary.addUsed(field.name);
        }

        String fieldName = dictionary.get();
        boolean setupField = false;

        for (MethodNode method : node.methods) {
            if (method.instructions.size() == 0) continue;

            InstructionModifier modifier = new InstructionModifier();

            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof JumpInsnNode) {
                    JumpInsnNode jumpInsn = (JumpInsnNode) instruction;
                    if (jumpInsn.getOpcode() == Opcodes.GOTO) {
                        InstructionBuilder builder = new InstructionBuilder();
                        builder.fieldInsn(Opcodes.GETSTATIC, node.name, fieldName, "I");
                        builder.jump(Opcodes.IFLT, jumpInsn.label);

                        boolean pop = false;
                        int randomInt = ThreadLocalRandom.current().nextInt(0, 5);
                        switch (randomInt) {
                            case 0:
                                builder.number(ThreadLocalRandom.current().nextInt());
                                pop = true;
                                break;
                            case 1:
                                builder.ldc(ThreadLocalRandom.current().nextLong());
                                break;
                            case 2:
                                builder.insn(Opcodes.ACONST_NULL);
                                pop = true;
                                break;
                            case 3:
                                builder.ldc(ThreadLocalRandom.current().nextFloat());
                                pop = true;
                                break;
                            case 4:
                                builder.ldc(ThreadLocalRandom.current().nextDouble());
                                break;
                        }

                        if (pop) {
                            builder.insn(Opcodes.POP);
                        } else {
                            builder.insn(Opcodes.POP2);
                        }

                        builder.insn(Opcodes.ACONST_NULL);
                        builder.insn(Opcodes.ATHROW);

                        modifier.replace(jumpInsn, builder.getList());
                        setupField = true;
                    }
                } else if (instruction instanceof VarInsnNode) {
                    VarInsnNode varInsn = (VarInsnNode) instruction;
                    switch (varInsn.getOpcode()) {
                        case Opcodes.ILOAD:
                        case Opcodes.LLOAD:
                        case Opcodes.FLOAD:
                        case Opcodes.DLOAD:
                        case Opcodes.ALOAD: {
                            LabelNode label = new LabelNode();
                            method.maxLocals = method.maxLocals + (varInsn.getOpcode() == Opcodes.LLOAD || varInsn.getOpcode() == Opcodes.DLOAD ? 2 : 1);

                            int index = method.maxLocals;

                            InstructionBuilder builder = new InstructionBuilder();
                            builder.varInsn(varInsn.getOpcode() + 33, index);
                            builder.varInsn(varInsn.getOpcode(), index);
                            builder.fieldInsn(Opcodes.GETSTATIC, node.name, fieldName, "I");
                            builder.jump(Opcodes.IFLT, label);

                            builder.fieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
                            builder.ldc(ThreadLocalRandom.current().nextLong());
                            builder.methodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(J)V", false);

                            builder.insn(Opcodes.ACONST_NULL);
                            builder.insn(Opcodes.ATHROW);
                            builder.label(label);

                            modifier.append(varInsn, builder.getList());
                            setupField = true;
                            break;
                        }
                        case Opcodes.ISTORE:
                        case Opcodes.LSTORE:
                        case Opcodes.FSTORE:
                        case Opcodes.DSTORE:
                        case Opcodes.ASTORE: {
                            InstructionBuilder builder = new InstructionBuilder();
                            builder.varInsn(varInsn.getOpcode() - 33, varInsn.var);

                            if (varInsn.getOpcode() == Opcodes.DSTORE || varInsn.getOpcode() == Opcodes.LSTORE) {
                                builder.insn(Opcodes.POP2);
                            } else {
                                builder.insn(Opcodes.POP);
                            }

                            modifier.append(varInsn, builder.getList());
                            break;
                        }
                    }
                }
            }

            modifier.apply(method);
        }

        if (setupField) {
            FieldNode field = new FieldNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, fieldName, "I", null, ThreadLocalRandom.current().nextInt(Integer.MIN_VALUE, 0));
            node.fields.add(field);
        }
    }
}
