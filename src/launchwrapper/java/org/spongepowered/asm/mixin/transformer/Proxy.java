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
package org.spongepowered.asm.mixin.transformer;

import net.minecraft.launchwrapper.IClassTransformer;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinProcessor;
import org.spongepowered.asm.service.ILegacyClassTransformer;
import org.spongepowered.asm.service.MixinService;

import java.util.ArrayList;
import java.util.List;

/**
 * Proxy transformer for the mixin transformer. These transformers are used
 * to allow the mixin transformer to be re-registered in the transformer
 * chain at a later stage in startup without having to fully re-initialise
 * the mixin transformer itself. Only the latest proxy to be instantiated
 * will actually provide callbacks to the underlying mixin transformer.
 */
public final class Proxy implements IClassTransformer, ILegacyClassTransformer {
    
    /**
     * All existing proxies
     */
    public static List<Proxy> proxies = new ArrayList<>();
    
    /**
     * Actual mixin transformer instance
     */
    public static MixinTransformer transformer = new MixinTransformer();
    
    /**
     * True if this is the active proxy, newer proxies disable their older
     * siblings
     */
    private boolean isActive = true;
    
    public Proxy() {
        for (Proxy proxy : Proxy.proxies) {
            proxy.isActive = false;
        }
        
        Proxy.proxies.add(this);
        MixinService.getService().getLogger("mixin").debug("Adding new mixin transformer proxy #{}", Proxy.proxies.size());
    }
    
    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (this.isActive) {
            return Proxy.transformer.transformClassBytes(name, transformedName, basicClass);
        }
        
        return basicClass;
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public boolean isDelegationExcluded() {
        return true;
    }

    @Override
    public byte[] transformClassBytes(String name, String transformedName, byte[] basicClass) {
        if (this.isActive) {
            return Proxy.transformer.transformClassBytes(name, transformedName, basicClass);
        }
        
        return basicClass;
    }
    
    /**
     * Tells the Mixin subsystem to process any Mixin configs that were added since we last checked. Mixins applied this way still cannot transform classes that are already loaded.
     * <p>A common use-case is to apply configs targeting classes that do not exist in the classloader before Mixin first initializes.</p>
     */
    public static void processLateConfigs() {
        final IMixinProcessor processor = transformer.getProcessor();
        final ILogger logger = MixinService.getService().getLogger("mixin");
        if (processor.getPendingMixinConfigs().isEmpty()) {
            logger.warn("Proxy::processLateConfigs was called with no pending configs.");
            return;
        }
        if (!(processor instanceof MixinProcessor)) {
            logger.error("Proxy.transformer.processor is not a MixinProcessor. Could not apply late configs.");
            return;
        }
        // NOTE: If anyone has a better way of doing this, please change this to use it. I'm just making a jankless version of the reflection-based method we've been using for years.
        final MixinProcessor mixinProcessor = (MixinProcessor) processor;
        mixinProcessor.selectConfigs(MixinEnvironment.getCurrentEnvironment());
        mixinProcessor.prepareConfigs(MixinEnvironment.getCurrentEnvironment(), mixinProcessor.extensions);
    }
}
