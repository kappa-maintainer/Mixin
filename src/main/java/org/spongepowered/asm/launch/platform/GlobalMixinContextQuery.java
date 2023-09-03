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

import org.spongepowered.asm.launch.GlobalProperties;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.extensibility.MixinContextQuery;

import java.util.ArrayList;
import java.util.List;

public final class GlobalMixinContextQuery {

    public static final String MIXIN_LOCATION_DECORATOR = "mixinLocation";
    public static final String MIXIN_OWNER_DECORATOR = "mixinOwner";
    public static final String UNKNOWN_LOCATION = "unknown-location";
    public static final String UNKNOWN_OWNER = "unknown-owner";
    
    private static boolean init = false;
    private static final Object EMPTY_VALUE = new Object();
    
    public static void init() {
        if (!init) {
            GlobalProperties.put(GlobalProperties.Keys.CLEANROOM_GLOBAL_MIXIN_CONTEXT_QUERY, new GlobalMixinContextQuery());
            init = true;
        }
    }

    public static String location(IMixinConfig config) {
        return location(config, UNKNOWN_LOCATION);
    }

    public static String location(IMixinConfig config, String defaultValue) {
        init();
        return GlobalProperties.<GlobalMixinContextQuery>get(GlobalProperties.Keys.CLEANROOM_GLOBAL_MIXIN_CONTEXT_QUERY)
                .getLocation(config, defaultValue);
    }

    public static String owner(IMixinConfig config) {
        return location(config, UNKNOWN_OWNER);
    }

    public static String owner(IMixinConfig config, String defaultValue) {
        init();
        return GlobalProperties.<GlobalMixinContextQuery>get(GlobalProperties.Keys.CLEANROOM_GLOBAL_MIXIN_CONTEXT_QUERY)
                .getOwner(config, defaultValue);
    }
    
    private final List<MixinContextQuery> queries = new ArrayList<MixinContextQuery>();
    
    private GlobalMixinContextQuery() { }

    public void add(MixinContextQuery query) {
        this.queries.add(query);
    }

    /**
     * Grabs the location descriptor for where the mixin was described from
     * @param config mixin config instance
     * @return location of where the mixin and its config was described
     */
    public String getLocation(IMixinConfig config) {
        return getLocation(config, UNKNOWN_LOCATION);
    }

    /**
     * Grabs the location descriptor for where the mixin was described from
     * @param config mixin config instance
     * @param defaultValue what will be returned if no suitable locations were found
     * @return location of where the mixin and its config was described
     */
    public String getLocation(IMixinConfig config, String defaultValue) {
        String value = config.getDecoration(MIXIN_LOCATION_DECORATOR);
        if (value != null) {
            return value == EMPTY_VALUE ? defaultValue : value;
        }
        List<MixinContextQuery> queries = this.queries;
        for (int i = 0; i < queries.size(); i++) {
            value = queries.get(i).getLocation(config);
            if (value != null) {
                break;
            }
        }
        config.decorate(MIXIN_LOCATION_DECORATOR, value == null ? EMPTY_VALUE : value);
        return value == null ? defaultValue : value;
    }

    /**
     * Grabs the mixin owner or something extremely similar of the  that described the mixin and its config
     * @param config mixin config instance
     * @return mixin owner that described the mixin and its config
     */
    public String getOwner(IMixinConfig config) {
        return getLocation(config, UNKNOWN_OWNER);
    }

    /**
     * Grabs the mixin owner or something extremely similar of the  that described the mixin and its config
     * @param config mixin config instance
     * @param defaultValue what will be returned if no suitable mixin owners were found
     * @return mixin owner that described the mixin and its config
     */
    public String getOwner(IMixinConfig config, String defaultValue) {
        String value = config.getDecoration(MIXIN_OWNER_DECORATOR);
        if (value != null) {
            return value == EMPTY_VALUE ? defaultValue : value;
        }
        List<MixinContextQuery> queries = this.queries;
        for (int i = 0; i < queries.size(); i++) {
            value = queries.get(i).getOwner(config);
            if (value != null) {
                break;
            }
        }
        config.decorate(MIXIN_OWNER_DECORATOR, value == null ? EMPTY_VALUE : value);
        return value == null ? defaultValue : value;
    }

}
