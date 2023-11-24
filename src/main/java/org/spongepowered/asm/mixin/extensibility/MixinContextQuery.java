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
package org.spongepowered.asm.mixin.extensibility;

import org.spongepowered.asm.launch.GlobalProperties;
import org.spongepowered.asm.launch.platform.GlobalMixinContextQuery;

/**
 * Mixin Context Query. Formerly known as ConfigDecorator.
 * Allows query of mixin location + mixin owners from their config alone.
 * Originally from Fabric's mixin fork, UniMix had also worked on it.
 * Now it can be generified to different environments.
 */
public abstract class MixinContextQuery {

    protected MixinContextQuery() {
        GlobalMixinContextQuery.init();
        GlobalProperties.<GlobalMixinContextQuery>get(GlobalProperties.Keys.CLEANROOM_GLOBAL_MIXIN_CONTEXT_QUERY).add(this);
    }

    /**
     * Grabs the mixin owner or something extremely similar of the mod that described the mixin and its config
     * @param config mixin config instance
     * @return mixin owner that described the mixin and its config
     */
    public abstract String getOwner(IMixinConfig config);

    /**
     * Grabs the location descriptor for where the mixin was described from
     * @param config mixin config instance
     * @return location of where the mixin and its config was described
     */
    public abstract String getLocation(IMixinConfig config);

}