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
package org.spongepowered.asm.mixin.gen;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.throwables.InvalidAccessorException;
import org.spongepowered.asm.mixin.transformer.ClassInfo;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.Bytecode;

/**
 * Generator for instance field getters
 */
public class AccessorGeneratorFieldGetter extends AccessorGeneratorField {
    /**
     * True if the accessor method is decorated with {@link Mutable}
     */
    private boolean mutable;
    public AccessorGeneratorFieldGetter(AccessorInfo info) {
        super(info);
    }
    @Override
    public void validate() {

        super.validate();

        ClassInfo.Method method = this.info.getClassInfo().findMethod(this.info.getMethod());
        this.mutable = method.isDecoratedMutable();
        if (this.mutable || !Bytecode.hasFlag(this.targetField, Opcodes.ACC_FINAL)) {
            return;
        }

        if (this.info.getMixin().getOption(MixinEnvironment.Option.DEBUG_VERBOSE)) {
            MixinService.getService().getLogger("mixin").warn("{} for final field {}::{} is not @Mutable. This won't work on newer Java, automatically stripping...", this.info,
                    ((MixinTargetContext)this.info.getMixin()).getTarget(), this.targetField.name);
        }
    }
    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.gen.AccessorGenerator#generate()
     */
    @Override
    public MethodNode generate() {
        if (this.mutable) {
            this.targetField.access &= ~Opcodes.ACC_FINAL;
        } else if ((this.targetField.access & Opcodes.ACC_FINAL) != 0) {
            this.targetField.access &= ~Opcodes.ACC_FINAL;
        }
        MethodNode method = this.createMethod(this.targetType.getSize(), this.targetType.getSize());
        if (!this.targetIsStatic) {
            method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        }
        int opcode = this.targetIsStatic ? Opcodes.GETSTATIC : Opcodes.GETFIELD;
        method.instructions.add(new FieldInsnNode(opcode, this.info.getTargetClassNode().name, this.targetField.name, this.targetField.desc));
        method.instructions.add(new InsnNode(this.targetType.getOpcode(Opcodes.IRETURN)));
        return method;
    }
    
}
