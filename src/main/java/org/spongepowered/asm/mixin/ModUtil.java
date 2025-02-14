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
package org.spongepowered.asm.mixin;

import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.injection.selectors.ISelectorContext;

public final class ModUtil {
    public static final String OWNER_DECORATOR = "mixinOwner";
    public static final String UNKNOWN_OWNER = "unknown-owner";

    public static String owner(IMixinConfig config) {
        return owner(config, UNKNOWN_OWNER);
    }

    public static String owner(IMixinConfig config, String defaultValue) {
        return getDecoration(config, OWNER_DECORATOR, defaultValue);
    }

    public static String owner(ISelectorContext context) {
        return getDecoration(getConfig(context), OWNER_DECORATOR, UNKNOWN_OWNER);
    }

    static IMixinConfig getConfig(ISelectorContext context) {
        return context.getMixin().getMixin().getConfig();
    }

    static <T> T getDecoration(IMixinConfig config, String key, T defaultValue) {
        if (config.hasDecoration(key)) {
            return config.getDecoration(key);
        } else {
            return defaultValue;
        }
    }
}