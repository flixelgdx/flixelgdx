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
import org.jetbrains.annotations.Nullable;
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
 * <p>A profile is a named set of overrides, written as a {@code [profile.<name>]} table. When one is selected, every
 * value is looked up in the profile first and in the rest of the file second, like a sticky note on a recipe that
 * changes only the lines it mentions:
 *
 * <pre>{@code
 * [window]
 * fullscreen = true
 *
 * [profile.dev]
 * app.mode = "debug"
 * window.fullscreen = false
 * defines.enable_cheats = true
 * }</pre>
 *
 * <p>{@code app.mode} is the one key that only a profile may set, so a build without a profile always starts in
 * release mode.
 *
 * @see ProjectValidator
 */
public final class ProjectToml {

  /** The build number used when {@code app.build} is left out. */
  public static final int DEFAULT_BUILD = 1;

  /** The highest build number allowed, which is the highest {@code versionCode} Google Play accepts. */
  public static final int MAX_BUILD = 2_100_000_000;

  /** The table that holds the profiles, one {@code [profile.<name>]} table each. */
  public static final String PROFILES = "profile";

  private static final List<String> PLATFORMS = List.of("desktop", "html5", "android");

  private ProjectToml() {}

  /**
   * Parses and checks the text of a {@code project.toml} without a profile.
   *
   * @param text The file contents.
   * @return The checked model, with defaults filled in for every optional value that is missing.
   * @throws GradleException if the text is not valid TOML, a required value is missing, or a value is wrong.
   */
  public static ProjectModel parse(String text) {
    return parse(text, null);
  }

  /**
   * Parses and checks the text of a {@code project.toml} with a profile's overrides applied.
   *
   * @param text The file contents.
   * @param profile The name of the profile to apply, or {@code null} or an empty string for none.
   * @return The checked model, with the profile's values applied and defaults filled in for every optional value that
   *     is still missing.
   * @throws GradleException if the text is not valid TOML, the profile does not exist or changes {@code app.package},
   *     a required value is missing, or a value is wrong.
   */
  public static ProjectModel parse(String text, @Nullable String profile) {
    TomlParseResult toml = Toml.parse(text);
    if (toml.hasErrors()) {
      StringBuilder message = new StringBuilder("project.toml is not valid TOML:");
      for (TomlParseError error : toml.errors()) {
        message.append("\n  ").append(error);
      }
      throw new GradleException(message.toString());
    }
    String name = profile == null ? "" : profile.strip();
    Lookup values = new Lookup(toml, name.isEmpty() ? null : profileTable(toml, name), name);

    // The mode can only come from a profile, so a build without one always starts in release mode and a debug build
    // cannot be shipped by accident.
    if (toml.get("app.mode") != null) {
      throw new GradleException("Invalid app.mode in project.toml: the mode can only be set inside a profile, such "
          + "as app.mode = \"debug\" under [profile.dev], so a build without a profile always starts in release mode.");
    }
    String mode = string(values, "app.mode", "");
    if (!mode.isEmpty()) {
      ProjectValidator.checkMode(values.where("app.mode"), mode);
    }

    String title = requiredString(values, "app.title");
    String company = requiredString(values, "app.company");
    String version = requiredString(values, "app.version");
    String packageName = requiredString(values, "app.package");
    String language = string(values, "app.language", "java");
    int build = positive(values, "app.build", DEFAULT_BUILD);
    if (build > MAX_BUILD) {
      throw new GradleException("Invalid " + values.where("app.build") + " in project.toml: it must be " + MAX_BUILD
          + " or less, not " + build + ", because that is the highest version code Android accepts.");
    }
    ProjectValidator.checkName(values.where("app.title"), title);
    ProjectValidator.checkName(values.where("app.company"), company);
    ProjectValidator.checkVersion(values.where("app.version"), version);
    ProjectValidator.checkPackage(values.where("app.package"), packageName);
    ProjectValidator.checkLanguage(values.where("app.language"), language);

    int width = positive(values, "window.width", 640);
    int height = positive(values, "window.height", 360);
    int framerate = positive(values, "window.framerate", 60);
    boolean vsync = bool(values, "window.vsync", true);
    boolean fullscreen = bool(values, "window.fullscreen", false);

    boolean hasRender = values.get("render") != null;
    boolean renderResolution = true;
    int renderWidth = 0;
    int renderHeight = 0;
    if (hasRender) {
      Object resolution = values.get("render.resolution");
      if (resolution instanceof Boolean enabled) {
        renderResolution = enabled;
      } else if (resolution != null) {
        int[] size = size(values, resolution);
        renderWidth = size[0];
        renderHeight = size[1];
      }
    }
    float renderScale = 1f;
    if (hasRender && values.get("render.scale") != null) {
      renderScale = scale(values, values.get("render.scale"));
    }
    boolean renderSmooth = bool(values, "render.smooth", true);
    boolean transparent = bool(values, "render.transparent", false);

    List<ProjectDefine> defines = ProjectDefines.parse(toml, values.profile(), name);
    return new ProjectModel(width, height, framerate, build, renderWidth, renderHeight, renderScale, title, company,
        version, packageName, language, platforms(values), defines, name, mode, vsync, fullscreen, hasRender,
        renderResolution, renderSmooth, transparent);
  }

  /**
   * Lists the profiles a file has.
   *
   * @param toml The parsed file.
   * @return The profile names in the order they are written, or an empty list when there are none.
   * @throws GradleException if {@code profile} is not a table.
   */
  static List<String> profileNames(TomlTable toml) {
    Object profiles = toml.get(PROFILES);
    if (profiles == null) {
      return List.of();
    }
    if (!(profiles instanceof TomlTable table)) {
      throw new GradleException("Invalid profile in project.toml: it must hold tables such as [profile.dev].");
    }
    return List.copyOf(table.keySet());
  }

