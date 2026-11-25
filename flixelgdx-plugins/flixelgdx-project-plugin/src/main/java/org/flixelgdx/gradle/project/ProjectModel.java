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

import java.util.List;

/**
 * The values read from a game's {@code project.toml}, checked and with defaults filled in.
 *
 * <p>Think of it as the filled-in form that every other part of the plugin reads from: the module list, the
 * project name and version, and the values the generated {@code Project} class holds. The defaults of the optional
 * values match the ones of {@code FlixelConfig.Builder}, so leaving a value out behaves the same as never calling
 * the matching builder method.
 *
 * @param width The window width in pixels.
 * @param height The window height in pixels.
 * @param framerate The target framerate.
 * @param build The build number, a whole number that goes up with every release (Android's {@code versionCode}).
 * @param renderWidth The render resolution width, where {@code 0} means the window width.
 * @param renderHeight The render resolution height, where {@code 0} means the window height.
 * @param renderScale How many pixels the scene is drawn with relative to the size it is displayed at.
 * @param title The window title and display name.
 * @param company The company or studio name.
 * @param version The game version.
 * @param packageName The Java package of the game, also used as the Gradle group.
 * @param language Either {@code java} or {@code kotlin}.
 * @param platforms The platform modules to include besides {@code core}, in the order they were listed.
 * @param vsync Whether VSync is requested at startup.
 * @param fullscreen Whether the game starts in fullscreen.
 * @param hasRender Whether the file has a {@code [render]} table at all.
 * @param renderResolution Whether a render resolution is used, {@code false} to draw at the native size.
 * @param renderSmooth Whether the scene is stretched with linear filtering instead of nearest-neighbor.
 * @param transparent Whether an alpha-capable framebuffer is requested.
 */
public record ProjectModel(
    int width,
    int height,
    int framerate,
    int build,
    int renderWidth,
    int renderHeight,
    float renderScale,
    String title,
    String company,
    String version,
    String packageName,
    String language,
    List<String> platforms,
    boolean vsync,
    boolean fullscreen,
    boolean hasRender,
    boolean renderResolution,
    boolean renderSmooth,
    boolean transparent) {

  /**
   * Creates a model with the build number left at its default of {@code 1}.
   *
   * <p>This is the constructor earlier versions had, kept so code that builds a model by hand still compiles.
   *
   * @param width The window width in pixels.
   * @param height The window height in pixels.
   * @param framerate The target framerate.
   * @param renderWidth The render resolution width, where {@code 0} means the window width.
   * @param renderHeight The render resolution height, where {@code 0} means the window height.
   * @param renderScale How many pixels the scene is drawn with relative to the size it is displayed at.
   * @param title The window title and display name.
   * @param company The company or studio name.
   * @param version The game version.
   * @param packageName The Java package of the game, also used as the Gradle group.
   * @param language Either {@code java} or {@code kotlin}.
   * @param platforms The platform modules to include besides {@code core}, in the order they were listed.
   * @param vsync Whether VSync is requested at startup.
   * @param fullscreen Whether the game starts in fullscreen.
   * @param hasRender Whether the file has a {@code [render]} table at all.
   * @param renderResolution Whether a render resolution is used, {@code false} to draw at the native size.
   * @param renderSmooth Whether the scene is stretched with linear filtering instead of nearest-neighbor.
   * @param transparent Whether an alpha-capable framebuffer is requested.
   */
  public ProjectModel(int width, int height, int framerate, int renderWidth, int renderHeight, float renderScale,
      String title, String company, String version, String packageName, String language, List<String> platforms,
      boolean vsync, boolean fullscreen, boolean hasRender, boolean renderResolution, boolean renderSmooth,
      boolean transparent) {
    this(width, height, framerate, ProjectToml.DEFAULT_BUILD, renderWidth, renderHeight, renderScale, title, company,
        version, packageName, language, platforms, vsync, fullscreen, hasRender, renderResolution, renderSmooth,
        transparent);
  }
}
