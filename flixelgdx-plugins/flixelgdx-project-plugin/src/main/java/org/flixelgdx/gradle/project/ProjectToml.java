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
package org.flixelgdx.gradle.project;

import org.gradle.api.GradleException;
import org.tomlj.Toml;
import org.tomlj.TomlArray;
import org.tomlj.TomlParseError;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlTable;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads the text of a {@code project.toml} into a checked {@link ProjectModel}.
 *
 * <p>Only the keys listed in the plugin README are read. Any other table or key, such as the {@code [installed]}
 * table that tools write, is ignored, so the file can grow without breaking older versions of the plugin.
 *
 * @see ProjectValidator
 */
public final class ProjectToml {

  private static final List<String> PLATFORMS = List.of("desktop", "html5", "android");

  private ProjectToml() {}

  /**
   * Parses and checks the text of a {@code project.toml}.
   *
   * @param text The file contents.
   * @return The checked model, with defaults filled in for every optional value that is missing.
   * @throws GradleException if the text is not valid TOML, a required value is missing, or a value is wrong.
   */
  public static ProjectModel parse(String text) {
    TomlParseResult toml = Toml.parse(text);
    if (toml.hasErrors()) {
      StringBuilder message = new StringBuilder("project.toml is not valid TOML:");
      for (TomlParseError error : toml.errors()) {
        message.append("\n  ").append(error);
      }
      throw new GradleException(message.toString());
    }

    String title = requiredString(toml, "app.title");
    String company = requiredString(toml, "app.company");
    String version = requiredString(toml, "app.version");
    String packageName = requiredString(toml, "app.package");
    String language = string(toml, "app.language", "java");
    ProjectValidator.checkName("app.title", title);
    ProjectValidator.checkName("app.company", company);
    ProjectValidator.checkVersion("app.version", version);
    ProjectValidator.checkPackage("app.package", packageName);
    ProjectValidator.checkLanguage("app.language", language);

    int width = positive(toml, "window.width", 640);
    int height = positive(toml, "window.height", 360);
    int framerate = positive(toml, "window.framerate", 60);
    boolean vsync = bool(toml, "window.vsync", true);
    boolean fullscreen = bool(toml, "window.fullscreen", false);

    boolean hasRender = toml.get("render") != null;
    boolean renderResolution = true;
    int renderWidth = 0;
    int renderHeight = 0;
    if (hasRender) {
      Object resolution = toml.get("render.resolution");
      if (resolution instanceof Boolean enabled) {
        renderResolution = enabled;
      } else if (resolution != null) {
        int[] size = size(resolution);
        renderWidth = size[0];
        renderHeight = size[1];
      }
    }
    float renderScale = 1f;
    if (hasRender && toml.get("render.scale") != null) {
      renderScale = scale(toml.get("render.scale"));
    }
    boolean renderSmooth = bool(toml, "render.smooth", true);
    boolean transparent = bool(toml, "render.transparent", false);

    return new ProjectModel(width, height, framerate, renderWidth, renderHeight, renderScale, title, company,
        version, packageName, language, platforms(toml), vsync, fullscreen, hasRender, renderResolution,
        renderSmooth, transparent);
  }

  private static List<String> platforms(TomlTable toml) {
    Object value = toml.get("platforms");
    if (value == null) {
      return List.of("desktop");
    }
    if (!(value instanceof TomlArray array)) {
      throw new GradleException("Invalid platforms in project.toml: it must be a list such as "
          + "[\"desktop\", \"html5\"].");
    }
    List<String> platforms = new ArrayList<>();
    for (int i = 0; i < array.size(); i++) {
      if (!(array.get(i) instanceof String name)) {
        throw new GradleException("Invalid platforms in project.toml: entry " + (i + 1) + " is not a string.");
      }
      if (name.equals("ios")) {
        throw new GradleException("Invalid platforms in project.toml: \"ios\" is not supported yet. "
            + "Use \"desktop\", \"html5\" or \"android\".");
      }
      if (!PLATFORMS.contains(name)) {
        throw new GradleException("Invalid platforms in project.toml: \"" + name + "\" is not a platform. "
            + "Use \"desktop\", \"html5\" or \"android\".");
      }
      if (!platforms.contains(name)) {
        platforms.add(name);
      }
    }
    return List.copyOf(platforms);
  }

  private static String requiredString(TomlTable toml, String key) {
    String value = string(toml, key, null);
    if (value == null) {
      throw new GradleException("Missing " + key + " in project.toml. Add it under the [" + key.substring(0,
          key.indexOf('.')) + "] table, for example: " + key.substring(key.indexOf('.') + 1) + " = \"...\".");
    }
    return value;
  }

  private static String string(TomlTable toml, String key, String fallback) {
    Object value = toml.get(key);
    if (value == null) {
      return fallback;
    }
    if (!(value instanceof String text)) {
      throw typeError(key, "a string in quotes");
    }
    return text;
  }

  private static boolean bool(TomlTable toml, String key, boolean fallback) {
    Object value = toml.get(key);
    if (value == null) {
      return fallback;
    }
    if (!(value instanceof Boolean flag)) {
      throw typeError(key, "true or false");
    }
    return flag;
  }

  private static int positive(TomlTable toml, String key, int fallback) {
    Object value = toml.get(key);
    if (value == null) {
      return fallback;
    }
    int number = whole(key, value);
    if (number <= 0) {
      throw new GradleException("Invalid " + key + " in project.toml: it must be greater than 0, not " + number
          + ".");
    }
    return number;
  }

  private static int whole(String key, Object value) {
    if (!(value instanceof Long number)) {
      throw typeError(key, "a whole number");
    }
    if (number < Integer.MIN_VALUE || number > Integer.MAX_VALUE) {
      throw new GradleException("Invalid " + key + " in project.toml: " + number + " is out of range.");
    }
    return number.intValue();
  }

  private static int[] size(Object value) {
    String key = "render.resolution";
    if (!(value instanceof TomlArray array) || array.size() != 2) {
      throw typeError(key, "a list of two numbers such as [1280, 720] (or false to turn it off)");
    }
    int[] size = new int[2];
    for (int i = 0; i < 2; i++) {
      size[i] = whole(key, array.get(i));
      if (size[i] < 0) {
        throw new GradleException("Invalid " + key + " in project.toml: " + size[i] + " is negative, but 0 or "
            + "more is needed (0 means the window size).");
      }
    }
    return size;
  }

  private static float scale(Object value) {
    double number;
    if (value instanceof Long whole) {
      number = whole;
    } else if (value instanceof Double decimal) {
      number = decimal;
    } else {
      throw typeError("render.scale", "a number such as 1.0");
    }
    if (!(number > 0) || Double.isInfinite(number)) {
      throw new GradleException("Invalid render.scale in project.toml: it must be greater than 0, not " + number
          + ".");
    }
    return (float) number;
  }

  private static GradleException typeError(String key, String expected) {
    return new GradleException("Invalid " + key + " in project.toml: it must be " + expected + ".");
  }
}
