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
package org.flixelgdx.graphics;

import org.flixelgdx.FlixelGame;
import org.flixelgdx.collections.FlixelList;
import org.flixelgdx.functional.FlixelDrawable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * The graphics device: the one interface a graphics backend implements and the surface game code
 * draws through, reached from {@link org.flixelgdx.Flixel#graphics Flixel.graphics}.
 *
 * <p>Each backend (for example, bgfx on native, WebGPU or WebGL in the browser) implements this
 * interface, so the same game code runs unchanged no matter which one is active. The underlying GPU
 * library is never named in the public API; you only ever talk to this manager.
 *
 * <p>The members fall into two groups. Most game code only uses the <b>high-level</b> ones: the
 * shared sprite batch, timing (frame rate, vertical sync), and display information (modes, density).
 * The <b>low-level</b> ones (frame boundaries, texture and mesh creation, shader compilation, the
 * native handle) are what a backend fills in and what the framework's own rendering drives; you can
 * reach for them to do advanced custom rendering, but typical games never need to.
 *
 * <p>A safe default is installed before startup, so {@code Flixel.graphics} is never {@code null};
 * on headless or not-yet-initialized sessions its methods simply do nothing and report neutral
 * values.
 *
 * <p>Every method has a safe default here so a backend can implement only what its platform
 * supports; unsupported queries return neutral values and unsupported actions do nothing. Read
 * {@link #getApi()} to learn what is actually running.
 *
 * <p>Example:
 *
 * <pre>{@code
 * FlixelBatch batch = Flixel.graphics.getBatch();
 * Flixel.graphics.setVSync(true);
 * int fps = Flixel.graphics.getFps();
 * }</pre>
 *
 * @see org.flixelgdx.Flixel#graphics
 * @see FlixelGraphicsApi
 */
public interface FlixelGraphicsManager {

  /**
   * @return Which graphics backend is running this session. Defaults to {@link FlixelGraphicsApi#Noop}.
   */
  @NotNull
  default FlixelGraphicsApi getApi() {
    return FlixelGraphicsApi.Noop;
  }

  /**
   * Returns whether the active backend maps the NDC depth range to {@code [0, 1]} (Vulkan, Metal,
   * Direct3D) rather than {@code [-1, 1]} (OpenGL, WebGL).
   *
   * <p>Orthographic projection matrices must match the backend's convention or all geometry at
   * the default depth will be depth-clipped. Renderers such as bgfx expose this as a capability
   * flag; query it here instead of hard-coding per-backend checks outside the graphics layer.
   *
   * <p>Defaults to {@code false}, the OpenGL convention, which is what headless and web backends
   * use.
   *
   * @return {@code true} when NDC depth runs from {@code 0} to {@code 1}.
   */
  default boolean isDepthZeroToOne() {
    return false;
  }

  /**
   * Returns the shared sprite batch every {@link FlixelDrawable} in the framework renders through.
   *
   * <p>When no backend is present (headless or pre-startup) this returns {@link FlixelUnsupportedBatch},
   * a no-op implementation whose operations do nothing. Check {@link #getApi()} if you need to know
   * whether a real GPU is available.
   *
   * @return The active batch; never {@code null}.
   */
  @NotNull
  default FlixelBatch getBatch() {
    return FlixelUnsupportedBatch.INSTANCE;
  }

  /**
   * Runs a task on the main render thread at a safe point.
   *
   * <p>Some work (loading callbacks, input events, network results) can arrive on a background
   * thread, but GPU calls must happen on the render thread. Pass such work here and the framework
   * runs it on the render thread before the next frame is drawn. On backends with a single thread,
   * it may run immediately.
   *
   * @param action The task to run on the render thread; ignored when {@code null}.
   */
  default void queueMainThread(@Nullable Runnable action) {}

  /**
   * Begins a new frame of rendering. Called once per frame by the framework before any draw work.
   */
  default void beginFrame() {}

  /**
   * Ends the current frame and presents it to the screen. Called once per frame by the framework
   * after all draw work.
   */
  default void endFrame() {}

  /**
   * Marks the start of one camera's on-screen draw pass. The framework calls this before drawing
   * each camera (and the overlay and debug passes) so a backend can isolate that camera's
   * projection from the others.
   *
   * <p>This matters on backends where the view or projection is shared state for a whole render
   * pass rather than per draw call. Without a fresh pass per camera, the last camera's projection
   * would apply to every camera drawn into the same pass, breaking per-camera zoom, scroll, and
   * debug overlays. Backends that set the projection per draw call can leave this as a no-op.
   */
  default void beginCameraPass() {}

  /**
   * Pins the resolution the whole scene is rendered at, independent of the window size.
   *
   * <p>Normally every sprite is drawn straight to the window, so a larger window (for example
   * fullscreen) shades proportionally more pixels and costs more GPU time. When a render resolution
   * is set, the framework instead draws the entire scene into one fixed-size off-screen surface and
   * then stretches that surface to fill the window in a single pass. The expensive per-pixel work is
   * now tied to this fixed size rather than the window, so fullscreen stops multiplying the cost.
   * This is the classic fix for a game that runs fine in a small window but drops frames when
   * maximized.
   *
   * <p>The size does not have to match the game's design size. Render below it (for example
   * {@code 960x540} for a {@code 1280x720} game) as a performance option on weak hardware, or above
   * it to supersample for smoother edges. For the picture to stay undistorted, keep the same aspect
   * ratio as the design size; the framework letterboxes the final stretch to the window just like a
   * {@link FlixelViewport.Scaling#FIT} camera.
   *
   * <p>By default, {@link FlixelGame} will automatically set this, at startup based on the initial
   * design size set. You can reset it at any time during runtime if you ever need to.
   *
   * <p>Example (render a 1280x720 game at a fixed 1280x720 no matter the window size):
   *
   * <pre>{@code
   * Flixel.graphics.setRenderResolution(1280, 720);
   * }</pre>
   *
   * @param width The fixed render width in pixels; values below {@code 1} disable the feature.
   * @param height The fixed render height in pixels; values below {@code 1} disable the feature.
   */
  default void setRenderResolution(int width, int height) {
    setRenderResolution(width, height, true);
  }

  /**
   * Pins the render resolution and chooses how the result is filtered when stretched to the window.
   *
   * <p>See {@link #setRenderResolution(int, int)} for the full explanation. This overload only adds
   * control over the upscale filter: smooth (linear) blends neighboring pixels for clean scaling of
   * high-resolution art, while nearest-neighbor keeps hard pixel edges, which is what pixel-art games
   * want.
   *
   * @param width The fixed render width in pixels; values below {@code 1} disable the feature.
   * @param height The fixed render height in pixels; values below {@code 1} disable the feature.
   * @param smooth {@code true} for linear filtering, {@code false} for nearest-neighbor.
   */
  default void setRenderResolution(int width, int height, boolean smooth) {}

  /** Disables a previously set render resolution, so the scene draws straight to the window again. */
  default void clearRenderResolution() {}

  /**
   * @return {@code true} when a fixed render resolution is active (see
   *     {@link #setRenderResolution(int, int)}). Defaults to {@code false}.
   */
  default boolean isRenderResolutionEnabled() {
    return false;
  }

  /**
   * Returns the width the scene is rendered at, which is the fixed render width when one is set and
   * the back buffer width otherwise. Framework rendering that sizes its own surfaces (such as the
   * global shader chain) uses this so it matches the scene, not the window.
   *
   * @return The scene render width in pixels.
   */
  default int getRenderWidth() {
    return getBackBufferWidth();
  }

  /**
   * Returns the height the scene is rendered at.
   *
   * @return The scene render height in pixels.
   * @see #getRenderWidth()
   */
  default int getRenderHeight() {
    return getBackBufferHeight();
  }

  /**
   * Redirects drawing into the fixed-resolution scene surface, when a render resolution is active.
   *
   * <p>The framework calls this once per frame right before it draws the cameras. When no render
   * resolution is set this does nothing and drawing goes straight to the window as usual. Pair every
   * call with {@link #endScene()}.
   */
  default void beginScene() {}

  /**
   * Ends scene redirection and stretches the fixed-resolution surface to fill the window.
   *
   * <p>The framework calls this once per frame after all cameras have drawn. When no render
   * resolution is set this does nothing. See {@link #beginScene()}.
   */
  default void endScene() {}

  /**
   * Uploads pixel data to a new GPU texture.
   *
   * @param width Texture width in pixels.
   * @param height Texture height in pixels.
   * @param rgba Tightly packed 8-bit-per-channel RGBA pixels, row by row.
   * @return A texture handle; a size-only stand-in when no backend is present.
   */
  @NotNull
  default FlixelTexture createTexture(int width, int height, @NotNull ByteBuffer rgba) {
    return new FlixelNoopTexture(width, height);
  }

  /**
   * Uploads a CPU-side image to a new GPU texture.
   *
   * @param image The pixels to upload.
   * @return A texture handle; a size-only stand-in when no backend is present.
   */
  @NotNull
  default FlixelTexture createTexture(@NotNull FlixelImage image) {
    return createTexture(image.width(), image.height(), image.pixels());
  }

  /**
   * Decodes an encoded image file (PNG, JPEG, and other common formats) into CPU-side pixels.
   *
   * <p>Decoding is a backend service because the codec differs per platform (stb on desktop, the
   * browser on web). Returns {@code null} when the data cannot be decoded or no backend is
   * present. Most games load images through the asset manager instead of calling this directly.
   *
   * @param encoded The raw bytes of the encoded file.
   * @return The decoded image, or {@code null} when decoding is unavailable or fails.
   */
  @Nullable
  default FlixelImage decodeImage(@NotNull ByteBuffer encoded) {
    return null;
  }

  /**
   * Uploads a GPU texture-container file (for example KTX2) straight to the GPU, keeping it in its
   * compressed form.
   *
   * <p>Unlike {@link #decodeImage(ByteBuffer)}, the bytes here are a full container (with header,
   * mip levels, and a compressed pixel format), not a plain image to unpack into RGBA. The backend
   * hands the whole container to the GPU driver, which keeps the compressed data resident and saves
   * both memory and upload bandwidth. This is how {@code .ktx2} siblings load when a backend
   * supports them; see {@link org.flixelgdx.asset.FlixelAssetManager#setCompressedTexturesEnabled(boolean)}.
   *
   * <p>Returns {@code null} when the running backend cannot consume compressed containers, so the
   * asset system can fall back to the plain image. The default is {@code null} (unsupported).
   *
   * @param container The raw bytes of a GPU texture-container file.
   * @return The uploaded texture, or {@code null} when compressed containers are unsupported or the
   *     data is invalid.
   */
  @Nullable
  default FlixelTexture createCompressedTexture(@NotNull ByteBuffer container) {
    return null;
  }

  /**
   * Creates an off-screen render target for post-processing passes.
   *
   * <p>When no backend is present this returns {@link FlixelUnsupportedRenderTarget}, whose
   * operations do nothing.
   *
   * @param width Target width in pixels.
   * @param height Target height in pixels.
   * @return A new render target; never {@code null}.
   */
  @NotNull
  default FlixelRenderTarget createRenderTarget(int width, int height) {
    return FlixelUnsupportedRenderTarget.INSTANCE;
  }

  /**
   * Clears the current draw surface (screen or active render target) to one color.
   *
   * @param r Red component in {@code [0, 1]}.
   * @param g Green component in {@code [0, 1]}.
   * @param b Blue component in {@code [0, 1]}.
   * @param a Alpha component in {@code [0, 1]}.
   */
  default void clear(float r, float g, float b, float a) {}

  /**
   * Fills the current view with an opaque color through the backend's fast clear, but only when that
   * view covers its whole surface.
   *
   * <p>This is an optimization for a camera's opaque background. Clearing a surface is much cheaper
   * than drawing a full-screen quad over it, since it skips shading every background pixel a second
   * time. A clear cannot always be limited to part of a shared surface, though, so this only succeeds
   * when the current view fills the entire surface (a full-screen camera). When it cannot be done
   * safely it returns {@code false} and the caller should draw a quad instead, which keeps
   * split-screen and other sub-region cameras filling only their own area.
   *
   * @param r Red component in {@code [0, 1]}.
   * @param g Green component in {@code [0, 1]}.
   * @param b Blue component in {@code [0, 1]}.
   * @param a Alpha component in {@code [0, 1]}.
   * @return {@code true} when the fill was handled by a clear, {@code false} to fall back to a quad.
   */
  default boolean fillViewOpaque(float r, float g, float b, float a) {
    return false;
  }

  /**
   * Restricts drawing to a rectangle of the draw surface, in framebuffer pixels measured from
   * the bottom-left corner. Used for sprite clip rectangles.
   *
   * @param x Left edge of the scissor rectangle.
   * @param y Bottom edge of the scissor rectangle.
   * @param width Scissor width; values below {@code 1} are clamped to {@code 1}.
   * @param height Scissor height; values below {@code 1} are clamped to {@code 1}.
   */
  default void setScissor(int x, int y, int width, int height) {}

  /** Removes the scissor rectangle so drawing covers the whole surface again. */
  default void clearScissor() {}

  /**
   * Sets the rectangle of the draw surface that rendering maps into, in framebuffer pixels
   * measured from the bottom-left corner. Cameras call this to place their viewport.
   *
   * @param x Left edge of the viewport.
   * @param y Bottom edge of the viewport.
   * @param width Viewport width.
   * @param height Viewport height.
   */
  default void setViewport(int x, int y, int width, int height) {}

  /**
   * @return The drawable surface width in physical pixels, or {@code 0} when unknown.
   */
  default int getBackBufferWidth() {
    return 0;
  }

  /**
   * @return The drawable surface height in physical pixels, or {@code 0} when unknown.
   */
  default int getBackBufferHeight() {
    return 0;
  }

  /**
   * Forces the whole draw surface's alpha channel to fully opaque without touching its color
   * channels.
   *
   * <p>This is only meaningful on desktop backends that requested a transparent-capable
   * framebuffer: after drawing, the framework calls this so tinted sprites do not composite
   * through the real desktop. Every other backend leaves it a no-op.
   */
  default void forceOpaqueAlpha() {}

  /**
   * Compiles a precompiled vertex and fragment shader pair into a backend program handle.
   *
   * <p>The byte arrays must contain shader bytecode in the format the active backend expects.
   * For the bgfx backend these are the {@code .bin} blobs produced by the FlixelGDX Gradle
   * plugin's shader cross-compilation step. Game code does not call this directly; it is
   * invoked by the framework when loading shader resources generated by the plugin.
   *
   * <p>When no backend is present (headless or pre-startup), or when the backend does not
   * support the supplied format, this returns {@link FlixelUnsupportedShader}, a no-op whose
   * {@link FlixelShaderProgram#isValid()} always returns {@code false}.
   *
   * @param vertex Compiled vertex shader bytes for the active backend.
   * @param fragment Compiled fragment shader bytes for the active backend.
   * @return A compiled {@link FlixelShaderProgram}; never {@code null}.
   */
  @NotNull
  default FlixelShaderProgram compileShaderProgram(byte @NotNull [] vertex, byte @NotNull [] fragment) {
    return FlixelUnsupportedShader.INSTANCE;
  }

  /**
   * Loads a shader the FlixelGDX Gradle plugin cross-compiled at build time and compiles the
   * variant matching the active renderer.
   *
   * <p>The plugin writes each shader's per-renderer bytecode to
   * {@code shaders/<name>/<variant>/vs.bin} and {@code fs.bin} on the classpath, where the variant
   * folder is chosen from {@link #getApi()} by {@link #shaderVariantDir(FlixelGraphicsApi)}. This
   * method reads the right pair and hands the bytes to
   * {@link #compileShaderProgram(byte[], byte[])}. Game code uses {@code FlixelShader.load(String)}
   * rather than calling this directly.
   *
   * <p>When the shader resource is missing (for example, a Direct3D variant a build could not
   * produce without an FXC compiler), this returns {@link FlixelUnsupportedShader} instead of
   * throwing, so a missing effect degrades gracefully to an unshaded draw.
   *
   * @param name The shader name declared in the {@code flixelShaders} build block.
   * @return A compiled {@link FlixelShaderProgram}; never {@code null}.
   */
  @NotNull
  default FlixelShaderProgram compileShaderProgram(@NotNull String name) {
    String dir = shaderVariantDir(getApi());
    byte[] vertex = readShaderResource("shaders/" + name + "/" + dir + "/vs.bin");
    byte[] fragment = readShaderResource("shaders/" + name + "/" + dir + "/fs.bin");
    if (vertex.length == 0 || fragment.length == 0) {
      return FlixelUnsupportedShader.INSTANCE;
    }
    return compileShaderProgram(vertex, fragment);
  }

  /**
   * Maps a graphics API to the resource sub-directory holding its compiled shader variant.
   *
   * <p>The folder names match what the FlixelGDX shader plugin emits: {@code dx11} for Direct3D,
   * {@code metal} for Metal, {@code spirv} for Vulkan, and {@code glsl} for OpenGL and every other
   * renderer.
   *
   * @param api The active graphics API.
   * @return The variant directory name.
   */
  @NotNull
  static String shaderVariantDir(@NotNull FlixelGraphicsApi api) {
    if (api == FlixelGraphicsApi.Direct3D11 || api == FlixelGraphicsApi.Direct3D12) {
      return "dx11";
    }
    if (api == FlixelGraphicsApi.Metal) {
      return "metal";
    }
    if (api == FlixelGraphicsApi.Vulkan) {
      return "spirv";
    }
    return "glsl";
  }

  /**
   * Reads a classpath resource fully into a byte array, returning an empty array when it is
   * absent or cannot be read.
   *
   * @param path The classpath resource path.
   * @return The resource bytes, or an empty array when unavailable.
   */
  private static byte @NotNull [] readShaderResource(@NotNull String path) {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    if (loader == null) {
      loader = FlixelGraphicsManager.class.getClassLoader();
    }
    try (InputStream in = loader.getResourceAsStream(path)) {
      if (in == null) {
        return new byte[0];
      }
      return in.readAllBytes();
    } catch (IOException e) {
      return new byte[0];
    }
  }

  /**
   * Allocates a reusable mesh backed by GPU buffers.
   *
   * <p>When no backend is present (headless or pre-startup) this returns {@link FlixelUnsupportedMesh},
   * a no-op implementation whose vertex and index counts are always {@code 0}. Check {@link #getApi()}
   * if you need to know whether a real GPU is available.
   *
   * @param layout Describes how each vertex is arranged.
   * @param maxVertices Maximum number of vertices the buffer must hold.
   * @param maxIndices Maximum number of indices the buffer must hold, or {@code 0} for no index buffer.
   * @param isStatic {@code true} to hint the data changes rarely, {@code false} for frequently updated data.
   * @return A new mesh; never {@code null}.
   */
  @NotNull
  default FlixelMesh createMesh(@NotNull FlixelVertexLayout layout, int maxVertices, int maxIndices, boolean isStatic) {
    return FlixelUnsupportedMesh.INSTANCE;
  }

  /**
   * @return The number of frames rendered during the last second (the measured frame rate), or
   *     {@code 0} when unknown.
   */
  default int getFps() {
    return 0;
  }

  /**
   * @return The frame-rate cap in frames per second, or {@code 0} when the frame rate is uncapped.
   */
  default int getTargetFps() {
    return 0;
  }

  /**
   * Requests a frame-rate cap, where the backend supports one.
   *
   * @param fps Target frames per second, or {@code 0} to run uncapped.
   */
  default void setTargetFps(int fps) {}

  /**
   * @return {@code true} when the frame is synchronized to the display's refresh (vertical sync).
   */
  default boolean isVSyncEnabled() {
    return false;
  }

  /**
   * Turns vertical sync on or off, where the backend supports it.
   *
   * @param enabled {@code true} to synchronize presentation to the display refresh.
   */
  default void setVSync(boolean enabled) {}

  /**
   * @return The display mode the game is currently presented with, or {@code null} when the backend
   *     cannot report one (common on web and mobile).
   */
  @Nullable
  default FlixelDisplayMode getDisplayMode() {
    return null;
  }

  /**
   * Returns every video mode the current monitor can switch to, for building a resolution picker.
   *
   * <p>This is a desktop concept; other platforms return an empty list.
   *
   * @return An unmodifiable list of available display modes, possibly empty; never {@code null}.
   */
  @NotNull
  FlixelList<FlixelDisplayMode> getDisplayModes();

  /**
   * Returns the pixel density of the display, useful for scaling UI on high-DPI screens.
   *
   * <p>A value of {@code 1.0} is the baseline (roughly 96 pixels per inch). A typical Retina or
   * high-DPI display reports around {@code 2.0}.
   *
   * @return The density scale factor, or {@code 1.0} when unknown.
   */
  default float getDensity() {
    return 1f;
  }

  /**
   * @return The display's pixels per inch, or {@code 0} when the backend cannot report it. Prefer
   *     {@link #getDensity()} for scaling; use this only when you need a physical measurement.
   */
  default float getPpi() {
    return 0f;
  }
}
