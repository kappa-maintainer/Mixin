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
package org.spongepowered.asm.obfuscation;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.MixinEnvironment.Option;
import org.spongepowered.asm.mixin.extensibility.IRemapper;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * This remapper together with
 * {@link org.spongepowered.asm.mixin.refmap.RemappingReferenceMapper} is
 * designed to address a problem with mixins when "deobfCompile" dependencies
 * are used in a project which is using newer MCP mappings than the ones the
 * imported dependency was compiled with.
 *
 * <p>Before now, refMaps in deobfCompile dependencies had to be disabled
 * because the obfuscated mappings were no use in a development environment.
 * However there existed no "mcp-to-different-version-of-mcp" mappings to use
 * instead.</p>
 *
 * <p>This class leverages the fact that mappings are provided into the
 * environment by GradleStart and consumes SRG mappings in the imported refMaps
 * and converts them on-the-fly using srg-to-mcp mappings. This allows refMaps
 * to be normalised to the current MCP environment.</p>
 *
 * <p>Note that this class takes a naïve approach to remapping on the basis that
 * searge names are unique and can thus be remapped with a straightforward dumb
 * string replacement. Whilst the input environment and mappings are
 * customisable via the appropriate environment vars, this fact should be taken
 * into account if a different mapping environment is to be used.</p>
 *
 * <p>All lookups are straightforward string replacements using <em>all</em>
 * values in the map, this basically means this is probably pretty slow, but I
 * don't care because the number of mappings processed is usually pretty small
 * (few hundred at most) and this is only used in dev where we don't actually
 * care about speed. Some performance is gained (approx 10ms per lookup) by
 * caching the transformed descriptors.</p>
 */
public class FMLLegacyDevRemapper implements IRemapper {

    /**
     * This is the default GradleStart-injected system property which tells us
     * the location of the srg-to-mcp mappings
     */
    private static final String DEFAULT_RESOURCE_PATH_PROPERTY = "net.minecraftforge.gradle.GradleStart.srg.srg-mcp";

    /**
     * The default environment name to map <em>from</em>
     */
    private static final String DEFAULT_MAPPING_ENV = "searge";

    /**
     * Logger
     */
    private static final Logger logger = LogManager.getLogger("mixin");

    /**
     * Loaded srgs, stored as a mapping of filename to mappings. Global cache so
     * that we only need to load each mapping file once.
     */
    private static final Map<String, Map<String, String>> srgs = new HashMap<String, Map<String,String>>();

    /**
     * The loaded mappings, retrieved from {@link #srgs} by filename
     */
    private final Map<String, String> mappings;

    /**
     * Cache of transformed descriptor mappings.
     */
    private final Map<String, String> descCache = new HashMap<String, String>();

    public FMLLegacyDevRemapper(MixinEnvironment env) {
        String resource = FMLLegacyDevRemapper.getResource(env);
        this.mappings = FMLLegacyDevRemapper.loadSrgs(resource);
        FMLLegacyDevRemapper.logger.info("Initialised Legacy FML Dev Remapper");
    }

    @Override
    public String mapMethodName(String owner, String name, String desc) {
        return mappings.getOrDefault(name, name);
    }

    @Override
    public String mapFieldName(String owner, String name, String desc) {
        return mappings.getOrDefault(name, name);
    }

    @Override
    public String map(String typeName) {
        return typeName;
    }

    @Override
    public String unmap(String typeName) {
        return typeName;
    }

    @Override
    public String mapDesc(String desc) {
        String remapped = descCache.get(desc);
        if(remapped == null) {
            remapped = desc;
            for (Entry<String, String> entry : this.mappings.entrySet()) {
                remapped = remapped.replace(entry.getKey(), entry.getValue());
            }
            descCache.put(desc, remapped);
        }
        return remapped;
    }

    @Override
    public String unmapDesc(String desc) {
        // TODO
        throw new UnsupportedOperationException();
    }

    /**
     * Read srgs from the specified file resource. The mappings are cached
     * internally so this will only read the file the first time it is called
     * with a particulare filename.
     *
     * @param fileName srg file to read
     * @return srgs read from file or empty map if the file could not be read
     */
    private static Map<String, String> loadSrgs(String fileName) {
        if (FMLLegacyDevRemapper.srgs.containsKey(fileName)) {
            return FMLLegacyDevRemapper.srgs.get(fileName);
        }

        final Map<String, String> map = new HashMap<String, String>();
        FMLLegacyDevRemapper.srgs.put(fileName, map);

        File file = new File(fileName);
        if (!file.isFile()) {
            return map;
        }

        try {
            Files.readLines(file, Charsets.UTF_8, new LineProcessor<Object>() {

                @Override
                public Object getResult() {
                    return null;
                }

                @Override
                public boolean processLine(String line) throws IOException {
                    if (Strings.isNullOrEmpty(line) || line.startsWith("#")) {
                        return true;
                    }
                    int fromPos = 0, toPos = 0;
                    if ((toPos = line.startsWith("MD: ") ? 2 : line.startsWith("FD: ") ? 1 : 0) > 0) {
                        String[] entries = line.substring(4).split(" ", 4);
                        map.put(
                                entries[fromPos].substring(entries[fromPos].lastIndexOf('/') + 1),
                                entries[toPos].substring(entries[toPos].lastIndexOf('/') + 1)
                        );
                    }
                    return true;
                }
            });
        } catch (IOException ex) {
            FMLLegacyDevRemapper.logger.warn("Could not read input SRG file: {}", fileName);
            FMLLegacyDevRemapper.logger.catching(ex);
        }

        return map;
    }

    private static String getResource(MixinEnvironment env) {
        String resource = env.getOptionValue(Option.REFMAP_REMAP_RESOURCE);
        return Strings.isNullOrEmpty(resource) ? System.getProperty(FMLLegacyDevRemapper.DEFAULT_RESOURCE_PATH_PROPERTY) : resource;
    }

    private static String getMappingEnv(MixinEnvironment env) {
        String resource = env.getOptionValue(Option.REFMAP_REMAP_SOURCE_ENV);
        return Strings.isNullOrEmpty(resource) ? FMLLegacyDevRemapper.DEFAULT_MAPPING_ENV : resource;
    }
}
