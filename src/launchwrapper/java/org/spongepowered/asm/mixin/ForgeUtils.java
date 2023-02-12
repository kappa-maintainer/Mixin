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

// Reference: https://github.com/FabricMC/fabric-loader/blob/2a378f1c563b6ec96ae6620a278ecd23fa09da0f/src/main/java/net/fabricmc/loader/impl/launch/FabricMixinBootstrap.java

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.minecraft.launchwrapper.Launch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.transformer.Config;

import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

public final class ForgeUtils {

    private static final Logger logger = LogManager.getLogger();

    public static final String KEY_MOD_ID = "fabric-modId";

    private ForgeUtils() {}

    public static void init() {
        Map<String, String> jarNameToModid = createJarNameToModidMap();

        for (Config config : Mixins.getConfigs()) {
            URL resource = Launch.classLoader.getResource(config.getName());
            if (resource != null) {
                String jarName = getJarNameFromResourceUrl(resource);
                if (jarName != null) {
                    String modid = jarNameToModid.get(jarName);
                    if (modid != null) {
                        config.getConfig().decorate(KEY_MOD_ID, modid);
                    }
                }
            }
        }
    }

    private static Map<String, String> createJarNameToModidMap() {
        Map<String, String> map = new HashMap<>();
        try {
            for (URL url : Collections.list(Launch.classLoader.getResources("mcmod.info"))) {
                String fileName = getJarNameFromResourceUrl(url);
                if (fileName != null) {
                    List<ModMetadata> metas = parseMcmodInfo(url);
                    if (!metas.isEmpty()) {
                        String modid = metas.get(0).modid;
                        if (modid != null) {
                            map.put(fileName, modid);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to construct jar name -> modid map, mod names will not be shown in errors.");
        }
        return map;
    }

    private static List<ModMetadata> parseMcmodInfo(URL url) {
        try {
            JsonElement root = new Gson().fromJson(new InputStreamReader(url.openStream()), JsonElement.class);
            if (root.isJsonArray()) {
                return Arrays.asList(new Gson().fromJson(new InputStreamReader(url.openStream()), ModMetadata[].class));
            } else {
                return Arrays.asList(new Gson().fromJson(new InputStreamReader(url.openStream()), ModMetadata.class));
            }
        } catch (Exception e) {
            logger.warn("Failed to parse mcmod.info at " + url + ": " + e);
        }
        return Arrays.asList();
    }

    private static String getJarNameFromResourceUrl(URL url) {
        if (url.getPath().contains("!/")) {
            String filePath = url.getPath().split("!/")[0];
            String[] parts = filePath.split("/");
            if (parts.length != 0) {
                return parts[parts.length - 1];
            }
        }
        return null;
    }

    private static class ModMetadata {
        String modid;
    }

}
