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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only view of a game's {@code project.toml}, available on every project as {@code game}.
 *
 * <p>Build scripts can read the values instead of repeating them:
 *
 * <pre>{@code
 * android {
 *   defaultConfig {
 *     applicationId = game.packageName
 *     versionName = game.version
 *   }
 * }
 * }</pre>
 *
 * <p>The same object is also registered as {@code flixelProject}, the name earlier versions used, so existing build
 * scripts keep working. Gradle's own {@code project.version} is set to the same version, so
 * {@code version.toString()} works everywhere without this extension.
 */
public final class ProjectExtension {

  /** The name the extension is registered under, which build scripts should use. */
  public static final String GAME_NAME = "game";

  /** The name earlier versions registered the extension under, kept so existing build scripts keep working. */
  public static final String NAME = "flixelProject";

  private final ProjectModel model;

  ProjectExtension(ProjectModel model) {
    this.model = model;
  }

  /**
   * The window title and display name of the game.
   *
   * @return The title.
   */
  public String getTitle() {
    return model.title();
  }

  /**
   * The Java package of the game, which is also the Gradle group.
   *
   * @return The package name.
   */
  public String getPackageName() {
    return model.packageName();
  }

  /**
   * The company or studio name.
   *
   * @return The company.
   */
  public String getCompany() {
    return model.company();
  }

  /**
   * The game version.
   *
   * @return The version.
   */
  public String getVersion() {
    return model.version();
  }

  /**
   * The build number, a whole number that goes up with every release.
   *
   * <p>Android compares it to decide whether an install is newer, so it is meant for {@code versionCode}:
   *
   * <pre>{@code
   * android {
   *   defaultConfig {
   *     versionCode = game.build
   *   }
   * }
   * }</pre>
   *
   * @return The build number, {@code 1} when {@code app.build} is left out.
   */
  public int getBuild() {
    return model.build();
  }

  /**
   * The name of the profile selected with {@code -Pflixel.profile}, whose values were applied on top of the file.
   *
   * @return The profile name, or an empty string when none is selected.
   */
  public String getProfile() {
    return model.profile();
  }

  /**
   * The language of the game's code, {@code java} or {@code kotlin}.
   *
   * @return The language.
   */
  public String getLanguage() {
    return model.language();
  }

  /**
   * The starting window width in pixels.
   *
   * @return The width.
   */
  public int getWidth() {
    return model.width();
  }

  /**
   * The starting window height in pixels.
   *
   * @return The height.
   */
  public int getHeight() {
    return model.height();
  }

  /**
   * The target framerate.
   *
   * @return The framerate.
   */
  public int getFramerate() {
    return model.framerate();
  }

  /**
   * Whether VSync is requested at startup.
   *
   * @return {@code true} for VSync.
   */
  public boolean isVsync() {
    return model.vsync();
  }

  /**
   * Whether the game starts in fullscreen.
   *
   * @return {@code true} for fullscreen.
   */
  public boolean isFullscreen() {
    return model.fullscreen();
  }

  /**
   * The platform modules the game builds for besides {@code core}, such as {@code desktop}.
   *
   * @return The platform names, in the order they were listed.
   */
  public List<String> getPlatforms() {
    return model.platforms();
  }

  /**
   * The values of the {@code [defines]} table, looked up by the key written in the file.
   *
   * <p>Build scripts can branch on the same flags the game's code uses:
   *
   * <pre>{@code
   * if (game.defines["demo"] == true) {
   *   packagr { appName = "My Game Demo" }
   * }
   * }</pre>
   *
   * @return A read-only map from each key to its value (a {@link Boolean}, {@link Integer}, {@link Long},
   *     {@link Float} or {@link String}), in the order the keys were written. Empty when there is no table.
   */
  public Map<String, Object> getDefines() {
    Map<String, Object> defines = new LinkedHashMap<>();
    for (ProjectDefine define : model.defines()) {
      defines.put(define.key(), define.value());
    }
    return Collections.unmodifiableMap(defines);
  }

  /**
   * The full parsed model, for the values that have no getter of their own such as the render settings.
   *
   * @return The model.
   */
  public ProjectModel getModel() {
    return model;
  }
}
