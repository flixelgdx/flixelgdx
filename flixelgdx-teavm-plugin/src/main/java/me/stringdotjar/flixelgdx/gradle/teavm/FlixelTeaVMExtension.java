/*********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 *********************************************************************************/

package me.stringdotjar.flixelgdx.gradle.teavm;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;

/**
 * Configuration extension exposed as the {@code flixelgdx} DSL block in a web module's
 * {@code build.gradle}.
 *
 * <p>All properties have sensible defaults and are optional. The only required configuration is
 * {@code teavm.all.mainClass} in the {@code org.teavm} plugin block (see
 * {@link FlixelTeaVMPlugin} for usage).
 *
 * <h2>Example</h2>
 *
 * <pre>{@code
 * flixelgdx {
 *   // Title of the game (default: "My FlixelGDX Game").
 *   title = 'My Game Title'
 *
 *   // Override the canvas element ID (default: "flixelgdx-canvas").
 *   canvasId = 'my-game-canvas'
 *
 *   // Change where the web app is assembled (default: "$buildDir/dist/webapp").
 *   // Must match teavm.js.outputDir! Otherwise, the generated index.html will not be found.
 *   outputDir = file("$buildDir/dist/webapp")
 *
 *   // Port for the `run` dev server task (default: 8080).
 *   devServerPort = 8080
 *
 *   // Specify the directory where game assets are stored (default: rootProject/assets/).
 *   assetsDir = file('../assets')
 *
 *   // Specify the directory where user-provided web resources are stored (default: src/main/webapp/).
 *   webappDir = file('src/main/webapp')
 *
 *   // Set to false to disable index.html auto-generation entirely (default: true).
 *   generateDefaultIndexHtml = true
 *
 *   // Set to false to disable automatic startup logo generation (default: true).
 *   generateDefaultStartupLogo = true
 *
 *   // Provide a custom startup logo instead of the built-in placeholder.
 *   customStartupLogo = file('src/main/webapp/startup-logo.png')
 *
 *   // Provide a custom index.html instead of the generated default.
 *   customIndexHtml = file('src/main/webapp/index.html')
 *
 *   // Provide a custom startup logo instead of the built-in placeholder.
 *   customStartupLogo = file('src/main/webapp/startup-logo.png')
 *
 *   // Provide a favicon that is copied to the output and linked in the generated index.html.
 *   customFavicon = file('src/main/webapp/favicon.ico')
 * }
 * }</pre>
 */
public interface FlixelTeaVMExtension {

  /** Gradle extension name used to register this extension under. */
  String NAME = "flixelgdx";

  /** Default title for the game's browser tab. */
  String DEFAULT_TITLE = "My FlixelGDX Game";

  /** Default HTML canvas element ID expected by {@code FlixelTeaVMLauncher}. */
  String DEFAULT_CANVAS_ID = "flixelgdx-canvas";

  /**
   * The title that is set on the browser tab the game is running in.
   *
   * @return The title of the tab.
   */
  Property<String> getTitle();

  /**
   * ID of the HTML {@code <canvas>} element that the game renders into.
   *
   * <p>Must match the {@code canvasID} field of {@code WebApplicationConfiguration} passed to
   * {@code FlixelTeaVMLauncher.launch()}. Defaults to {@value #DEFAULT_CANVAS_ID}.
   *
   * @return the canvas element ID property.
   */
  Property<String> getCanvasId();

  /**
   * Directory into which the assembled web application is written.
   *
   * <p>This must match the value of {@code teavm.js.outputDir} in the {@code org.teavm} plugin
   * block so that copied assets, web resources, and the generated {@code index.html} are placed
   * alongside the compiled {@code teavm.js} file. Defaults to
   * {@code "$buildDir/dist/webapp"}.
   *
   * @return the output directory property.
   */
  DirectoryProperty getOutputDir();

  /**
   * Directory that contains user-provided web resources such as a custom {@code index.html},
   * favicon, or additional scripts.
   *
   * <p>All files found here are copied verbatim into {@link #getOutputDir()}. If this directory
   * contains an {@code index.html}, the plugin skips automatic index generation. Defaults to
   * {@code src/main/webapp} relative to the web module.
   *
   * @return the webapp source directory property.
   */
  DirectoryProperty getWebappDir();

  /**
   * Directory whose contents are copied to {@code <outputDir>/assets/} before each build.
   *
   * <p>Defaults to the {@code assets/} directory at the root of the Gradle project (i.e. the
   * sibling of the core, desktop, and teavm modules).
   *
   * @return the assets source directory property.
   */
  DirectoryProperty getAssetsDir();

  /**
   * Whether the plugin should generate a default {@code index.html} when none is found in {@link #getWebappDir()}.
   *
   * <p>The generated page includes a {@code <canvas>} with the ID from {@link #getCanvasId()} and
   * a {@code <script>} tag that loads {@code js/teavm.js}. Set to {@code false} to suppress
   * generation entirely (you must then provide your own {@code index.html}). Defaults to {@code true}.
   *
   * @return the {@code generate-index-html} property.
   */
  Property<Boolean> getGenerateDefaultIndexHtml();

  /**
   * Whether the plugin should automatically add a default {@code default-startup-logo.png} file when
   * none is found in {@link #getWebappDir()}.
   *
   * <p>It does this by copying the default file (located in the {@code resources} folder) into the user's
   * {@code <outputDir>/assets/} folder, as gdx-teavm expects a loading logo when the game is being prepared.
   *
   * @return the {@code generate-default-startup-logo} property.
   */
  Property<Boolean> getGenerateDefaultStartupLogo();

  /**
   * TCP port that the {@code run} task's embedded HTTP dev server listens on.
   *
   * <p>Defaults to {@code 8080}. Change this if port 8080 is already in use on your machine:
   *
   * <pre>{@code
   * flixelgdx {
   *   devServerPort = 9000
   * }
   * }</pre>
   *
   * @return the dev server port property.
   */
  Property<Integer> getDevServerPort();

  /**
   * Optional path to a custom {@code index.html} file.
   *
   * <p>When set, this file is copied verbatim to {@link #getOutputDir()} as {@code index.html},
   * bypassing both the default template generator and any {@code index.html} found in
   * {@link #getWebappDir()}. The canvas ID substitution ({@code {{CANVAS_ID}}}) is not applied.
   * The developer is responsible for the full HTML content.
   *
   * @return the custom index.html file property.
   */
  RegularFileProperty getCustomIndexHtml();

  /**
   * Optional path to a custom {@code startup-logo.png} file.
   *
   * <p>When set, this file is copied to {@code <outputDir>/assets/startup-logo.png}, replacing
   * both the built-in placeholder and any auto-generation. The file must be a valid PNG image.
   *
   * @return the custom startup logo file property.
   */
  RegularFileProperty getCustomStartupLogo();

  /**
   * Optional path to a favicon file (any format supported by browsers, e.g. {@code .ico}, {@code .png}).
   *
   * <p>When set, the file is copied to {@link #getOutputDir()} and a {@code <link rel="icon">} tag
   * referencing it is injected into the generated {@code index.html}. Has no effect when a custom
   * {@code index.html} is provided via {@link #getCustomIndexHtml()} or {@link #getWebappDir()},
   * since those are copied verbatim.
   *
   * @return the custom favicon file property.
   */
  RegularFileProperty getCustomFavicon();
}
