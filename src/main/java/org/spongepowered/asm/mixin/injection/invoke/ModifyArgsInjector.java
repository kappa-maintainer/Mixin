/*
 * This file is part of Mixin, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.asm.mixin.injection.invoke;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;
import org.spongepowered.asm.mixin.injection.InjectionPoint.RestrictTargetLevel;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes.InjectionNode;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.injection.struct.Target.Extension;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.util.Bytecode;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * A bytecode injector which allows a single argument of a chosen method call to
 * be altered. For details see javadoc for {@link ModifyArgs &#64;ModifyArgs}.
 */
public class ModifyArgsInjector extends InvokeInjector {
    
    private static final String ARGS_NAME = "org/spongepowered/asm/mixin/injection/invoke/arg/Args";
    private static final String FACTORY_DESC = "(I)L" + ARGS_NAME + ";"; 
    private static final String PUSH_DESC = "(Ljava/lang/Object;)L" + ARGS_NAME + ";";
    private static final String GET_DESC = "(I)Ljava/lang/Object;";

    private static Printer printer = new Textifier();
    private static TraceMethodVisitor mp = new TraceMethodVisitor(printer);

    public static String insnToString(AbstractInsnNode insn){
        insn.accept(mp);
        StringWriter sw = new StringWriter();
        printer.print(new PrintWriter(sw));
        printer.getText().clear();
        return sw.toString();
    }

    /**
     * @param info Injection info
     */
    public ModifyArgsInjector(InjectionInfo info) {
        super(info, "@ModifyArgs");
        
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.injection.invoke.InvokeInjector
     *      #checkTarget(org.spongepowered.asm.mixin.injection.struct.Target)
     */
    @Override
    protected void checkTarget(Target target) {
        this.checkTargetModifiers(target, false);
    }
    
    @Override
    protected void inject(Target target, InjectionNode node) {
        this.checkTargetForNode(target, node, RestrictTargetLevel.ALLOW_ALL);
        super.inject(target, node);
    }

    /**
     * Do the injection
     */
    @Override
    protected void injectAtInvoke(Target target, InjectionNode node) {
        MethodInsnNode targetMethod = (MethodInsnNode)node.getCurrentTarget();
        
        Type[] args = Type.getArgumentTypes(targetMethod.desc);
        if (args.length == 0) {
            throw new InvalidInjectionException(this.info, "@ModifyArgs injector " + this + " targets a method invocation "
                    + targetMethod.name + targetMethod.desc + " with no arguments!");
        }
        
        boolean withArgs = this.verifyTarget(target);

        InsnList insns = new InsnList();
        Extension extraStack = target.extendStack().add(1);
        
        this.packArgs(insns, targetMethod, args);
        
        if (withArgs) {
            extraStack.add(target.arguments);
            Bytecode.loadArgs(target.arguments, insns, target.isStatic ? 0 : 1);
        }
        
        this.invokeHandler(insns);
        this.unpackArgs(insns, args);
        
        extraStack.apply();
        target.insns.insertBefore(targetMethod, insns);
    }

    private boolean verifyTarget(Target target) {
        String shortDesc = String.format("(L%s;)V", ARGS_NAME);
        if (!this.methodNode.desc.equals(shortDesc)) {
            String targetDesc = Bytecode.changeDescriptorReturnType(target.method.desc, "V");
            String longDesc = String.format("(L%s;%s", ARGS_NAME, targetDesc.substring(1));
            
            if (this.methodNode.desc.equals(longDesc)) {
                return true;
            }
            
            throw new InvalidInjectionException(this.info, "@ModifyArgs injector " + this + " has an invalid signature "
                    + this.methodNode.desc + ", expected " + shortDesc + " or " + longDesc);
        }
        return false;
    }

    private void packArgs(InsnList insns, MethodInsnNode targetMethod, Type[] args) {
        if (args.length > 5) {
            insns.add(new IntInsnNode(Opcodes.BIPUSH, args.length));
        } else {
            insns.add(new InsnNode(Opcodes.ICONST_0 + args.length));
        }
        insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ARGS_NAME, "of", FACTORY_DESC, false));
        
        for (int i = args.length - 1; i >= 0; i--) {
            insns.add(new InsnNode(Opcodes.SWAP));
            if (Bytecode.getBoxingType(args[i]) != null) {
                String box = Bytecode.getBoxingType(args[i]);
                insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC, box, "valueOf", "(" + args[i].getDescriptor() + ")L" + box + ";"));
            }
            insns.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Object"));
            insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, ARGS_NAME, "push", PUSH_DESC, false));
        }
        
        insns.add(new InsnNode(Opcodes.DUP));
        
        if (!this.isStatic) {
            insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
            insns.add(new InsnNode(Opcodes.SWAP));
        }
    }

    private void unpackArgs(InsnList insns, Type[] args) {
        for (int i = 0; i < args.length; i++) {
            if (i < args.length - 1) {
                insns.add(new InsnNode(Opcodes.DUP));
            }
            if (i > 5) {
                insns.add(new IntInsnNode(Opcodes.BIPUSH, i));
            } else {
                insns.add(new InsnNode(Opcodes.ICONST_0 + i));
            }
            insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, ARGS_NAME, "get", GET_DESC, false));
            if (Bytecode.getBoxingType(args[i]) != null) {
                String boxingType = Bytecode.getBoxingType(args[i]);
                String unboxingMethod = Bytecode.getUnboxingMethod(args[i]);
                insns.add(new TypeInsnNode(Opcodes.CHECKCAST, boxingType));
                insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, boxingType, unboxingMethod, "()" + args[i].getDescriptor(), false));
            } else {
                insns.add(new TypeInsnNode(Opcodes.CHECKCAST, args[i].getInternalName()));
            }
            if (i < args.length - 1) {
                if (args[i].getSize() == 1) {
                    insns.add(new InsnNode(Opcodes.SWAP));
                } else {
                    insns.add(new InsnNode(Opcodes.DUP2_X1));
                    insns.add(new InsnNode(Opcodes.POP2));
                }
            }
        }
    }
}