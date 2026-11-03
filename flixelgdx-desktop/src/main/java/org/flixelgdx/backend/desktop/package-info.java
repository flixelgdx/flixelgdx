/**
 * Desktop backend for FlixelGDX, targeting Windows, macOS, and Linux through SDL3, WebGPU, and
 * miniaudio.
 *
 * <p>This module is the bridge between the platform-agnostic
 * {@code flixelgdx-core} API and the native system. It wires together three native libraries
 * (SDL3 for windowing and input, WebGPU for rendering, miniaudio for audio) and hands them to the
 * core through FlixelGDX's backend interfaces. Game code never touches this module directly; it
 * only uses the abstractions in core ({@link org.flixelgdx.Flixel Flixel},
 * {@link org.flixelgdx.FlixelGame FlixelGame}, and so on).
 *
 * <h2>Launching a game</h2>
 * <p>The only class most games ever import from this module is
 * {@link org.flixelgdx.backend.desktop.FlixelDesktopLauncher FlixelDesktopLauncher}. One call
 * from your {@code main} method installs every backend service and starts the game loop:
 *
 * <pre>{@code
 * public final class DesktopLauncher {
 *   public static void main(String[] args) {
 *     FlixelDesktopLauncher.launch(new MyGame());
 *   }
 *   private DesktopLauncher() {}
 * }
 * }</pre>
 *
 * <p>The two-argument overload accepts a
 * {@link org.flixelgdx.backend.FlixelRuntimeMode FlixelRuntimeMode}, which runs after all default
 * services are installed but before the game starts.
 *
 * <h2>What the launcher installs</h2>
 * <p>A call to {@link org.flixelgdx.backend.desktop.FlixelDesktopLauncher#launch(org.flixelgdx.FlixelGame) FlixelDesktopLauncher.launch(...)}
 * wires up the following:
 * <ul>
 *   <li><b>Window</b> -
 *       {@link org.flixelgdx.backend.desktop.FlixelSdlWindow FlixelSdlWindow} wraps an SDL3
 *       window and exposes title, size, position, fullscreen, decoration, focus, opacity, and
 *       close controls through {@link org.flixelgdx.Flixel#window Flixel.window}.</li>
 *   <li><b>Graphics</b> - the shared WebGPU renderer from {@code flixelgdx-gpu}
 *       ({@link org.flixelgdx.backend.gpu.FlixelGpuGraphics FlixelGpuGraphics}) draws into a WebGPU
 *       surface created from the SDL3 window's native handles, and implements the entire rendering
 *       API: textures, batched sprite drawing, render targets, and custom WGSL shaders. WebGPU
 *       selects the best available graphics API (Vulkan, Metal, Direct3D 12) at startup. You
 *       can override the graphics API by passing the JVM argument {@code -Dflixel.render.backend}
 *       with your preferred choice. Review the switch statement in {@code resolveBackendType}
 *       inside of {@link org.flixelgdx.backend.desktop.FlixelDesktopRunner} for the different options.
 *       The launcher installs the WebGPU backend (jwebgpu's FFM backend) itself, so games never do. The
 *       JVM must be started with {@code --enable-native-access=ALL-UNNAMED}, which Gradle run tasks
 *       and packaged games set for you.</li>
 *   <li><b>Audio</b> - {@code FlixelMiniAudio} in the {@code audio} sub-package provides
 *       multi-channel playback, volume control, and DSP effects (reverb, echo, low-pass) through
 *       miniaudio. It is accessed via {@link org.flixelgdx.Flixel#sound Flixel.sound}.</li>
 *   <li><b>Input</b> - {@code FlixelDesktopInputDevice} translates SDL3 keyboard and mouse
 *       events into the core input API. {@code FlixelSdlGamepadProvider} adds gamepad support
 *       (SDL3's controller database covers hundreds of devices out of the box).
 *       {@code FlixelSdlMouseIconManager} lets game code change or hide the system cursor.</li>
 *   <li><b>Text rasterization</b> - {@code FlixelStbFontRasterizer} in the {@code text}
 *       sub-package turns {@code .ttf} and {@code .otf} files into glyph atlases via stb_truetype,
 *       enabling {@link org.flixelgdx.text.FlixelFontRegistry FlixelFontRegistry} to bake fonts at
 *       any pixel size.</li>
 *   <li><b>File system</b> - A JVM file seam backed by the classpath and the OS filesystem is
 *       installed as {@link org.flixelgdx.Flixel#files Flixel.files}.</li>
 *   <li><b>Asset manager</b> - A JVM-based asset manager is installed as
 *       {@link org.flixelgdx.Flixel#assets Flixel.assets}.</li>
 *   <li><b>Logging</b> - JVM stack traces and optional file logging are wired into the logger.
 *       Log files are written to the platform's writable directory.</li>
 * </ul>
 *
 * <h2>The game loop</h2>
 * <p>{@link org.flixelgdx.backend.desktop.FlixelDesktopRunner FlixelDesktopRunner} owns the
 * native main loop. It initializes SDL3, creates the window, turns it into a WebGPU surface,
 * then pumps SDL3 events every frame and drives the game via
 * {@link org.flixelgdx.FlixelGame#advanceTime(float) advanceTime},
 * {@link org.flixelgdx.FlixelGame#update(float) update}, and
 * {@link org.flixelgdx.FlixelGame#draw(org.flixelgdx.graphics.FlixelBatch) draw}
 * with accurate elapsed time. It also handles the continuous-rendering flag: when the window
 * loses focus and the game requests non-continuous rendering, the loop idles instead of spinning,
 * saving CPU and battery.
 *
 * <h2>Sub-packages</h2>
 * <ul>
 *   <li>{@code audio} - miniaudio integration ({@code FlixelMiniAudio},
 *       {@code FlixelMiniAudioSound}, {@code FlixelMiniAudioGroup}).</li>
 *   <li>{@code graphics} - the stb_image decoder ({@code FlixelStbImage}). Rendering itself lives
 *       in the shared {@code flixelgdx-gpu} module.</li>
 *   <li>{@code input} - SDL3 keyboard, mouse, and gamepad handling
 *       ({@code FlixelDesktopInputDevice}, {@code FlixelSdlGamepad},
 *       {@code FlixelSdlGamepadProvider}, {@code FlixelSdlKeyMap},
 *       {@code FlixelSdlMouseIconManager}).</li>
 *   <li>{@code text} - stb_truetype font rasterizer ({@code FlixelStbFontRasterizer},
 *       {@code FlixelStbRasterizedFont}).</li>
 *   <li>{@code asset} - JVM asset manager with background loading
 *       ({@code FlixelJvmAssetManager}).</li>
 *   <li>{@code file} - classpath and OS filesystem seam
 *       ({@code FlixelJvmFiles}, {@code FlixelJvmFile}).</li>
 *   <li>{@code logging} - desktop logging helpers.</li>
 *   <li>{@code runtime} - JVM runtime device
 *       ({@code FlixelJvmRuntimeDevice}).</li>
 * </ul>
 *
 * @see org.flixelgdx.backend.desktop.FlixelDesktopLauncher
 * @see org.flixelgdx.backend.desktop.FlixelDesktopRunner
 * @see org.flixelgdx.backend.desktop.FlixelSdlWindow
 */
package org.flixelgdx.backend.desktop;
