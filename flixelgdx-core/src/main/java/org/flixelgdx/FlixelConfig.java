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

import org.flixelgdx.backend.FlixelWindow;
import org.flixelgdx.util.save.FlixelSave;
import org.jetbrains.annotations.NotNull;

/**
 * Immutable startup configuration for a {@link FlixelGame}.
 *
 * <p>Build one via {@link Builder}, pass it to your {@link FlixelGame} constructor, and the
 * framework reads it once at startup. No property can change after {@link Builder#build()} returns,
 * so there is never any ambiguity between what was configured and what the game is running with.
 *
 * <p>Most properties have sensible defaults (640x360, 60 fps, VSync on, windowed), so you only
 * need to set what differs. Two properties deserve special attention:
 *
 * <ul>
 *   <li>{@code title} - the text shown in the game window's title bar. Required; pass it to the
 *       {@link Builder} constructor.</li>
 *   <li>{@code company} - the studio or organization name. Strongly recommended whenever the game
 *       uses {@link FlixelSave}. The save system combines it with the title to build the
 *       OS-specific data directory ({@code %APPDATA%\Company\Title\saves\} on Windows,
 *       {@code ~/Library/Application Support/Company/Title/saves/} on macOS,
 *       {@code $XDG_DATA_HOME/Company/Title/saves/} on Linux). Calling
 *       {@link FlixelSave#bind(String, String)} without a company name is an error.</li>
 * </ul>
 *
 * <pre>{@code
 * new FlixelConfig.Builder("My Game")
 *     .company("My Studio")
 *     .version("1.0.0")
 *     .size(1280, 720)
 *     .build()
 * }</pre>
 *
 * @see FlixelGame
 * @see Builder
 */
public final class FlixelConfig {

  @NotNull
  private final String title;
  @NotNull
  private final String company;
  @NotNull
  private final String version;

  private final int width;
  private final int height;
  private final int framerate;
  private final int renderWidth;
  private final int renderHeight;

  private final boolean vsync;
  private final boolean fullscreen;
  private final boolean renderResolutionEnabled;
  private final boolean renderSmooth;
  private final boolean transparentFramebuffer;

