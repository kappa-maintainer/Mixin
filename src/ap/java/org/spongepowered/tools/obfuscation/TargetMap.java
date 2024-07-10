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
package org.spongepowered.tools.obfuscation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.spongepowered.tools.obfuscation.mirror.TypeHandle;
import org.spongepowered.tools.obfuscation.mirror.TypeReference;

import com.google.common.io.Files;
import com.google.gson.*;

/**
 * Serialisable map of classes to their associated mixins, used so that we can
 * pass target information for supermixins from one compiler session to another
 */
public final class TargetMap extends HashMap<TypeReference, Set<TypeReference>> {

    private static final long serialVersionUID = 1L;
    private static final Gson GSON = new Gson();
    
    /**
     * Session ID, used to identify the temp file
     */
    private final String sessionId;

    /**
     * Create a new TargetMap with a session ID based on the current system time
     */
    private TargetMap() {
        this(String.valueOf(System.currentTimeMillis()));
    }
    
    /**
     * Create a TargetMap with the specified session ID
     * 
     * @param sessionId session id
     */
    private TargetMap(String sessionId) {
        this.sessionId = sessionId;
    }
    
    /**
     * Get the session ID
     */
    public String getSessionId() {
        return this.sessionId;
    }

    /**
     * Register target classes for the specified mixin
     * 
     * @param mixin mixin to add targets for
     */
    void registerTargets(AnnotatedMixin mixin) {
        this.registerTargets(mixin.getTargets(), mixin.getHandle());
    }

    /**
     * Register target classes for the supplied mixin
     * 
     * @param targets List of targets
     * @param mixin Mixin class
     */
    void registerTargets(List<TypeHandle> targets, TypeHandle mixin) {
        for (TypeHandle target : targets) {
            this.addMixin(target, mixin);
        }
    }
    
    /**
     * Register the specified mixin against the specified target
     * 
     * @param target Target class
     * @param mixin Mixin class
     */
    void addMixin(TypeHandle target, TypeHandle mixin) {
        this.addMixin(target.getReference(), mixin.getReference());
    }

    /**
     * Register the specified mixin against the specified target
     * 
     * @param target Target class
     * @param mixin Mixin class
     */
    void addMixin(String target, String mixin) {
        this.addMixin(new TypeReference(target), new TypeReference(mixin));
    }
    
    /**
     * Register the specified mixin against the specified target
     * 
     * @param target Target class
     * @param mixin Mixin class
     */
    void addMixin(TypeReference target, TypeReference mixin) {
        Set<TypeReference> mixins = this.getMixinsFor(target);
        mixins.add(mixin);
    }
    
    /**
     * Get mixin classes which target the specified class
     * 
     * @param target Target class
     * @return Collection of mixins registered as targetting the specified class
     */
    Collection<TypeReference> getMixinsTargeting(TypeHandle target) {
        return this.getMixinsTargeting(target.getReference());
    }
    
    /**
     * Get mixin classes which target the specified class
     * 
     * @param target Target class
     * @return Collection of mixins registered as targetting the specified class
     */
    Collection<TypeReference> getMixinsTargeting(TypeReference target) {
        return Collections.<TypeReference>unmodifiableCollection(this.getMixinsFor(target));
    }
    
    /**
     * Get mixin set for specified class
     * 
     * @param target Target class
     * @return Set of mixins registered as targetting the specified class
     */
    private Set<TypeReference> getMixinsFor(TypeReference target) {
        Set<TypeReference> mixins = this.get(target);
        if (mixins == null) {
            mixins = new HashSet<TypeReference>();
            this.put(target, mixins);
        }
        return mixins;
    }
    
    /**
     * Read upstream library mixins from a file
     * 
     * @param file File to read from
     * @throws IOException if an error occurs whilst reading the file
     */
    public void readImports(File file) throws IOException {
        if (!file.isFile()) {
            return;
        }
        
        for (String line : Files.readLines(file, Charset.defaultCharset())) {
            String[] parts = line.split("\t");
            if (parts.length == 2) {
                this.addMixin(parts[1], parts[0]);
            }
        }
    }

    /**
     * Write this target map to temporary session file
     * 
     * @param temp Set "delete on exit" for the file
     */
    public void write(boolean temp) {
        JsonObject jsonObject = new JsonObject();

        for (Entry<TypeReference, Set<TypeReference>> entry : this.entrySet()) {
            final JsonArray array = new JsonArray();

            for (TypeReference reference : entry.getValue()) {
                array.add(new JsonPrimitive(reference.getName()));
            }

            jsonObject.add(entry.getKey().getName(), array);
        }

        String json = GSON.toJson(jsonObject);

        File sessionFile = TargetMap.getSessionFile(this.sessionId);
        if (temp) {
            sessionFile.deleteOnExit();
        }

        try (FileOutputStream outputStream = new FileOutputStream(sessionFile)) {
            outputStream.write(json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    /**
     * Attemp to deserialise a TargetMap from the specified file
     * 
     * @param sessionFile File to read
     * @return deserialised map or null if deserialisation failed
     */
    private static TargetMap read(File sessionFile) {
        try (Reader reader = new BufferedReader(new FileReader(sessionFile))) {
            final JsonObject jsonObject = GSON.fromJson(reader, JsonObject.class);
            final TargetMap targetMap = new TargetMap();

            for (Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                JsonArray array = entry.getValue().getAsJsonArray();

                for (JsonElement element : array) {
                    targetMap.addMixin(entry.getKey(), element.getAsString());
                }
            }

            return targetMap;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
    
    /**
     * Create a TargetMap for the specified session id. Generate new map if
     * the session id is invalid or the file cannot be read. Session ID can be
     * null
     * 
     * @param sessionId session to deserialise, can be null to create new
     *      session
     * @return new TargetMap
     */
    public static TargetMap create(String sessionId) {
        if (sessionId != null) {
            File sessionFile = TargetMap.getSessionFile(sessionId);
            if (sessionFile.exists()) {
                TargetMap map = TargetMap.read(sessionFile);
                if (map != null) {
                    return map;
                }
            }
        }
        
        return new TargetMap();
    }

    private static File getSessionFile(String sessionId) {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        return new File(tempDir, String.format("mixin-targetdb-%s.tmp", sessionId));
    }

}
