/**
 * Web backend for FlixelGDX, targeting the browser through TeaVM and WebGL2.
 *
 * <p>This module is the bridge between the platform-agnostic {@code flixelgdx-core} API and the
 * browser environment. It maps every backend interface onto the browser APIs that provide the same
 * thing: WebGL2 for rendering, the Web Audio API for sound, DOM events for keyboard and mouse input,
 * the Web Gamepad API for controllers, {@code localStorage} for save data, and standard browser APIs
 * for the window, clipboard, notifications, and wake lock. Game code never imports from this module
 * directly; it only uses the abstractions in core ({@link org.flixelgdx.Flixel Flixel},
 * {@link org.flixelgdx.FlixelGame FlixelGame}, and so on).
 *
 * <h2>Launching a game</h2>
 *
 * <p>The only class most games ever import from this module is
 * {@link org.flixelgdx.backend.html5.FlixelHtml5Launcher FlixelHtml5Launcher}. Create a TeaVM
 * entry point class in your web module and call it with one line:
 *
 * <pre>{@code
 * public final class WebLauncher {
 *   public static void main(String[] args) {
 *     FlixelHtml5Launcher.launch(new MyGame());
 *   }
 *   private WebLauncher() {}
 * }
 * }</pre>
 *
 * <p>The two-argument overload accepts a
 * {@link org.flixelgdx.backend.FlixelRuntimeMode FlixelRuntimeMode} to force a fixed mode,
 * bypassing the URL parameter resolution the single-argument version uses.
 *
 * <h2>Runtime mode</h2>
 *
 * <p>A desktop game reads its runtime mode from a JVM property ({@code -Dflixel.mode=debug}). A
 * browser game has no launch arguments, so the web backend reads from the page URL instead.
 * Appending {@code ?flixel.mode=debug} (or the shorthand {@code ?debug}) to the address starts the
 * game in {@link org.flixelgdx.backend.FlixelRuntimeMode#DEBUG DEBUG} without a rebuild.
 * {@code ?flixel.mode=test} selects {@link org.flixelgdx.backend.FlixelRuntimeMode#TEST TEST}.
 * With no such parameter the game runs in
 * {@link org.flixelgdx.backend.FlixelRuntimeMode#RELEASE RELEASE}, unless the build plugin baked
 * in a different default through the {@code flixelgdx { mode = ... }} DSL option.
 *
 * <h2>What the launcher installs</h2>
 *
 * <p>A call to {@link org.flixelgdx.backend.html5.FlixelHtml5Launcher#launch(org.flixelgdx.FlixelGame)
 * FlixelHtml5Launcher.launch(...)} wires up the following:
 * <ul>
 *   <li><b>Window</b> -
 *       {@link org.flixelgdx.backend.html5.FlixelHtml5Window FlixelHtml5Window} maps the
 *       {@link org.flixelgdx.backend.FlixelWindow FlixelWindow} contract onto a single HTML
 *       {@code <canvas>} element. Tab title, canvas dimensions, fullscreen (via the Fullscreen
 *       API), and focus state are supported. Operations with no browser equivalent (window
 *       position, opacity, decorations) are no-ops.</li>
 *   <li><b>Graphics</b> -
 *       {@link org.flixelgdx.backend.html5.graphics.FlixelHtml5Graphics FlixelHtml5Graphics} in
 *       the {@code graphics} sub-package creates a WebGL2 context on the canvas and implements the
 *       full rendering API: texture upload, batched sprite drawing via
 *       {@link org.flixelgdx.backend.html5.graphics.FlixelWebGlBatch FlixelWebGlBatch}, render
 *       targets, and custom shaders compiled at runtime from GLSL source strings.</li>
 *   <li><b>Audio</b> -
 *       {@link org.flixelgdx.backend.html5.audio.FlixelWebAudioFactory FlixelWebAudioFactory} in
 *       the {@code audio} sub-package provides multi-channel playback, per-sound volume and pan
 *       controls, and sound groups through the Web Audio API. Audio effects (reverb, echo,
 *       low-pass) are not supported on the web and degrade to the framework's shared no-ops.</li>
 *   <li><b>Input</b> -
 *       {@link org.flixelgdx.backend.html5.input.FlixelHtml5InputDevice FlixelHtml5InputDevice}
 *       translates DOM keyboard, mouse, wheel, and touch events into the core input API, using
 *       physical {@code KeyboardEvent.code} mapping so layout-independent keys work correctly.
 *       {@link org.flixelgdx.backend.html5.input.FlixelHtml5GamepadProvider FlixelHtml5GamepadProvider}
 *       adds gamepad support through the Web Gamepad API with the standard layout mapping.</li>
 *   <li><b>File system</b> -
 *       {@link org.flixelgdx.backend.html5.file.FlixelHtml5Files FlixelHtml5Files} in the
 *       {@code file} sub-package backs asset reads from a warm in-memory cache populated before
 *       the game starts, and backs save-data reads and writes with {@code localStorage}.</li>
 *   <li><b>Asset preloading</b> -
 *       {@link org.flixelgdx.backend.html5.asset.FlixelHtml5AssetPreloader FlixelHtml5AssetPreloader}
 *       downloads every bundled asset (listed in an {@code assets/assets.txt} manifest generated
 *       by the build plugin) before the game loop starts, so asset reads inside game code are
 *       always synchronous cache lookups rather than network round-trips.</li>
 *   <li><b>Host integration</b> -
 *       {@link org.flixelgdx.backend.html5.FlixelHtml5HostIntegration FlixelHtml5HostIntegration}
 *       bridges the Notifications API for toasts, the Clipboard API for copy and paste, the Screen
 *       Wake Lock API to keep the display awake, and {@code beforeunload} for an exit
 *       confirmation.</li>
 * </ul>
 *
 * <h2>The game loop</h2>
 *
 * <p>{@link org.flixelgdx.backend.html5.FlixelHtml5Runner FlixelHtml5Runner} drives the game loop
 * from {@code requestAnimationFrame} rather than a blocking {@code while} loop, because the
 * browser's single thread would freeze the tab if blocked. Each callback advances the game by one
 * frame and schedules the next one. The runner also wires the browser resize and tab-visibility
 * events so the canvas tracks the page and audio and updates pause when the tab is hidden.
 *
 * @see org.flixelgdx.backend.html5.FlixelHtml5Launcher
 * @see org.flixelgdx.backend.html5.FlixelHtml5Runner
 * @see org.flixelgdx.backend.html5.FlixelHtml5Window
 */
package org.flixelgdx.backend.html5;
