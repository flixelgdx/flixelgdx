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
package org.flixelgdx;

import org.jetbrains.annotations.NotNull;

/**
 * The configuration object for a {@link FlixelGame} instance.
 *
 * <p>Pass an instance to your {@link FlixelGame} constructor to supply all startup settings in one
 * place instead of through a chain of constructor overloads. Use the fluent builder methods to set
 * only what you need; everything else defaults to a sensible value.
 *
 * <p>The {@code title} and {@code company} fields deserve special attention:
 * <ul>
 *   <li>{@code title} is required. It is displayed in the game window's title bar.</li>
 *   <li>{@code company} is strongly recommended whenever the game uses {@link org.flixelgdx.util.save.FlixelSave}.
 *       The save system needs it to resolve the correct OS-specific directory ({@code %APPDATA%\Company\Game\}
 *       on Windows, {@code ~/Library/Application Support/Company/Game/} on macOS, and
 *       {@code $XDG_DATA_HOME/Company/Game/} on Linux). Calling
 *       {@link org.flixelgdx.util.save.FlixelSave#bind(String, String)} without setting a company name is
 *       an error; use {@link org.flixelgdx.util.save.FlixelSave#bind(String, String, org.flixelgdx.file.FlixelFile)}
 *       with a custom directory as an alternative.</li>
 * </ul>
 *
 * <p>Example:
 *
 * <pre>{@code
 * public class MyGame extends FlixelGame {
 *
 *   public MyGame() {
 *     super(
 *       new FlixelGameConfig("My Game")
 *         .company("My Studio")
 *         .version("1.0.0")
 *         .size(1280, 720)
 *         .framerate(60),
 *       () -> new MenuState()
 *     );
 *   }
 * }
 * }</pre>
 *
 * @see FlixelGame
 */
public final class FlixelGameConfig {

  @NotNull
  private final String title;

  @NotNull
  private String company = "";

  @NotNull
  private String version = "";

  private int width = 640;

  private int height = 360;

  private int framerate = 60;

  private boolean vsync = true;

  private boolean fullscreen = false;

  /**
   * Creates a new configuration with the given window title and default values for everything else
   * (640x360, 60 fps, VSync on, windowed).
   *
   * @param title The title to display in the game window's title bar. Must not be null or empty.
   * @throws IllegalArgumentException if {@code title} is null or empty.
   */
  public FlixelGameConfig(@NotNull String title) {
    if (title == null || title.isEmpty()) {
      throw new IllegalArgumentException("Game title cannot be null or empty.");
    }
    this.title = title;
  }

  /**
   * Sets the company or studio name. Used by {@link org.flixelgdx.util.save.FlixelSave} to build
   * the OS-specific save directory path.
   *
   * @param company The company or studio name.
   * @return This config, for chaining.
   */
  @NotNull
  public FlixelGameConfig company(@NotNull String company) {
    this.company = company != null ? company : "";
    return this;
  }

  /**
   * Sets the game version string (for example {@code "1.0.0"} or {@code "2.3.1-beta"}).
   *
   * @param version The version string.
   * @return This config, for chaining.
   */
  @NotNull
  public FlixelGameConfig version(@NotNull String version) {
    this.version = version != null ? version : "";
    return this;
  }

  /**
   * Sets the starting window size and the width and height of the first camera.
   *
   * @param width The width in pixels.
   * @param height The height in pixels.
   * @return This config, for chaining.
   */
  @NotNull
  public FlixelGameConfig size(int width, int height) {
    this.width = width;
    this.height = height;
    return this;
  }

  /**
   * Sets the target update and render framerate.
   *
   * @param framerate Frames per second.
   * @return This config, for chaining.
   */
  @NotNull
  public FlixelGameConfig framerate(int framerate) {
    this.framerate = framerate;
    return this;
  }

  /**
   * Controls whether VSync is requested at startup.
   *
   * @param vsync {@code true} to cap rendering to the monitor's refresh rate.
   * @return This config, for chaining.
   */
  @NotNull
  public FlixelGameConfig vsync(boolean vsync) {
    this.vsync = vsync;
    return this;
  }

  /**
   * Controls whether the game starts in fullscreen mode.
   *
   * @param fullscreen {@code true} to start fullscreen.
   * @return This config, for chaining.
   */
  @NotNull
  public FlixelGameConfig fullscreen(boolean fullscreen) {
    this.fullscreen = fullscreen;
    return this;
  }

  @NotNull
  public String getTitle() {
    return title;
  }

  @NotNull
  public String getCompany() {
    return company;
  }

  @NotNull
  public String getVersion() {
    return version;
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public int getFramerate() {
    return framerate;
  }

  public boolean isVsync() {
    return vsync;
  }

  public boolean getVsync() {
    return vsync;
  }

  public boolean isFullscreen() {
    return fullscreen;
  }

  public boolean getFullscreen() {
    return fullscreen;
  }
}
