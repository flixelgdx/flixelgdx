/*
 * MIT License
 *
 * Copyright (c) 2026 stringdotjar
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.flixelgdx.gradle.shader.worker;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * The shader contract of the framework as the plugin sees it: the preamble text and the numbers that go with it.
 *
 * <p>Both come from flixelgdx-gpu. The plugin's build copies the preamble file and extracts the constants of
 * {@code FlixelWgsl} into a properties file, and this class reads those two resources, so nothing about the contract
 * is written down a second time.
 */
public final class Contract {

  private static final String PREAMBLE_KEY = "PREAMBLE";
  private static final String ROOT = "/org/flixelgdx/gradle/shader/";

  final String preamble;
  final String fragmentEntry;
  final String vertexEntry;
  final String defaultVertexEntry;
  final int preambleLines;
  final int frameworkGroup;
  final int shaderGroup;
  final int shaderBinding;
  final int globalsSize;

  private Contract(String preamble, Map<?, ?> p) {
    this.preamble = preamble;
    this.fragmentEntry = get(p, "FRAGMENT_ENTRY");
    this.vertexEntry = get(p, "VERTEX_ENTRY");
    this.defaultVertexEntry = get(p, "DEFAULT_VERTEX_ENTRY");
    this.frameworkGroup = Integer.parseInt(get(p, "FRAMEWORK_GROUP"));
    this.shaderGroup = Integer.parseInt(get(p, "SHADER_GROUP"));
    this.shaderBinding = Integer.parseInt(get(p, "SHADER_BINDING"));
    this.globalsSize = Integer.parseInt(get(p, "GLOBALS_SIZE"));
    // The assembled text is preamble + '\n' + user source, so user line 1 is this many lines further down.
    this.preambleLines = SourceText.countNewlines(preamble) + 1;
  }

  /**
   * Loads the contract from the plugin's resources.
   *
   * @return The contract.
   * @throws IllegalStateException if a resource is missing.
   */
  public static Contract load() {
    String preamble = read("flixel_preamble.wgsl");
    Properties props = new Properties();
    try (InputStream in = Contract.class.getResourceAsStream(ROOT + "shader_contract.properties")) {
      if (in == null) {
        throw new IllegalStateException("The plugin is missing shader_contract.properties.");
      }
      props.load(in);
    } catch (IOException e) {
      throw new IllegalStateException("Could not read shader_contract.properties.", e);
    }
    return new Contract(preamble, props);
  }

  /**
   * Rebuilds a contract from the map made by {@link #toMap()}.
   *
   * <p>The Gradle process reads the resources and hands the values to the validator process this way, so the
   * validator does not depend on where the plugin's resources sit on its class path.
   *
   * @param values The map.
   * @return The contract.
   */
  public static Contract fromMap(Map<String, String> values) {
    return new Contract(get(values, PREAMBLE_KEY), values);
  }

  /**
   * Flattens the contract into a map of strings.
   *
   * @return A new map holding the preamble under {@value #PREAMBLE_KEY} and every constant under its name.
   */
  public Map<String, String> toMap() {
    Map<String, String> map = new LinkedHashMap<>();
    map.put(PREAMBLE_KEY, preamble);
    map.put("FRAGMENT_ENTRY", fragmentEntry);
    map.put("VERTEX_ENTRY", vertexEntry);
    map.put("DEFAULT_VERTEX_ENTRY", defaultVertexEntry);
    map.put("FRAMEWORK_GROUP", Integer.toString(frameworkGroup));
    map.put("SHADER_GROUP", Integer.toString(shaderGroup));
    map.put("SHADER_BINDING", Integer.toString(shaderBinding));
    map.put("GLOBALS_SIZE", Integer.toString(globalsSize));
    return map;
  }

  /**
   * Builds the full module text for a shader file, exactly like {@code FlixelWgsl.assemble(...)}.
   *
   * @param userSource The text of the shader file.
   * @return The preamble, a line break, and the file.
   */
  String assemble(String userSource) {
    return new StringBuilder(preamble.length() + userSource.length() + 1)
        .append(preamble).append('\n').append(userSource).toString();
  }

  private static String get(Map<?, ?> values, String key) {
    Object value = values.get(key);
    if (value == null) {
      throw new IllegalStateException("The shader contract has no value for " + key + ".");
    }
    return value.toString();
  }

  private static String read(String name) {
    try (InputStream in = Contract.class.getResourceAsStream(ROOT + name)) {
      if (in == null) {
        throw new IllegalStateException("The plugin is missing " + name + ".");
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("Could not read " + name + ".", e);
    }
  }
}