  private static TomlTable profileTable(TomlTable toml, String name) {
    List<String> names = profileNames(toml);
    // A list path instead of a dotted key, so a name with a dot in it is still read as one name.
    Object table = names.contains(name) ? toml.get(List.of(PROFILES, name)) : null;
    if (table == null) {
      throw new GradleException("The profile '" + name + "' was selected with -P" + ProjectPlugin.PROFILE_PROPERTY
          + ", but project.toml has no [profile." + name + "] table. "
          + (names.isEmpty() ? "It has no profiles yet." : "Its profiles are: " + String.join(", ", names) + "."));
    }
    if (!(table instanceof TomlTable profile)) {
      throw new GradleException("Invalid profile." + name + " in project.toml: it must be a table.");
    }
    if (profile.get("app.package") != null) {
      throw new GradleException("Invalid profile." + name + ".app.package in project.toml: the package cannot change "
          + "per profile, because the game's code imports the generated Project class from it. For a separate "
          + "Android app ID, use applicationIdSuffix in the Android module's build file.");
    }
    return profile;
  }

  private static List<String> platforms(Lookup values) {
    Object value = values.get("platforms");
    if (value == null) {
      return List.of("desktop");
    }
    String key = values.where("platforms");
    if (!(value instanceof TomlArray array)) {
      throw new GradleException("Invalid " + key + " in project.toml: it must be a list such as "
          + "[\"desktop\", \"html5\"].");
    }
    List<String> platforms = new ArrayList<>();
    for (int i = 0; i < array.size(); i++) {
      if (!(array.get(i) instanceof String name)) {
        throw new GradleException("Invalid " + key + " in project.toml: entry " + (i + 1) + " is not a string.");
      }
      if (name.equals("ios")) {
        throw new GradleException("Invalid " + key + " in project.toml: \"ios\" is not supported yet. "
            + "Use \"desktop\", \"html5\" or \"android\".");
      }
      if (!PLATFORMS.contains(name)) {
        throw new GradleException("Invalid " + key + " in project.toml: \"" + name + "\" is not a platform. "
            + "Use \"desktop\", \"html5\" or \"android\".");
      }
      if (!platforms.contains(name)) {
        platforms.add(name);
      }
    }
    return List.copyOf(platforms);
  }

  private static String requiredString(Lookup values, String key) {
    String value = string(values, key, null);
    if (value == null) {
      throw new GradleException("Missing " + key + " in project.toml. Add it under the [" + key.substring(0,
          key.indexOf('.')) + "] table, for example: " + key.substring(key.indexOf('.') + 1) + " = \"...\".");
    }
    return value;
  }

  private static String string(Lookup values, String key, String fallback) {
    Object value = values.get(key);
    if (value == null) {
      return fallback;
    }
    if (!(value instanceof String text)) {
      throw typeError(values.where(key), "a string in quotes");
    }
    return text;
  }

  private static boolean bool(Lookup values, String key, boolean fallback) {
    Object value = values.get(key);
    if (value == null) {
      return fallback;
    }
    if (!(value instanceof Boolean flag)) {
      throw typeError(values.where(key), "true or false");
    }
    return flag;
  }

  private static int positive(Lookup values, String key, int fallback) {
    Object value = values.get(key);
    if (value == null) {
      return fallback;
    }
    String field = values.where(key);
    int number = whole(field, value);
    if (number <= 0) {
      throw new GradleException("Invalid " + field + " in project.toml: it must be greater than 0, not " + number
          + ".");
    }
    return number;
  }

  private static int whole(String field, Object value) {
    if (!(value instanceof Long number)) {
      throw typeError(field, "a whole number");
    }
    if (number < Integer.MIN_VALUE || number > Integer.MAX_VALUE) {
      throw new GradleException("Invalid " + field + " in project.toml: " + number + " is out of range.");
    }
    return number.intValue();
  }

  private static int[] size(Lookup values, Object value) {
    String field = values.where("render.resolution");
    if (!(value instanceof TomlArray array) || array.size() != 2) {
      throw typeError(field, "a list of two numbers such as [1280, 720] (or false to turn it off)");
    }
    int[] size = new int[2];
    for (int i = 0; i < 2; i++) {
      size[i] = whole(field, array.get(i));
      if (size[i] < 0) {
        throw new GradleException("Invalid " + field + " in project.toml: " + size[i] + " is negative, but 0 or "
            + "more is needed (0 means the window size).");
      }
    }
    return size;
  }

  private static float scale(Lookup values, Object value) {
    String field = values.where("render.scale");
    double number;
    if (value instanceof Long whole) {
      number = whole;
    } else if (value instanceof Double decimal) {
      number = decimal;
    } else {
      throw typeError(field, "a number such as 1.0");
    }
    if (!(number > 0) || Double.isInfinite(number)) {
      throw new GradleException("Invalid " + field + " in project.toml: it must be greater than 0, not " + number
          + ".");
    }
    return (float) number;
  }

  private static GradleException typeError(String field, String expected) {
    return new GradleException("Invalid " + field + " in project.toml: it must be " + expected + ".");
  }

  /** Looks values up in the selected profile first and in the rest of the file second. */
  private record Lookup(TomlTable base, @Nullable TomlTable profile, String name) {

    @Nullable
    Object get(String key) {
      Object value = profile == null ? null : profile.get(key);
      return value != null ? value : base.get(key);
    }

    /** Names where a value comes from, so an error points at the profile line when the profile set it. */
    String where(String key) {
      return profile != null && profile.get(key) != null ? PROFILES + "." + name + "." + key : key;
    }
  }
}
