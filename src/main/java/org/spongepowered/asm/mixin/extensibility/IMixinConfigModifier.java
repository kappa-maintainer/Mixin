package org.spongepowered.asm.mixin.extensibility;

import org.spongepowered.asm.mixin.MixinEnvironment;

import java.util.List;

public interface IMixinConfigModifier {
    void injectConfig(String config);
    List<String> modifyMixinClasses(List<String> mixinClasses);
    List<String> modifyMixinClassesClient(List<String> mixinClasses);
    List<String> modifyMixinClassesServer(List<String> mixinClasses);
    MixinEnvironment modifyEnvironment(MixinEnvironment environment);
    boolean shouldAddMixinConfig(boolean current);
}
