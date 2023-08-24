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
