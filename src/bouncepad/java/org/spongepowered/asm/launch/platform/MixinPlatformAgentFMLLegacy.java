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
package org.spongepowered.asm.launch.platform;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.launchwrapper.Launch;
import org.spongepowered.asm.launch.GlobalProperties;
import org.spongepowered.asm.launch.platform.container.ContainerHandleURI;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IRemapper;
import org.spongepowered.asm.service.mojang.MixinServiceLaunchWrapper;
import org.spongepowered.asm.util.Constants;

import net.minecraft.launchwrapper.ITweaker;

/**
 * Platform agent for use under FML and LaunchWrapper.
 * 
 * <p>When FML is present we scan containers for the manifest entries which are
 * inhibited by the tweaker, in particular the <tt>FMLCorePlugin</tt> and
 * <tt>FMLCorePluginContainsFMLMod</tt> entries. This is required because FML
 * performs no further processing of containers if they contain a tweaker!</p>
 */
public class MixinPlatformAgentFMLLegacy extends MixinPlatformAgentAbstract implements IMixinPlatformServiceAgent {

    private static final String NEW_LAUNCH_HANDLER_CLASS = "net.minecraftforge.fml.relauncher.FMLLaunchHandler";
    private static final String CLIENT_TWEAKER_TAIL = ".common.launcher.FMLTweaker";
    private static final String SERVER_TWEAKER_TAIL = ".common.launcher.FMLServerTweaker";
    private static final String GETSIDE_METHOD = "side";
    
    private static final String LOAD_CORE_MOD_METHOD = "loadCoreMod";
    private static final String GET_REPARSEABLE_COREMODS_METHOD = "getReparseableCoremods";
    private static final String CORE_MOD_MANAGER_CLASS = "net.minecraftforge.fml.relauncher.CoreModManager";

    private static final String FML_REMAPPER_ADAPTER_CLASS = "org.spongepowered.asm.bridge.RemapperAdapterFML";
    private static final String FML_CMDLINE_COREMODS = "fml.coreMods.load";
    private static final String FML_PLUGIN_WRAPPER_CLASS = "FMLPluginWrapper";
    private static final String FML_CORE_MOD_INSTANCE_FIELD = "coreModInstance";

    private static final String MFATT_FORCELOADASMOD = "ForceLoadAsMod";
    private static final String MFATT_FMLCOREPLUGIN = "FMLCorePlugin";
    private static final String MFATT_COREMODCONTAINSMOD = "FMLCorePluginContainsFMLMod";
    
    private static final String FML_TWEAKER_DEOBF = "FMLDeobfTweaker";
    private static final String FML_TWEAKER_INJECTION = "FMLInjectionAndSortingTweaker";
    private static final String FML_TWEAKER_TERMINAL = "TerminalTweaker";

    /**
     * Coremod classes which have already been bootstrapped, so that we know not
     * to inject them
     */
    private static final Set<String> loadedCoreMods = new HashSet<String>();
    
    /**
     * Discover mods specified to FML on the command line (via JVM arg) at
     * startup so that we know to ignore them
     */
    static {
        for (String cmdLineCoreMod : System.getProperty(MixinPlatformAgentFMLLegacy.FML_CMDLINE_COREMODS, "").split(",")) {
            if (!cmdLineCoreMod.isEmpty()) {
                MixinPlatformAgentAbstract.logger.debug("FML platform agent will ignore coremod {} specified on the command line", cmdLineCoreMod);
                MixinPlatformAgentFMLLegacy.loadedCoreMods.add(cmdLineCoreMod);
            }
        }
    }


    /**
     * If running under FML, we will attempt to inject any coremod specified in
     * the metadata, FML's CoremodManager returns an ITweaker instance which is
     * the "handle" to the injected mod, we will need to proxy calls through to
     * the wrapper. If initialisation fails (for example if we are not running
     * under FML or if an FMLCorePlugin key is not specified in the metadata)
     * then this handle will be null.
     */
    private ITweaker coreModWrapper;
    
    /**
     * Core mod manager class
     */
    private Class<?> clCoreModManager;
    
    /**
     * True if this agent is initialised during pre-injection 
     */
    private boolean initInjectionState;

    @SuppressWarnings("deprecation")
    @Override
    public AcceptResult accept(MixinPlatformManager manager, IContainerHandle handle) {
        if (this.getCoreModManagerClass() == null) {
            return AcceptResult.INVALID;
        }
        
        if (!(handle instanceof ContainerHandleURI) || super.accept(manager, handle) != AcceptResult.ACCEPTED) {
            return AcceptResult.REJECTED;
        }

        return AcceptResult.ACCEPTED;
    }


