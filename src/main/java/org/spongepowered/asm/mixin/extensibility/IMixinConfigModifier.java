package org.spongepowered.asm.mixin.extensibility;

import org.spongepowered.asm.mixin.MixinEnvironment;

import java.util.List;

public interface IMixinConfigModifier {
    default void injectConfig(String config){}
    default List<String> modifyMixinClasses(List<String> mixinClasses){return mixinClasses;}
    default List<String> modifyMixinClassesClient(List<String> mixinClasses){return mixinClasses;}
    default List<String> modifyMixinClassesServer(List<String> mixinClasses){return mixinClasses;}
    default MixinEnvironment modifyEnvironment(MixinEnvironment environment){return environment;}
    default boolean shouldAddMixinConfig(boolean current){return current;}
}
