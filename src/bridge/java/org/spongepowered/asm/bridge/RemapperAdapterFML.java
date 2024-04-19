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
package org.spongepowered.asm.bridge;

import java.util.function.Function;

import org.objectweb.asm.commons.Remapper;
import org.spongepowered.asm.mixin.extensibility.IRemapper;

/**
 * Remapper adapter which remaps using FML's deobfuscating remapper
 */
public final class RemapperAdapterFML extends RemapperAdapter {

    private final Function<String, String> unmap;
    
    private RemapperAdapterFML(org.objectweb.asm.commons.Remapper remapper, Function<String, String> unmap) {
        super(remapper);
        this.logger.info("Initialised Mixin FML Remapper Adapter with {}", remapper);
        this.unmap = unmap;
    }

    @Override
    public String unmap(String typeName) {
        try {
            return this.unmap.apply(typeName);
        } catch (Exception ex) {
            return typeName;
        }
    }

    /**
     * Factory method
     */
    public static IRemapper create(Remapper remapper, Function<String, String> unmap) {
        return new RemapperAdapterFML(remapper, unmap);
    }


}