    @Override
    public String getPhaseProvider() {
        return MixinPlatformAgentFMLLegacy.class.getName() + "$PhaseProvider";
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.launch.IMixinPlatformAgent#prepare()
     */
    @Override
    public void prepare() {
        this.initInjectionState |= MixinPlatformAgentFMLLegacy.isTweakerQueued(MixinPlatformAgentFMLLegacy.FML_TWEAKER_INJECTION);
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.launch.platform.IMixinPlatformAgent#inject()
     */
    @Override
    public void inject() {
        if (this.coreModWrapper != null && this.checkForCoInitialisation()) {
            MixinPlatformAgentAbstract.logger.debug("FML agent is co-initiralising coremod instance {} for {}", this.coreModWrapper, this.handle);
            this.coreModWrapper.injectIntoClassLoader(Launch.classLoader);
        }
    }

    /**
     * Performs a naive check which attempts to discover whether we are pre or
     * post FML's main injection. If we are <i>pre</i>, then we must <b>not</b>
     * manually call <tt>injectIntoClassLoader</tt> on the wrapper because FML
     * will add the wrapper to the tweaker list itself. This occurs when mixin
     * tweaker is loaded explicitly.
     * 
     * <p>In the event that we are <i>post</i> FML's injection, then we must
     * instead call <tt>injectIntoClassLoader</tt> on the wrapper manually.</p>
     * 
     * @return true if FML was already injected
     */
    protected final boolean checkForCoInitialisation() {
        boolean injectionTweaker = MixinPlatformAgentFMLLegacy.isTweakerQueued(MixinPlatformAgentFMLLegacy.FML_TWEAKER_INJECTION);
        boolean terminalTweaker = MixinPlatformAgentFMLLegacy.isTweakerQueued(MixinPlatformAgentFMLLegacy.FML_TWEAKER_TERMINAL);
        if ((this.initInjectionState && terminalTweaker) || injectionTweaker) {
            MixinPlatformAgentAbstract.logger.debug("FML agent is skipping co-init for {} because FML will inject it normally", this.coreModWrapper);
            return false;
        }
        
        return !MixinPlatformAgentFMLLegacy.isTweakerQueued(MixinPlatformAgentFMLLegacy.FML_TWEAKER_DEOBF);
    }

    /**
     * Attempt to get the FML CoreModManager, tries the post-1.8 namespace first
     * and falls back to 1.7.10 if class lookup fails
     */
    private Class<?> getCoreModManagerClass() {
        if (this.clCoreModManager != null) {
            return this.clCoreModManager;
        }
        
        try {
                this.clCoreModManager = Class.forName(GlobalProperties.getString(
                        GlobalProperties.Keys.FML_CORE_MOD_MANAGER, MixinPlatformAgentFMLLegacy.CORE_MOD_MANAGER_CLASS));
        } catch (ClassNotFoundException ex) {
            MixinPlatformAgentAbstract.logger.info("FML platform manager could not load class {}. Proceeding without FML support.",
                    ex.getMessage());
        }
        
        return this.clCoreModManager;
    }

    /**
     * Check whether a tweaker ending with <tt>tweakName</tt> has been enqueued
     * but not yet visited.
     * 
     * @param tweakerName Tweaker name to 
     * @return true if a tweaker with the specified name is queued
     */
    private static boolean isTweakerQueued(String tweakerName) {
        for (String tweaker : GlobalProperties.<List<String>>get(MixinServiceLaunchWrapper.BLACKBOARD_KEY_TWEAKCLASSES)) {
            if (tweaker.endsWith(tweakerName)) {
                return true;
            }
        }
        return false;
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.launch.platform.IMixinPlatformServiceAgent
     *      #init()
     */
    @Override
    public void init() {
    }

    public static void injectRemapper(IRemapper remapper) {
        try {
            MixinEnvironment defaultEnv = MixinEnvironment.getDefaultEnvironment();
            if ((boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment")) {
                //current.setOption(MixinEnvironment.Option.DISABLE_REFMAP, true);
                defaultEnv.setOption(MixinEnvironment.Option.REFMAP_REMAP, true);
                defaultEnv.setOption(MixinEnvironment.Option.REFMAP_REMAP_RESOURCE, true);
                defaultEnv.setOption(MixinEnvironment.Option.REFMAP_REMAP_SOURCE_ENV, true);
                //current.setOption(MixinEnvironment.Option.OBFUSCATION_TYPE, true);
                System.setProperty("mixin.env.refMapRemapping", "build/createSrg2Mcp/output.tsrg");
                System.setProperty("mixin.env.refMapRemappingEnv", "mcp");
                //System.setProperty("mixin.env.obf", "deobf");
            }
            defaultEnv.getRemappers().add(remapper);

        } catch (Exception ex) {
            MixinPlatformAgentAbstract.logger.debug("Failed instancing FML remapper adapter, things will probably go horribly for notch-obf'd mods!");
        }
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.launch.platform.MixinPlatformAgentAbstract
     *      #getSideName()
     */
    @Override
    public String getSideName() {
        // Using this method first prevents us from accidentally loading FML
        // classes too early when using the tweaker in dev
        List<ITweaker> tweakerList = GlobalProperties.<List<ITweaker>>get(MixinServiceLaunchWrapper.BLACKBOARD_KEY_TWEAKS);
        if (tweakerList == null) {
            return null;
        }
        for (ITweaker tweaker : tweakerList) {
            if (tweaker.getClass().getName().endsWith(MixinPlatformAgentFMLLegacy.SERVER_TWEAKER_TAIL)) {
                return Constants.SIDE_SERVER;
            } else if (tweaker.getClass().getName().endsWith(MixinPlatformAgentFMLLegacy.CLIENT_TWEAKER_TAIL)) {
                return Constants.SIDE_CLIENT;
            }
        }

        return MixinPlatformAgentAbstract.invokeStringMethod(Launch.classLoader, MixinPlatformAgentFMLLegacy.NEW_LAUNCH_HANDLER_CLASS,
                MixinPlatformAgentFMLLegacy.GETSIDE_METHOD);
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.launch.platform.IMixinPlatformServiceAgent
     *      #getMixinContainers()
     */
    @Override
    public Collection<IContainerHandle> getMixinContainers() {
        return null;
    }



}
