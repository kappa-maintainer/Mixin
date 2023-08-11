package org.spongepowered.asm.mixin.extensibility;

import org.spongepowered.asm.service.IMixinService;

import java.util.List;

public interface IMixinProcessor {

    /**
     * @return the current MixinService that provided this processor
     */
    IMixinService getMixinService();

    /**
     * @return queued mixin configurations in the current environment
     */
    List<IMixinConfig> getMixinConfigs();

    /**
     * @return queued pending mixin configurations that has not been processed yet
     */
    List<IMixinConfig> getPendingMixinConfigs();

}