  private FlixelConfig(@NotNull Builder builder) {
    this.title = builder.title;
    this.company = builder.company;
    this.version = builder.version;
    this.width = builder.width;
    this.height = builder.height;
    this.framerate = builder.framerate;
    this.renderWidth = builder.renderWidth;
    this.renderHeight = builder.renderHeight;
    this.vsync = builder.vsync;
    this.fullscreen = builder.fullscreen;
    this.renderResolutionEnabled = builder.renderResolutionEnabled;
    this.renderSmooth = builder.renderSmooth;
    this.transparentFramebuffer = builder.transparentFramebuffer;
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

  /**
   * Returns {@code true} when the game renders at a fixed resolution and upscales to the window.
   *
   * <p>Enabled by default; see {@link Builder#renderResolution(int, int)}.
   *
   * @return {@code true} when a fixed render resolution is configured.
   */
  public boolean isRenderResolutionEnabled() {
    return renderResolutionEnabled;
  }

  /**
   * Returns the fixed render width in pixels, falling back to the design width when none was set.
   *
   * @return The render width to draw the scene at.
   */
  public int getRenderWidth() {
    return renderWidth > 0 ? renderWidth : width;
  }

  /**
   * Returns the fixed render height in pixels, falling back to the design height when none was set.
   *
   * @return The render height to draw the scene at.
   */
  public int getRenderHeight() {
    return renderHeight > 0 ? renderHeight : height;
  }

  /**
   * Returns {@code true} for smooth (linear) upscaling, or {@code false} for nearest-neighbor.
   *
   * @return {@code true} when linear filtering is used during upscaling.
   */
  public boolean isRenderSmooth() {
    return renderSmooth;
  }

  public boolean isVsync() {
    return vsync;
  }

  public boolean isFullscreen() {
    return fullscreen;
  }

  public boolean getFullscreen() {
    return fullscreen;
  }

  /**
   * Returns whether an alpha-capable (transparent) default framebuffer was requested at launch.
   *
   * <p>When {@code true}, the launcher creates the window with compositing support so
   * {@link FlixelWindow#setTransparencyActive(boolean)} can blend the game with the desktop.
   * When {@code false} (the default), the framebuffer is opaque and transparency has no effect.
   *
   * @return {@code true} when an alpha-capable framebuffer was requested.
   * @see Builder#transparentFramebuffer(boolean)
   * @see org.flixelgdx.backend.FlixelWindow#setTransparencyActive(boolean)
   */
  public boolean isTransparentFramebuffer() {
    return transparentFramebuffer;
  }

  /**
   * Fluent builder for {@link FlixelConfig}.
   *
   * <p>The game title is required and must be supplied to the constructor. Everything else
   * defaults to a safe value and can be set in any order before calling {@link #build()}.
   *
   * <p>The same builder instance must not be reused after {@link #build()} is called; create a
   * new one instead.
   *
   * <pre>{@code
   * FlixelConfig config = new FlixelConfig.Builder("My Game")
   *     .company("My Studio")
   *     .version("1.0.0")
   *     .size(1280, 720)
   *     .framerate(144)
   *     .vsync(false)
   *     .build();
   * }</pre>
   */
  public static final class Builder {

    @NotNull
    private final String title;
    @NotNull
    private String company = "";
    @NotNull
    private String version = "";

    private int width = 640;
    private int height = 360;
    private int framerate = 60;
    private int renderWidth = 0;
    private int renderHeight = 0;

    private boolean vsync = true;
    private boolean fullscreen = false;
    private boolean renderResolutionEnabled = true;
    private boolean renderSmooth = true;
    private boolean transparentFramebuffer = false;

    /**
     * Creates a builder for a game with the given window title.
     *
     * @param title The title to display in the game window's title bar. Must not be null or empty.
     * @throws IllegalArgumentException if {@code title} is null or empty.
     */
    public Builder(@NotNull String title) {
      if (title == null || title.isEmpty()) {
        throw new IllegalArgumentException("Game title cannot be null or empty.");
      }
      this.title = title;
    }

    /**
     * Sets the company or studio name. Used by {@link FlixelSave} to build the OS-specific save
     * directory.
     *
     * @param company The company or studio name.
     * @return This builder, for chaining.
     */
    @NotNull
    public Builder company(@NotNull String company) {
      this.company = company != null ? company : "";
      return this;
    }

    /**
     * Sets the game version string (for example {@code "1.0.0"} or {@code "2.3.1-beta"}).
     *
     * @param version The version string.
     * @return This builder, for chaining.
     */
    @NotNull
    public Builder version(@NotNull String version) {
      this.version = version != null ? version : "";
      return this;
    }

    /**
     * Sets the starting window size and the dimensions of the first camera. Also sets
     * the render resolution by default.
     *
     * @param width The width in pixels.
     * @param height The height in pixels.
     * @return This builder, for chaining.
     */
    @NotNull
    public Builder size(int width, int height) {
      this.width = width;
      this.height = height;
      return this;
    }

    /**
     * Sets the target update and render framerate.
     *
     * @param framerate Frames per second.
     * @return This builder, for chaining.
     */
    @NotNull
    public Builder framerate(int framerate) {
      this.framerate = framerate;
      return this;
    }

    /**
     * Controls whether VSync is requested at startup.
     *
     * @param vsync {@code true} to cap rendering to the monitor's refresh rate.
     * @return This builder, for chaining.
     */
    @NotNull
    public Builder vsync(boolean vsync) {
      this.vsync = vsync;
      return this;
    }

    /**
     * Controls whether the game starts in fullscreen mode.
     *
     * @param fullscreen {@code true} to start fullscreen.
     * @return This builder, for chaining.
     */
    @NotNull
    public Builder fullscreen(boolean fullscreen) {
      this.fullscreen = fullscreen;
      return this;
    }

    /**
     * Sets a fixed render resolution the whole scene is drawn at before being upscaled to the
     * window, with smooth (linear) filtering.
     *
     * <p>A fixed render resolution is <b>on by default</b> at the design size set by
     * {@link #size(int, int)}, so most games do not need to call this. Use it to render at a
     * different size than the design size: below it (for example {@code 960x540} for a
     * {@code 1280x720} game) as a performance option, or above it to supersample for smoother
     * edges. Keep the same aspect ratio as the design size to avoid distortion. To turn the
     * feature off entirely and draw straight to the window, call {@link #disableRenderResolution()}.
     *
     * @param width The fixed render width in pixels.
     * @param height The fixed render height in pixels.
     * @return This builder, for chaining.
     */
    @NotNull
    public Builder renderResolution(int width, int height) {
      return renderResolution(width, height, true);
    }

    /**
     * Sets a fixed render resolution and chooses how it is filtered when upscaled to the window.
     *
     * @param width The fixed render width in pixels.
     * @param height The fixed render height in pixels.
     * @param smooth {@code true} for linear filtering, {@code false} for nearest-neighbor (crisp
     *     pixel art).
     * @return This builder, for chaining.
     * @see #renderResolution(int, int)
     */
    @NotNull
    public Builder renderResolution(int width, int height, boolean smooth) {
      this.renderWidth = width;
      this.renderHeight = height;
      this.renderSmooth = smooth;
      this.renderResolutionEnabled = true;
      return this;
    }

    /**
     * Turns off the fixed render resolution so the scene draws straight to the window at its real
     * size. This opts out of the on-by-default behavior described in
     * {@link #renderResolution(int, int)}.
     *
     * @return This builder, for chaining.
     */
    @NotNull
    public Builder disableRenderResolution() {
      this.renderResolutionEnabled = false;
      return this;
    }

    /**
     * Requests an alpha-capable (transparent) default framebuffer at launch.
     *
     * <p>When {@code true}, the window is created with compositor support so
     * {@link FlixelWindow#setTransparencyActive(boolean)} can blend the game with the desktop
     * at runtime. Without this, {@code setTransparencyActive(true)} renders transparent areas as
     * black because the back buffer has no alpha channel.
     *
     * <p><b>WARNING:</b> This can cause minor performance overhead on low-end devices, so only
     * enable it when your game actually uses desktop transparency.
     *
     * @param transparentFramebuffer {@code true} to request an alpha-capable framebuffer.
     * @return This builder, for chaining.
     * @see FlixelWindow#setTransparencyActive(boolean)
     */
    @NotNull
    public Builder transparentFramebuffer(boolean transparentFramebuffer) {
      this.transparentFramebuffer = transparentFramebuffer;
      return this;
    }

    /**
     * Builds the immutable {@link FlixelConfig} from the values set on this builder.
     *
     * @return A new, immutable config instance.
     */
    @NotNull
    public FlixelConfig build() {
      return new FlixelConfig(this);
    }
  }
}
