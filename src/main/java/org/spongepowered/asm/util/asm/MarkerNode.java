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
package org.spongepowered.asm.util.asm;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.LabelNode;

/**
 * A label node used as a marker in the bytecode. Does not actually visit the
 * label when visited.
 */
public class MarkerNode extends LabelNode {
    
    /**
     * Marks the end of the initialiser in a constructor
     */
    public static final int INITIALISER_TAIL = 1;
    
    /**
     * Marks the start of the body in a constructor
     */
    public static final int BODY_START = 2;
    
    /**
     * The type for this marker
     */
    public final int type;

    public MarkerNode(int type) {
        super(null);
        this.type = type;
    }

    @Override
    public void accept(MethodVisitor methodVisitor) {
        // Noop
    }

}
