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

import org.flixelgdx.asset.FlixelAssetManager;
import org.flixelgdx.asset.FlixelAssetMode;
import org.flixelgdx.asset.FlixelNoopAssetManager;
import org.flixelgdx.audio.FlixelNoopSoundFactory;
import org.flixelgdx.audio.FlixelSoundFactory;
import org.flixelgdx.audio.FlixelSoundManager;
import org.flixelgdx.backend.FlixelAlerter;
import org.flixelgdx.backend.FlixelGameRunner;
import org.flixelgdx.backend.FlixelHaptics;
import org.flixelgdx.backend.FlixelHostIntegration;
import org.flixelgdx.backend.FlixelNoopAlerter;
import org.flixelgdx.backend.FlixelNoopHaptics;
import org.flixelgdx.backend.FlixelNoopHostIntegration;
import org.flixelgdx.backend.FlixelNoopRuntimeDevice;
import org.flixelgdx.backend.FlixelNoopWindow;
import org.flixelgdx.backend.FlixelPlatform;
import org.flixelgdx.backend.FlixelRuntimeDevice;
import org.flixelgdx.backend.FlixelRuntimeMode;
import org.flixelgdx.backend.FlixelWindow;
import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.debug.FlixelDebugManager;
import org.flixelgdx.debug.FlixelDebugOverlay;
import org.flixelgdx.debug.FlixelDebugWatchManager;
import org.flixelgdx.debug.FlixelHeadlessDebugOverlay;
import org.flixelgdx.file.FlixelFiles;
import org.flixelgdx.file.FlixelNoopFiles;
import org.flixelgdx.functional.FlixelAntialiasable;
import org.flixelgdx.functional.FlixelDrawable;
import org.flixelgdx.graphics.FlixelBatch;
import org.flixelgdx.graphics.FlixelGraphicsManager;
import org.flixelgdx.graphics.FlixelNoopGraphicsManager;
import org.flixelgdx.group.FlixelGroupable;
import org.flixelgdx.input.FlixelInputDevice;
import org.flixelgdx.input.FlixelKeyboardListener;
import org.flixelgdx.input.FlixelMouseListener;
import org.flixelgdx.input.FlixelNoopInputDevice;
import org.flixelgdx.input.FlixelTouchListener;
import org.flixelgdx.input.gamepad.FlixelGamepadAxis;
import org.flixelgdx.input.gamepad.FlixelGamepadButton;
import org.flixelgdx.input.gamepad.FlixelGamepadInputManager;
import org.flixelgdx.input.keyboard.FlixelKeyInputManager;
import org.flixelgdx.input.mouse.FlixelMouseButton;
import org.flixelgdx.input.mouse.FlixelMouseInputManager;
import org.flixelgdx.input.touch.FlixelTouch;
import org.flixelgdx.input.touch.FlixelTouchManager;
import org.flixelgdx.logging.FlixelLogConsoleSink;
import org.flixelgdx.logging.FlixelLogFileHandler;
import org.flixelgdx.logging.FlixelLogMode;
import org.flixelgdx.logging.FlixelLogger;
import org.flixelgdx.logging.FlixelNoopStackTraceProvider;
import org.flixelgdx.logging.FlixelStackTraceProvider;
import org.flixelgdx.math.FlixelRandom;
import org.flixelgdx.math.FlixelVector;
import org.flixelgdx.tween.FlixelTween;
import org.flixelgdx.tween.type.FlixelAngleTween;
import org.flixelgdx.tween.type.FlixelColorTween;
import org.flixelgdx.tween.type.FlixelFlickerTween;
import org.flixelgdx.tween.type.FlixelGoalTween;
import org.flixelgdx.tween.type.FlixelNumTween;
import org.flixelgdx.tween.type.FlixelShakeTween;
import org.flixelgdx.tween.type.motion.FlixelCircularMotion;
import org.flixelgdx.tween.type.motion.FlixelCubicMotion;
import org.flixelgdx.tween.type.motion.FlixelLinearMotion;
import org.flixelgdx.tween.type.motion.FlixelLinearPath;
import org.flixelgdx.tween.type.motion.FlixelQuadMotion;
import org.flixelgdx.tween.type.motion.FlixelQuadPath;
import org.flixelgdx.util.FlixelExceptionUtil;
import org.flixelgdx.util.save.FlixelSave;
import org.flixelgdx.util.signal.FlixelSignal;
import org.flixelgdx.util.signal.FlixelSignalData.StateSwitchSignalData;
import org.flixelgdx.util.signal.FlixelSignalData.UpdateSignalData;
import org.flixelgdx.util.timer.FlixelTimer;
import org.flixelgdx.util.timer.FlixelTimerListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The static singleton entry point and global manager for the FlixelGDX framework.
 *
 * <p>This class exposes core services, settings, and utility methods needed to develop games and interactive
 * applications using FlixelGDX. Nearly all main gameplay logic interacts with Flixel via this class, either to control
 * the playback loop, switch states/scenes, access global systems (input, audio, asset management, logging, debugging),
 * or modify global properties.
 *
 * <h2>Core Responsibilities</h2>
 * <ul>
 *   <li>
 *     <b>State Management:</b>
 *     Switches between {@link FlixelState} instances to manage major scenes in your game.
 *   </li>
 *   <li>
 *     <b>Input Handling:</b>
 *     Provides access to the keyboard manager ({@link #keys}) for polling key states and input events.
 *   </li>
 *   <li>
 *     <b>Sound System:</b>
 *     Exposes a global {@link #sound} manager for playing music and sound effects.
 *   </li>
 *   <li>
 *     <b>Asset Loading:</b>
 *     Offers a unified {@link #assets} interface for loading, caching, and retrieving textures, sounds, and data.
 *   </li>
 *   <li>
 *     <b>Host integration:</b>
 *     Desktop notifications and task attention via {@link #host}. Separate from blocking
 *     {@link FlixelAlerter#info(String, String)} dialogs.
 *   </li>
 *   <li>
 *     <b>Window control:</b>
 *     Transparency helpers and desktop window tweaks via {@link #window}.
 *   </li>
 *   <li>
 *     <b>Logging and Debugging:</b>
 *     Centralizes log output through {@link #log}, and supplies tools for in-game watches and performance tracking.
 *   </li>
 *   <li>
 *     <b>Camera and Drawing Context:</b>
 *     Handles the active camera selection and global antialiasing options.
 *   </li>
 *   <li>
 *     <b>Signals and Events:</b>
 *     Emits signals for state switches, updates, and critical events.
 *   </li>
 *   <li>
 *     <b>Frame timers:</b>
 *     {@link FlixelTimer#getGlobalManager()} is stepped from {@link FlixelGame}.
 *     Use {@link FlixelTimer#wait(float, FlixelTimerListener)} or {@code start(...)} on the manager.
 *     {@link #timeScale} scales timer elapsed only (not the whole game loop).
 *   </li>
 * </ul>
 *
 * <h2>Typical Usage</h2>
 *
 * <pre>{@code
 * // Switch states.
 * Flixel.switchState(() -> new MyGameState());
 *
 * // Play a sound.
 * Flixel.sound.play("explosion.mp3");
 *
 * // Check if a key is pressed.
 * if (Flixel.keys.justPressed(FlixelKeys.SPACE)) {
 *   // Jump!
 * }
 *
 * // Check if a mouse button is pressed.
 * if (Flixel.mouse.justPressed(FlixelMouseButton.LEFT)) {
 *   // Left mouse button was just pressed!
 * }
 *
 * // Log diagnostic information.
 * Flixel.info("Player has reached checkpoint.");
 * Flixel.warn("Player is low on health.");
 * Flixel.error("Game crashed!");
 *
 * // Load an asset.
 * Flixel.assets.load("player.png");
 *
 * // Use the global signal system.
 * Flixel.Signals.preStateSwitch.add(data -> {
 *   Flixel.info("Now switching to state: " + data.state().toString());
 * });
 * }</pre>
 *
 * <h2>Design Notes</h2>
 * <ul>
 *   <li>
 *     The {@code Flixel} class is <em>not</em> meant to be instantiated. It should be interacted with strictly via
 *     its static fields and methods.
 *   </li>
 *   <li>
 *     Custom configuration and subsystems can be plugged in by replacing or augmenting the static references, e.g.,
 *     custom {@link FlixelLogger} or {@link FlixelStackTraceProvider} for advanced logging.
 *   </li>
 *   <li>
 *     All engine systems are globally accessible through this class to simplify game logic implementation.
 *   </li>
 * </ul>
 *
 * <h2>Threading</h2>
 * <p>
 * All Flixel APIs, unless otherwise noted, are intended to be called from the main rendering thread.
 * </p>
 *
 * <h2>Lifecycle</h2>
 * <p>
 * The Flixel singleton is initialized by the internal game bootstrap sequence. Applications should not attempt to
 * reinitialize or replace this class directly.
 * </p>
 *
 * @author stringdotjar
 */
public final class Flixel {

  /**
   * Minimum allowed elapsed time in seconds for one frame. {@link FlixelGame} clamps the raw frame delta to
   * at least this value so a zero-delta does not break motion and timers.
   */
  public static final float MIN_ELAPSED = 0.000001f;

  /**
   * Maximum allowed elapsed time in seconds for one frame. {@link FlixelGame} clamps the raw frame delta
   * to at most this value so a long hitch does not move physics too far in one step.
   */
  public static final float MAX_ELAPSED = 0.1f;

  /**
   * Automatically applies the globally set {@link #setAntialiasing(boolean) antialiasing} value for any
   * member added to the current {@link FlixelState}.
   *
   * <p>Note that when this value is set to {@code true}, any {@link FlixelSprite}'s / {@link FlixelAntialiasable}'s
   * antialiasing property will be ignored.
   */
  public static boolean applyAntialiasingOnStateAdd = false;

  /**
   * The active {@link FlixelGame} instance driving the game lifecycle.
   *
   * <p>This reference is set during {@link #start(FlixelGame, FlixelGameRunner)} before any other system is
   * brought up. It exposes the main render loop, the camera list, window dimensions, and low-level
   * controls such as fullscreen toggling and framerate caps.
   *
   * <p>Most game code never needs to touch {@code Flixel.game} directly. Prefer the specialized
   * fields ({@link #sound}, {@link #assets}, {@link #keys}, {@link #cameras}, etc.) for day-to-day
   * work. Reach for this field only when the high-level APIs do not cover what you need.
   *
   * <p>Example:
   * <pre>{@code
   * // Change the game's background color.
   * Flixel.game.bgColor.set(Color.BLUE);
   * }</pre>
   */
  @NotNull
  public static FlixelGame game;

  /**
   * The global list of active {@link FlixelCamera cameras}, ordered back-to-front.
   *
   * <p>The first entry, {@code Flixel.cameras.first()}, is the main camera that the framework follows
   * and renders to by default. Add more cameras for split-screen, picture-in-picture, or separate UI
   * layers.
   *
   * <p>This reference is {@code final} and never {@code null}: the framework creates a default camera
   * at startup and refreshes the list (in place) on a state switch, so it is safe to read from your
   * state's {@code create()} onward. Mutate it through the list's own methods (for example
   * {@link FlixelArray#add}); the array itself is never replaced.
   *
   * <p>Example:
   * <pre>{@code
   * // The main camera.
   * FlixelCamera main = Flixel.cameras.first();
   * main.follow(player);
   *
   * // A second camera for a UI overlay.
   * Flixel.cameras.add(new FlixelCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
   * }</pre>
   */
  @NotNull
  public static final FlixelArray<FlixelCamera> cameras = new FlixelArray<>(FlixelCamera[]::new);

  /**
   * The framework's default random number generator.
   *
   * <p>This is a ready-to-use {@link FlixelRandom} for the common case where you
   * just want "a random number" without managing your own generator. Because it is
   * seedable, you can make an entire run reproducible by calling
   * {@link FlixelRandom#setSeed(long)} once at startup, which is invaluable for
   * deterministic replays, seeded procedural generation, and debugging.
   *
   * <p>Create your own {@link FlixelRandom} instead when you need an independent,
   * separately-seeded stream (for example, cosmetic effects that should not
   * disturb gameplay determinism).
   *
   * <p>Example:
   * <pre>{@code
   * int damage = Flixel.random.nextInt(8, 12);
   * if (Flixel.random.nextBool(0.1f)) {
   *   spawnRareDrop();
   * }
   * }</pre>
   */
  @NotNull
  public static final FlixelRandom random = new FlixelRandom();

  /**
   * The platform-specific alert dialog provider.
   *
   * <p>Alert dialogs are blocking modal windows that pause execution until the user dismisses them.
   * Reserve them for critical events (unrecoverable errors, required permission prompts) rather
   * than routine game feedback. For non-blocking OS notifications that do not interrupt gameplay,
   * use {@link #host} instead.
   *
   * <p>Example:
   * <pre>{@code
   * Flixel.alert.info("Hello", "Welcome to the game!");
   * Flixel.alert.warn("Warning", "These permissions are required to continue.");
   * Flixel.alert.error("Save Failed", "Could not write to disk. Check your permissions.");
   * }</pre>
   */
  @NotNull
  public static FlixelAlerter alert = FlixelNoopAlerter.INSTANCE;

  /**
   * The currently active state.
   *
   * <p>This is the {@link FlixelState} that is updated and drawn every frame. Switch to a
   * different state using {@link #switchState(Supplier)}; reading this field directly is useful
   * when you need to query the current state without holding a separate reference (for example,
   * inside a signal listener or a global utility method).
   *
   * <p>This field is {@code null} until the first call to {@link #switchState(Supplier)}.
   */
  @Nullable
  public static FlixelState state;

  /**
   * Factory that produces a fresh instance of the current state for {@link #resetState()}.
   *
   * <p>Updated automatically whenever {@link #switchState(Supplier)} (or any of its overloads)
   * is called. The factory is stored as-is, so calling {@code resetState()} re-invokes it to get
   * a brand-new state instance.
   *
   * <p>Set this to {@code null} to disable {@link #resetState()} for the current state, making
   * it a no-op.
   */
  @Nullable
  public static Supplier<FlixelState> currentStateFactory = null;

  /**
   * The keyboard input manager for the game.
   *
   * <p>Poll key states every frame using the methods on this manager. FlixelGDX distinguishes
   * between keys that are held down, freshly pressed on the current frame, and freshly released
   * on the current frame, so your game logic can respond precisely to each event type.
   *
   * <p>Key constants are defined in {@link org.flixelgdx.input.keyboard.FlixelKey FlixelKey}. Pass any of
   * those constants to the methods below:
   * <ul>
   *   <li>{@link FlixelKeyInputManager#pressed(int)} - {@code true} while the key is held down.</li>
   *   <li>{@link FlixelKeyInputManager#justPressed(int)} - {@code true} on the first frame the key is pressed.</li>
   *   <li>{@link FlixelKeyInputManager#justReleased(int)} - {@code true} on the first frame the key is released.</li>
   * </ul>
   *
   * <p>Example:
   * <pre>{@code
   * // Check if the spacebar is held.
   * if (Flixel.keys.pressed(FlixelKey.SPACE)) {
   *   player.jump();
   * }
   *
   * // Fire a bullet only on the first frame the key is pressed.
   * if (Flixel.keys.justPressed(FlixelKey.Z)) {
   *   player.shoot();
   * }
   * }</pre>
   *
   * @see org.flixelgdx.input.keyboard.FlixelKey
   */
  @NotNull
  public static FlixelKeyInputManager keys;

  /**
   * The central audio manager.
   *
   * <p>Use this manager to play sound effects and background music. It separates
   * one-shot sound effects from long-running music tracks and exposes independent volume
   * controls for each category plus a master volume knob that scales both.
   *
   * <p>The audio backend is platform-specific and is injected by the launcher before
   * {@link #start(FlixelGame, FlixelGameRunner)}. On all platforms, it's typically powered by miniaudio,
   * and for web (TeaVM) it utilizes the Web Audio API.
   *
   * <p>Example:
   * <pre>{@code
   * // Play a one-shot sound effect.
   * Flixel.sound.play("hit.wav");
   *
   * // Start looping background music at half volume.
   * Flixel.sound.playMusic("theme.ogg", 0.5f, true);
   *
   * // Mute all audio.
   * Flixel.sound.setMasterVolume(0f);
   * }</pre>
   */
  @NotNull
  public static FlixelSoundManager sound;

  /**
   * The central asset manager for the game.
   *
   * <p>Use this manager to load, cache, and retrieve any external resource your game needs:
   * textures, audio clips, fonts, JSON data files, and more. The manager tracks which assets have
   * been loaded so the same file is never loaded twice and automatically unloads non-persistent
   * assets when states switch, freeing GPU and heap memory between scenes.
   *
   * <p>Assets marked persistent (see {@link FlixelAssetManager#load(String, boolean)}) survive
   * state switches and are ideal for shared resources such as a global UI atlas or a music track
   * that spans multiple states.
   *
   * <p>Example:
   * <pre>{@code
   * // Load a texture and retrieve it.
   * Flixel.assets.load("player.png");
   * FlixelGraphic tex = Flixel.assets.get<FlixelGraphic>("player.png").get();
   *
   * // Mark an asset persistent so it survives state switches.
   * Flixel.assets.setPersist("shared_ui_atlas.png", true);
   * }</pre>
   */
  @NotNull
  public static FlixelAssetManager assets = FlixelNoopAssetManager.INSTANCE;

  /**
   * The debug watch manager for the game.
   *
   * <p>A watch is a named, live value that the debug overlay displays while the game runs. Watches
   * are an efficient way to inspect frame-by-frame state (player position, health, physics
   * variables) without scattering temporary log statements throughout your code.
   *
   * <p>Add a watch with a name and a supplier lambda; the overlay calls the supplier every frame
   * to refresh the displayed value. Remove watches you no longer need to keep the overlay clean.
   * Suppliers are never called when the overlay is hidden, so there is no runtime cost in release
   * builds.
   *
   * <p>Example:
   * <pre>{@code
   * // Show the player's position live in the debug overlay.
   * Flixel.watch.add("Player X", player::getX);
   * Flixel.watch.add("Player Y", player::getY);
   * Flixel.watch.add("Total Score", () -> orbs + gemsAmount);
   *
   * // Remove a watch when it is no longer needed.
   * Flixel.watch.remove("Player X");
   * }</pre>
   */
  @NotNull
  public static FlixelDebugWatchManager watch;

  /**
   * The central debug manager for the game.
   *
   * <p>The debug manager is the primary interface to FlixelGDX's built-in debugging toolset. It
   * bridges game code and the active {@link FlixelDebugOverlay} without requiring a direct
   * reference to the overlay or even knowing whether one is installed. All methods are safe to
   * call in any build and no-op gracefully when the game is not running in debug mode.
   *
   * <p>Key capabilities:
   * <ul>
   *   <li>
   *     <b>Visibility toggle:</b> Call {@link FlixelDebugManager#toggleVisible()} or
   *     {@link FlixelDebugManager#setVisible(boolean)} to show or hide the overlay at runtime.
   *   </li>
   *   <li>
   *     <b>Draw debug:</b> {@link FlixelDebugManager#setDrawDebug(boolean)} enables bounding-box
   *     and collision-shape visualization over every live object in the current state.
   *   </li>
   *   <li>
   *     <b>Custom commands:</b> Register interactive console commands with
   *     {@link FlixelDebugManager#registerCommand(String, Consumer)} to run
   *     arbitrary game code from the debug overlay's input line.
   *   </li>
   * </ul>
   *
   * <p>The overlay itself is created by a factory set before the game starts. Desktop launchers
   * typically supply a richer overlay (for example, one built with Dear ImGui), while headless or
   * web builds fall back to {@link FlixelHeadlessDebugOverlay}. Use
   * {@link Flixel#setDebugOverlay(Supplier)} to install a custom factory before
   * {@link #start(FlixelGame, FlixelGameRunner)}.
   *
   * <p>Example:
   * <pre>{@code
   * // Show the debug overlay when the game starts (debug mode only).
   * Flixel.debug.setVisible(true);
   *
   * // Draw bounding boxes over all objects.
   * Flixel.debug.setDrawDebug(true);
   *
   * // Register a custom console command.
   * Flixel.debug.registerCommand("god", args -> player.setInvincible(true));
   * }</pre>
   */
  @NotNull
  public static FlixelDebugManager debug;

  /**
   * The preferences-based save data helper for the game.
   *
   * <p>{@link FlixelSave} is a simple key-value store that persists between sessions. It is
   * backed by platform-native storage through the file seam: a save file on desktop, browser
   * {@code localStorage} on web, and the equivalent on mobile.
   *
   * <p>Call {@link FlixelSave#bind(String, String)} once before using any other method to open (or
   * create) the named preferences file. After that, read and write values directly through the
   * {@link FlixelSave#data} map, then flush them to disk with {@link FlixelSave#flush()}.
   *
   * <p>Example:
   * <pre>{@code
   * // Open the save file (typically in FlixelState.create()).
   * Flixel.save.bind("MySaveFile", "slot1");
   *
   * // Read and write a high score.
   * int prevBest = (int) Flixel.save.data.get("highScore", 0);
   * if (score > prevBest) {
   *   Flixel.save.data.put("highScore", score);
   *   Flixel.save.flush();
   * }
   * }</pre>
   */
  @NotNull
  public static FlixelSave save;

  /**
   * The mouse and pointer input manager for the game.
   *
   * <p>Poll button states and screen-space coordinates every frame. Like {@link #keys}, this
   * manager distinguishes between buttons that are currently held, just pressed this frame, and
   * just released this frame, so your code can react precisely to each event. Button constants are
   * defined in {@link FlixelMouseButton}.
   *
   * <p>Example:
   * <pre>{@code
   * // Fire on the first frame the left button is pressed.
   * if (Flixel.mouse.justPressed(FlixelMouseButton.LEFT)) {
   *   spawnBullet(Flixel.mouse.getWorldX(), Flixel.mouse.getWorldY());
   * }
   *
   * // Pan the camera while the right button is held.
   * if (Flixel.mouse.pressed(FlixelMouseButton.RIGHT)) {
   *   camera.pan(Flixel.mouse.getDeltaX(), Flixel.mouse.getDeltaY());
   * }
   * }</pre>
   *
   * @see FlixelMouseButton
   */
  @NotNull
  public static FlixelMouseInputManager mouse;

  /**
   * The multitouch input manager for the game.
   *
   * <p>Tracks up to {@link FlixelTouchManager#DEFAULT_MAX_POINTERS} simultaneous fingers. Access
   * per-pointer state through the pre-allocated {@link FlixelTouchManager#list list} array, or use
   * the convenience methods for quick checks:
   *
   * <pre>{@code
   * // React on first contact.
   * if (Flixel.touches.list[0].isJustPressed()) {
   *   spawnEffect(Flixel.touches.list[0].worldX, Flixel.touches.list[0].worldY);
   * }
   *
   * // Check how many fingers are down.
   * if (Flixel.touches.count() >= 2) {
   *   beginPinchZoom();
   * }
   * }</pre>
   *
   * @see FlixelTouchManager
   * @see FlixelTouch
   */
  @NotNull
  public static FlixelTouchManager touches;

  /**
   * The gamepad and controller input manager.
   *
   * <p>The gamepad system is <strong>disabled by default</strong>. Set {@link FlixelGamepadInputManager#enabled} to
   * {@code true} before the game loop starts if your game needs controller support:
   *
   * <pre>{@code
   * Flixel.gamepads.enabled = true;
   * }</pre>
   *
   * <p>The gamepad system abstracts physical controllers (Xbox, PlayStation, generic USB) behind
   * logical button and axis tokens from {@link FlixelGamepadButton} and {@link FlixelGamepadAxis}, so
   * the same game code works across different controller layouts without any platform-specific
   * branching. The same tokens build custom mappings, so a button minted with
   * {@link FlixelGamepadButton#of(String)} is polled exactly like the built-in ones.
   *
   * <p>Each connected controller is identified by a zero-based index. Player 1's controller is
   * index {@code 0}, player 2's is index {@code 1}, and so on. Query button states with
   * {@link FlixelGamepadInputManager#pressed(int, FlixelGamepadButton)},
   * {@link FlixelGamepadInputManager#justPressed(int, FlixelGamepadButton)}, and
   * {@link FlixelGamepadInputManager#justReleased(int, FlixelGamepadButton)}, or read analog axes with
   * {@link FlixelGamepadInputManager#getAxis(int, FlixelGamepadAxis)}.
   *
   * <p>Example:
   * <pre>{@code
   * // Opt in to controller support before the game loop starts.
   * Flixel.gamepads.enabled = true;
   *
   * // Check if player 1 pressed the A button this frame.
   * if (Flixel.gamepads.justPressed(0, FlixelGamepadButton.A)) {
   *   player.jump();
   * }
   *
   * // Read the left stick's horizontal axis for movement.
   * float horizontal = Flixel.gamepads.getAxis(0, FlixelGamepadAxis.LEFT_X);
   * player.setVelocityX(horizontal * MOVE_SPEED * elapsed);
   * }</pre>
   *
   * @see FlixelGamepadButton
   * @see FlixelGamepadAxis
   */
  @NotNull
  public static FlixelGamepadInputManager gamepads;

  /**
   * The default logger used for game diagnostics and debugging output.
   *
   * <p>The logger formats and routes messages to the console, the in-game debug overlay, and
   * optionally a persistent log file. It supports three severity levels: informational
   * ({@link FlixelLogger#info}), warnings ({@link FlixelLogger#warn}), and errors
   * ({@link FlixelLogger#error}), each visually distinguished by color in terminals that support
   * ANSI codes.
   *
   * <p>For convenience, the three most common logging calls are promoted to static methods on
   * {@link Flixel} itself: {@link Flixel#info}, {@link Flixel#warn}, and {@link Flixel#error}.
   * These all delegate to this field.
   *
   * <p>Each message is automatically annotated with the calling class name and line number by the
   * active {@link FlixelStackTraceProvider}, making it easy to trace output back to its source
   * without a full stack dump.
   *
   * <p>To write logs to a file, call {@link FlixelLogger#startFileLogging()} after configuring
   * {@link FlixelLogger#setCanStoreLogs(boolean)} and optionally
   * {@link FlixelLogger#setLogsFolder(String)}.
   *
   * <p>Example:
   * <pre>{@code
   * // Simple convenience wrappers on Flixel itself.
   * Flixel.info("Player spawned.");
   * Flixel.warn("Low health!");
   * Flixel.error("Save file corrupted.");
   *
   * // Create a log with a distinguishing tag.
   * Flixel.info("AI", "Pathfinding recalculated.");
   *
   * // Enable persistent file logging at startup.
   * Flixel.log.setCanStoreLogs(true);
   * Flixel.log.startFileLogging();
   * }</pre>
   */
  @NotNull
  public static FlixelLogger log = new FlixelLogger(FlixelLogMode.SIMPLE);

  /**
   * Desktop window integration for transparency helpers, opacity control, and OS-level window tweaks.
   *
   * <p>On desktop (LWJGL3), this field is replaced by a real implementation before
   * {@link Flixel#start(FlixelGame, FlixelGameRunner)} runs. On an unknown platform it falls back to
   * {@link FlixelNoopWindow}, which silently ignores every call, so you can always write
   * {@code Flixel.window.setOpacity(0.8f)} without wrapping it in a platform check.
   *
   * <p>Typical uses include transparent window backgrounds for overlay-style applications,
   * programmatic window repositioning, and toggling window decorations (title bar, border) at
   * runtime.
   *
   * <p>Example:
   * <pre>{@code
   * // Make the window semi-transparent.
   * Flixel.window.setOpacity(0.85f);
   *
   * // Remove the title bar and border.
   * Flixel.window.setDecorated(false);
   * }</pre>
   */
  @NotNull
  public static FlixelWindow window = FlixelNoopWindow.INSTANCE;

  /**
   * The single public surface for drawing and GPU state (the sprite batch, frame rate, vertical
   * sync, display modes, and pixel density).
   *
   * <p>The active graphics backend is installed here before {@link Flixel#start(FlixelGame, FlixelGameRunner)}
   * runs. Until then, and on headless targets, it falls back to {@link FlixelNoopGraphicsManager},
   * so calls are always safe to make. The underlying GPU library is never exposed; game code only
   * ever talks to this manager.
   *
   * <p>Example:
   *
   * <pre>{@code
   * // Grab the shared batch used throughout the game.
   * FlixelBatch batch = Flixel.graphics.getBatch();
   *
   * // Cap the frame rate and turn on vertical sync.
   * Flixel.graphics.setTargetFps(60);
   * Flixel.graphics.setVSync(true);
   * }</pre>
   */
  @NotNull
  public static FlixelGraphicsManager graphics = FlixelNoopGraphicsManager.INSTANCE;

  /**
   * Host integration for toast notifications, taskbar attention signals, clipboard access and more.
   *
   * <p>This manager is typically what you would use to interact with the operating system or
   * environment the game is currently running on. A common case is checking what platform
   * is assigned for specific functionality, such as checking if it's on mobile and a mobile
   * button should be added.
   *
   * <p>Notifications can differ depending on the platform. For web, you need to request
   * permission first before sending one, otherwise {@link FlixelHostIntegration#supportsNotifications()}
   * will return false and sending a notification will do nothing. On mobile platforms like
   * Android, you typically need to add a permission to a config before you can send one.
   *
   * <p>Example:
   * <pre>{@code
   * // Show a non-blocking OS notification when a download finishes.
   * Flixel.host.requestNotificationPermission();
   * if (Flixel.host.supportsNotifications()) {
   *   Flixel.host.sendNotification("Download complete", "Your level pack is ready to play.");
   * }
   *
   * // Flash the taskbar button to get the user's attention.
   * Flixel.host.requestAttention();
   *
   * // Check the current platform.
   * if (Flixel.host.getPlatform() == FlixelPlatform.Android) {
   *   addBackButton();
   * }
   * }</pre>
   */
  @NotNull
  public static FlixelHostIntegration host = FlixelNoopHostIntegration.INSTANCE;

  /**
   * Haptic (vibration) feedback for mobile devices.
   *
   * <p>On platforms without a vibration motor (desktop, web) this defaults to a silent no-op, so
   * all calls are always safe to make. Check {@link FlixelHaptics#isSupported()} first if your
   * game logic needs to know whether feedback will actually fire.
   *
   * <p>Example:
   *
   * <pre>{@code
   * // Short pulse when the player takes damage.
   * Flixel.haptics.vibrate(60);
   *
   * // Repeating heartbeat pattern (restarts from index 0) while a timer is critical.
   * Flixel.haptics.vibrate(new long[] { 0, 80, 120, 80, 600 }, 0);
   *
   * // Cancel when the timer ends.
   * Flixel.haptics.cancel();
   * }</pre>
   *
   * @see FlixelHaptics
   */
  @NotNull
  public static FlixelHaptics haptics = FlixelNoopHaptics.INSTANCE;

  /**
   * The low-level input backend that reads the keyboard and pointer for the current platform.
   *
   * <p>This is the plumbing beneath {@link #keys}, {@link #mouse}, and {@link #touches}: those
   * managers poll it each frame and register their event listeners with it. Game code rarely
   * touches it directly, but it is available for a quick poll or to register a custom
   * {@link FlixelKeyboardListener}, {@link FlixelMouseListener}, or {@link FlixelTouchListener}.
   *
   * <p>The active backend is installed here before {@link Flixel#start(FlixelGame, FlixelGameRunner)}.
   * Until then (and on headless sessions) it is a safe no-op that reports nothing pressed, so reads are always
   * safe.
   *
   * <p>Example:
   *
   * <pre>{@code
   * // Register a custom keyboard listener.
   * Flixel.input.addKeyboardListener(new FlixelKeyboardListener() {
   *   @Override
   *   public boolean keyDown(int keycode) {
   *     if (keycode == FlixelKey.F7) {
   *       triggerDebugFunction();
   *     }
   *   }
   * });
   *
   * // Check keyboard press directly instead of using the polled keys manager.
   * if (Flixel.input.isKeyPressed(FlixelKey.ESCAPE)) {
   *   openPauseMenu();
   * }
   * }</pre>
   *
   * @see FlixelInputDevice
   */
  @NotNull
  public static FlixelInputDevice input = FlixelNoopInputDevice.INSTANCE;

  /**
   * The file system used to read assets, save data, and any other files the game needs.
   *
   * <p>This is the seam beneath every file read in FlixelGDX. Open a handle from one of its roots
   * (for example {@link FlixelFiles#internal internal} for bundled assets or
   * {@link FlixelFiles#external external} for save data), then query or read it. Asset loading, fonts,
   * and sounds all resolve their files through here.
   *
   * <p>The active backend is installed here before {@link Flixel#start(FlixelGame, FlixelGameRunner)}.
   * Until then (and on headless sessions) it is a safe no-op that reports every file as missing, so reads
   * never crash.
   *
   * <p>Example:
   *
   * <pre>{@code
   * FlixelFile level = Flixel.files.internal("levels/level1.json");
   * if (level.exists()) {
   *   parseLevel(level.readString());
   * }
   * }</pre>
   *
   * @see org.flixelgdx.file.FlixelFiles
   */
  @NotNull
  public static FlixelFiles files = FlixelNoopFiles.INSTANCE;

  /**
   * The current machine and program layout: memory usage, whether the game runs from a JAR or an
   * IDE, the working directory, and where log files should go.
   *
   * <p>All of this is platform-specific, so like {@link #host} and {@link #window} it is a backend
   * seam. Desktop JVM builds install {@code FlixelJvmRuntimeDevice} here before
   * {@link Flixel#start(FlixelGame, FlixelGameRunner)}. Until then (and on platforms that cannot
   * report it) it falls back to {@link FlixelNoopRuntimeDevice}, so calls are always safe.
   *
   * <p>Example:
   *
   * <pre>{@code
   * if (Flixel.runtime.isRunningFromJar()) {
   *   // Behavior for a packaged build.
   * }
   * }</pre>
   *
   * @see FlixelRuntimeDevice
   */
  @NotNull
  public static FlixelRuntimeDevice runtime = FlixelNoopRuntimeDevice.INSTANCE;

  /**
   * Global timescale applied to the game's update loop each frame.
   *
   * <p>{@code 1f} is normal speed; values below {@code 1f} slow the game down, values above {@code 1f} speed it up.
   * The raw platform delta is clamped to [{@link #MIN_ELAPSED}, {@link #MAX_ELAPSED}] first, then multiplied by
   * this value before being stored in {@link #elapsed} and passed to {@code update()}. All systems that read
   * {@link #getElapsed()} (such as physics, animations, tweens, timers, and camera follow) are affected uniformly.
   *
   * <p>The debug overlay's Controls panel exposes a slider for this value at runtime (range 0.1x to 4.0x).
   */
  public static float timeScale = 1f;

  /** The capped elapsed time for the current frame. Set by {@link FlixelGame} after clamping the raw frame delta. */
  static float elapsed = 0f;

  /** The raw clamped platform delta before {@link #timeScale} is applied. Set by {@link FlixelGame} each frame. */
  static float rawElapsed = 0f;

  /** Has the global manager been initialized yet? */
  static boolean initialized = false;

  /**
   * World bounds used by {@link #overlap} and {@link #collide} for broad-phase culling.
   * Format: {@code [x, y, width, height]}. Defaults to a very large area.
   */
  private static final float[] worldBounds = { -10000f, -10000f, 20000f, 20000f };

  /** The camera currently being drawn in {@link FlixelDrawable#draw(FlixelBatch)}. */
  @Nullable
  private static FlixelCamera drawCamera;

  /** System used to detect where a log comes from when a log is created. **/
  @NotNull
  public static FlixelStackTraceProvider stackTraceProvider = FlixelNoopStackTraceProvider.INSTANCE;

  /**
   * Platform-specific handler for writing log output to a file. May be
   * {@code null} on platforms that do not support file logging (e.g., web/TeaVM).
   */
  @Nullable
  public static FlixelLogFileHandler logFileHandler;

  /**
   * When non-null, {@link FlixelLogger} sends each console line here instead of {@code System.out}
   * (for example, styled output in the browser). Set before {@link #start(FlixelGame, FlixelGameRunner)}.
   */
  @Nullable
  public static FlixelLogConsoleSink logConsoleSink;

  /**
   * Platform-specific factory for creating sounds and sound groups.
   * Set by the launcher before {@link #start(FlixelGame, FlixelGameRunner)}; defaults to a silent no-op so audio
   * calls are always safe.
   */
  @NotNull
  public static FlixelSoundFactory soundFactory = FlixelNoopSoundFactory.INSTANCE;

  /**
   * Callbacks run at the very top of {@link #start(FlixelGame, FlixelGameRunner)}, before any core system is
   * created. Both the framework and games may add to this list: use it to install or replace
   * implementations (a custom asset manager, logger, sound factory) before anything reads them.
   */
  @NotNull
  public static final FlixelArray<Runnable> beforeStart = new FlixelArray<>();

  /**
   * Callbacks run at the end of {@link #start(FlixelGame, FlixelGameRunner)}, after every
   * core system exists but before the platform runner takes over the loop. Use these to tweak
   * fully constructed systems (register asset loaders, add signal listeners) right before the
   * game runs.
   */
  @NotNull
  public static final FlixelArray<Runnable> afterStart = new FlixelArray<>();

  /** The runtime mode (TEST, DEBUG, RELEASE) set by the launcher. */
  private static FlixelRuntimeMode runtimeMode = FlixelRuntimeMode.RELEASE;

  /**
   * Factory used to create the debug overlay when the game starts. Developers can replace
   * this with their own subclass via {@link #setDebugOverlay(Supplier)} before the game
   * starts (i.e. in the launcher, before {@link FlixelGame#create()} runs).
   */
  private static Supplier<FlixelDebugOverlay> debugOverlayFactory = FlixelHeadlessDebugOverlay::new;

  /** Should the game use antialiasing globally? */
  private static boolean antialiasing = false;

  /** Whether the game is running in debug mode. Can only be set once from the launcher. */
  private static boolean debugMode = false;

  /** Guard that ensures {@link #setDebugMode(boolean)} is only called once. */
  private static boolean debugModeSet = false;

  /** Guard that ensures {@link #setRuntimeMode(FlixelRuntimeMode)} is only called once. */
  private static boolean runtimeModeSet = false;

  /**
   * Starts the entire Flixel system and runs the game.
   *
   * <p>This is the single entry point a launcher calls. It runs the {@link #beforeStart}
   * callbacks, wires up every core system Flixel needs ({@link FlixelAssetManager}, the audio
   * system, input managers, the logger, and more), runs the {@link #afterStart} callbacks, and
   * finally hands the game to the given platform {@link FlixelGameRunner}, which owns the
   * update/draw loop from there.
   *
   * <p>Missing platform pieces never crash startup: any backend hook that was not installed
   * (alerter, sound factory, stack trace provider) simply stays a safe no-op. Advanced users can
   * swap implementations from an {@link #afterStart} callback before any core system reads them.
   *
   * <p>Game code does not call this directly. The platform launcher builds the backend runner and
   * calls it for you, so starting a game is a single line:
   *
   * <pre>{@code
   * public static void main(String[] args) {
   *   FlixelDesktopLauncher.launch(new MyGame());
   * }
   * }</pre>
   *
   * @param gameInstance The {@link FlixelGame} instance to run.
   * @param runner The platform runner that owns the window and the update/draw loop. Use
   *     {@link FlixelGameRunner#NOOP} for headless sessions and tests.
   */
  public static void start(@NotNull FlixelGame gameInstance, @NotNull FlixelGameRunner runner) {
    if (initialized) {
      warn("Flixel.start(...) was called more than once; ignoring the extra call.");
      return;
    }

    // Always configure the crash handler before anything else.
    runtime.setCrashHandler((thread, throwable) -> {
      String logs = FlixelExceptionUtil.getFullExceptionMessage(throwable);
      String threadName = thread != null ? thread.getName() : "main";
      String msg = "There was an uncaught exception on thread \"" + threadName + "\"!\n" + logs;
      error(msg);
      alert.error("Uncaught Exception", msg);
      // It's possible that something in beforeStart can throw an exception, so we add a
      // guard here before the game object is assigned, because who wants a crash handler
      // that crashes while trying to say it crashed?
      if (game != null) {
        game.destroy();
      }
      // Only quit on non-iOS platforms to avoid App Store guideline violations!
      if (host.getPlatform() != FlixelPlatform.iOS) {
        window.setAbsorbCloseRequests(false);
        exit();
      }
    });

    for (int i = 0; i < beforeStart.getSize(); i++) {
      beforeStart.get(i).run();
    }

    game = gameInstance;

    // Initialize the core systems.
    keys = new FlixelKeyInputManager();
    watch = new FlixelDebugWatchManager();
    debug = new FlixelDebugManager();
    save = new FlixelSave();
    mouse = new FlixelMouseInputManager();
    touches = new FlixelTouchManager();
    gamepads = new FlixelGamepadInputManager();
    sound = new FlixelSoundManager(soundFactory);

    // Register default tween pools (pool factories avoid extra allocations when pooling tweens).
    FlixelTween.registerTweenType(FlixelGoalTween.class, () -> new FlixelGoalTween(null))
        .registerTweenType(FlixelNumTween.class, () -> new FlixelNumTween(0, 0, null, null))
        .registerTweenType(FlixelAngleTween.class, () -> new FlixelAngleTween(null))
        .registerTweenType(FlixelColorTween.class, () -> new FlixelColorTween(null))
        .registerTweenType(FlixelShakeTween.class, () -> new FlixelShakeTween(null))
        .registerTweenType(FlixelFlickerTween.class, () -> new FlixelFlickerTween(null))
        .registerTweenType(FlixelLinearMotion.class, () -> new FlixelLinearMotion(null))
        .registerTweenType(FlixelCircularMotion.class, () -> new FlixelCircularMotion(null))
        .registerTweenType(FlixelQuadMotion.class, () -> new FlixelQuadMotion(null))
        .registerTweenType(FlixelCubicMotion.class, () -> new FlixelCubicMotion(null))
        .registerTweenType(FlixelLinearPath.class, () -> new FlixelLinearPath(null))
        .registerTweenType(FlixelQuadPath.class, () -> new FlixelQuadPath(null));

    initialized = true;

    for (int i = 0; i < afterStart.getSize(); i++) {
      afterStart.get(i).run();
    }

    runner.run(gameInstance);
  }

  /**
   * Switches to the state produced by the given factory.
   *
   * <p>The factory is called immediately to obtain the new state, then stored so that
   * {@link #resetState()} can call it again later to get a fresh instance.
   *
   * <p>Example:
   * <pre>{@code
   * Flixel.switchState(() -> new GameplayState());
   * }</pre>
   *
   * @param stateFactory A factory that returns the new {@link FlixelState} to enter. Must not be
   *     {@code null} and must not return {@code null}.
   */
  public static void switchState(@NotNull Supplier<FlixelState> stateFactory) {
    switchState(stateFactory, true);
  }

  /**
   * Switches to the state produced by the given factory.
   *
   * @param stateFactory A factory that returns the new {@link FlixelState} to enter. Must not be
   *     {@code null} and must not return {@code null}.
   * @param clearTweens Should all active tweens be canceled and their pools be cleared?
   */
  public static void switchState(@NotNull Supplier<FlixelState> stateFactory, boolean clearTweens) {
    switchState(stateFactory, clearTweens, true);
  }

  /**
   * Switches to the state produced by the given factory.
   *
   * @param stateFactory A factory that returns the new {@link FlixelState} to enter. Must not be
   *     {@code null} and must not return {@code null}.
   * @param clearTweens Should all active tweens be canceled and their pools be cleared?
   * @param clearTimers Should all active timers be canceled?
   */
  public static void switchState(@NotNull Supplier<FlixelState> stateFactory, boolean clearTweens,
      boolean clearTimers) {
    switchState(stateFactory, clearTweens, clearTimers, true);
  }

  /**
   * Switches to the state produced by the given factory.
   *
   * <p>This is the primary overload all others delegate to. The factory is invoked once to
   * obtain the state, then retained as {@link #currentStateFactory} so {@link #resetState()}
   * can produce a fresh instance without any extra work from the caller.
   *
   * @param stateFactory A factory that returns the new {@link FlixelState} to enter. Must not be
   *     {@code null} and must not return {@code null}.
   * @param clearTweens Should all active tweens be canceled and their pools be cleared?
   * @param clearTimers Should all active timers be canceled?
   * @param triggerGC Should Java's garbage collector be triggered for memory cleanup?
   */
  public static void switchState(@NotNull Supplier<FlixelState> stateFactory, boolean clearTweens, boolean clearTimers,
      boolean triggerGC) {
    Objects.requireNonNull(stateFactory, "State factory cannot be null.");
    FlixelState newState = stateFactory.get();

    Signals.preStateSwitch.dispatch(new StateSwitchSignalData(state));

    if (!initialized) {
      throw new IllegalStateException("Flixel has not been initialized yet.");
    }
    if (state != null) {
      state.destroy();
    }

    FlixelAssetMode mode = assets.getAssetMode();
    if (mode == FlixelAssetMode.STANDARD || mode == FlixelAssetMode.AGGRESSIVE) {
      if (sound != null) {
        sound.clearNonPersist();
      }
      if (assets != null) {
        assets.clearNonPersist();
      }
    }
    if (clearTweens) {
      FlixelTween.cancelActiveTweens();
      FlixelTween.clearTweenPools();
    }
    if (clearTimers) {
      FlixelTimer.cancelAll();
    }
    game.resetCameras();
    state = Objects.requireNonNull(newState, "State factory must not return null.");
    state.ensureMembers();
    state.create();
    currentStateFactory = stateFactory;

    if (triggerGC) {
      System.gc();
    }

    Signals.postStateSwitch.dispatch(new StateSwitchSignalData(state));
  }

  /**
   * Logs a debug message using the default tag.
   *
   * @param message The message to log.
   */
  public static void debug(Object message) {
    log.debug(message);
  }

  /**
   * Logs a debug message under a custom tag.
   *
   * @param tag The tag to log the message under.
   * @param message The message to log.
   */
  public static void debug(String tag, Object message) {
    log.debug(tag, message);
  }

  /**
   * Logs a debug message under a custom tag, replacing each {@code {}} placeholder with
   * the corresponding argument in order.
   *
   * @param tag The tag to log the message under.
   * @param message The format string, where each {@code {}} is replaced by the next argument.
   * @param args The arguments to substitute into the message.
   */
  public static void debug(String tag, Object message, Object... args) {
    log.debug(tag, message, args);
  }

  /**
   * Logs a generic informational message. This is likely the method you'll use the most,
   * as it's for general messages that don't fit into the other log methods.
   *
   * @param message The message to log.
   */
  public static void info(Object message) {
    info(log.getDefaultTag(), message);
  }

  /**
   * Logs a generic informational message with a custom tag. This is likely the method
   * you'll use the most, as it's for general messages that don't fit into the other log methods.
   *
   * @param tag The tag to log the message under.
   * @param message The message to log.
   */
  public static void info(String tag, Object message) {
    log.info(tag, message);
  }

  /**
   * Logs an informational message under a custom tag, replacing each {@code {}} placeholder
   * with the corresponding argument in order.
   *
   * @param tag The tag to log the message under.
   * @param message The format string, where each {@code {}} is replaced by the next argument.
   * @param args The arguments to substitute into the message.
   */
  public static void info(String tag, Object message, Object... args) {
    log.info(tag, message, args);
  }

  /**
   * Logs a generic warning message. This is for messages that are not errors, but are
   * still important to note.
   *
   * @param message The message to log.
   */
  public static void warn(Object message) {
    warn(log.getDefaultTag(), message);
  }

  /**
   * Logs a warning message with a custom tag. This is for messages that are not errors
   * but are still important to note.
   *
   * @param tag The tag to log the message under.
   * @param message The message to log.
   */
  public static void warn(String tag, Object message) {
    log.warn(tag, message);
  }

  /**
   * Logs a warning message under a custom tag, replacing each {@code {}} placeholder with
   * the corresponding argument in order.
   *
   * @param tag The tag to log the message under.
   * @param message The format string, where each {@code {}} is replaced by the next argument.
   * @param args The arguments to substitute into the message.
   */
  public static void warn(String tag, Object message, Object... args) {
    log.warn(tag, message, args);
  }

  /**
   * Logs an error message with red highlighting (and the file location underlined).
   * This is for events that are typically not recoverable.
   *
   * @param message The message to log.
   */
  public static void error(String message) {
    log.error(message);
  }

  /**
   * Logs an error message with red highlighting (and the file location underlined), including
   * the throwable's string representation.
   *
   * @param message The message to log.
   * @param throwable The throwable to log.
   */
  public static void error(Object message, Throwable throwable) {
    log.error(message, throwable);
  }

  /**
   * Logs an error message with red highlighting (and the file location underlined) under
   * a custom tag.
   *
   * @param tag The tag to log the message under.
   * @param message The message to log.
   */
  public static void error(String tag, Object message) {
    log.error(tag, message);
  }

  /**
   * Logs an error message with red highlighting (and the file location underlined) under
   * a custom tag, including the throwable's string representation.
   *
   * @param tag The tag to log the message under.
   * @param message The message to log.
   * @param throwable The throwable to log.
   */
  public static void error(String tag, Object message, Throwable throwable) {
    log.error(tag, message, throwable);
  }

  /**
   * Logs an error message using the default tag, replacing each {@code {}} placeholder with
   * the corresponding argument in order, including the throwable's string representation.
   *
   * @param message The format string, where each {@code {}} is replaced by the next argument.
   * @param throwable The throwable to log.
   * @param args The arguments to substitute into the message.
   */
  public static void error(Object message, Throwable throwable, Object... args) {
    log.error(message, throwable, args);
  }

  /**
   * Logs an error message under a custom tag, replacing each {@code {}} placeholder with
   * the corresponding argument in order.
   *
   * @param tag The tag to log the message under.
   * @param message The format string, where each {@code {}} is replaced by the next argument.
   * @param args The arguments to substitute into the message.
   */
  public static void error(String tag, Object message, Object... args) {
    log.error(tag, message, args);
  }

  /**
   * Logs an error message under a custom tag, replacing each {@code {}} placeholder with
   * the corresponding argument in order, including the throwable's string representation.
   *
   * @param tag The tag to log the message under.
   * @param message The format string, where each {@code {}} is replaced by the next argument.
   * @param throwable The throwable to log.
   * @param args The arguments to substitute into the message.
   */
  public static void error(String tag, Object message, Throwable throwable, Object... args) {
    log.error(tag, message, throwable, args);
  }

  /**
   * Returns the game's fixed design width in game pixels, as set by {@link FlixelGame.Config}.
   *
   * <p>This is the width your game logic is authored against. It never changes at runtime, no matter
   * the window size, fullscreen state, render resolution, or viewport policy, so it is the value to
   * use when laying things out against the game's own coordinate space (for example, centering).
   *
   * <p>If you instead want the width of the world that is actually visible on screen (which an
   * extend-style viewport can grow beyond the design width) use {@link #getVisibleWidth()}.
   *
   * @return The fixed design width in game pixels.
   */
  public static int getDesignWidth() {
    return game != null ? game.getInitialWidth() : getVisibleWidth();
  }

  /**
   * Returns the game's fixed design height in game pixels, as set by {@link FlixelGame.Config}.
   *
   * @return The fixed design height in game pixels.
   * @see #getDesignWidth()
   */
  public static int getDesignHeight() {
    return game != null ? game.getInitialHeight() : getVisibleHeight();
  }

  /**
   * Returns the width of the world that is currently visible on screen, in game pixels.
   *
   * <p>When cameras are active this is the first camera's viewport world width, which reflects the
   * active viewport policy: it equals {@link #getDesignWidth()} for a letterboxing (FIT) viewport,
   * but an extend-style viewport grows it to fill the screen. Unlike {@link #getDesignWidth()}, this
   * can change when the window is resized, so reach for it (not the design width) when you want to
   * scale or place things relative to the actual visible screen area.
   *
   * @return The visible world width in game pixels.
   */
  public static int getVisibleWidth() {
    if (!cameras.isEmpty()) {
      return (int) cameras.first().getWorldWidth();
    }
    return game.getInitialWidth();
  }

  /**
   * Returns the height of the world that is currently visible on screen, in game pixels.
   *
   * @return The visible world height in game pixels.
   * @see #getVisibleWidth()
   */
  public static int getVisibleHeight() {
    if (!cameras.isEmpty()) {
      return (int) cameras.first().getWorldHeight();
    }
    return game.getInitialHeight();
  }

  /**
   * Returns the game's fixed design size in game pixels, as set in the {@link FlixelGame.Config}.
   *
   * <p>This matches {@link #getDesignWidth()} / {@link #getDesignHeight()} and, unlike
   * {@link #getVisibleWidth()} / {@link #getVisibleHeight()}, always reflects the fixed design
   * dimensions set at startup, unaffected by the window size or viewport type.
   */
  public static FlixelVector getSize() {
    return new FlixelVector(game.getInitialWidth(), game.getInitialHeight());
  }

  /**
   * Requests that the game quits.
   *
   * <p>This is a convenience that forwards to {@link FlixelWindow#close() Flixel.window.close()}. If
   * the window is absorbing close requests (see {@link FlixelWindow#setAbsorbCloseRequests(boolean)}),
   * that still applies. On web and mobile, where the host owns the lifecycle, this may do nothing.
   */
  public static void exit() {
    window.close();
  }

  /**
   * Returns the capped elapsed time (in seconds) for the current frame. This value is clamped
   * between {@link #MIN_ELAPSED} and {@link #MAX_ELAPSED} by
   * {@link FlixelGame} each frame.
   */
  public static float getElapsed() {
    return elapsed;
  }

  /**
   * Returns the raw platform delta (in seconds) for the current frame, clamped to
   * [{@link #MIN_ELAPSED}, {@link #MAX_ELAPSED}] but not multiplied by {@link #timeScale}.
   * Use this when you need actual wall-clock frame time regardless of the active time scale.
   */
  public static float getRawElapsed() {
    return rawElapsed;
  }

  /**
   * Sets whether the game is running in debug mode. This may only be called <strong>once</strong>,
   * typically from the platform launcher before the game starts. A second call throws an
   * {@link IllegalStateException}.
   *
   * @param enabled {@code true} to enable debug mode.
   * @throws IllegalStateException If called more than once.
   */
  public static void setDebugMode(boolean enabled) {
    if (debugModeSet) {
      throw new IllegalStateException("Debug mode can only be set once (from the launcher).");
    }
    debugMode = enabled;
    debugModeSet = true;
  }

  public static boolean isDebugMode() {
    return debugMode;
  }

  /**
   * Sets the runtime mode for the game. This may only be called <strong>once</strong>, typically
   * from the platform launcher before the game starts. A second call throws an
   * {@link IllegalStateException}.
   *
   * @param mode The {@link FlixelRuntimeMode} to set.
   * @throws IllegalStateException If called more than once.
   */
  public static void setRuntimeMode(@NotNull FlixelRuntimeMode mode) {
    if (runtimeModeSet) {
      throw new IllegalStateException("Runtime mode can only be set once (from the launcher).");
    }
    runtimeMode = mode;
    runtimeModeSet = true;
  }

  /** Returns the current runtime mode. Defaults to {@link FlixelRuntimeMode#RELEASE}. */
  public static FlixelRuntimeMode getRuntimeMode() {
    return runtimeMode;
  }

  /**
   * Refreshes the current state by invoking the factory last set by {@link #switchState(Supplier)}.
   * Does nothing if the factory is {@code null}.
   *
   * <p>This is the equivalent of calling {@code Flixel.switchState(() -> new CurrentState())}.
   */
  public static void resetState() {
    Objects.requireNonNull(game, "Game is not initialized. Call start(...) first.");
    Supplier<FlixelState> factory = currentStateFactory;
    if (factory != null) {
      switchState(factory, true, true, true);
    }
  }

  /**
   * Sets a factory that produces the {@link FlixelDebugOverlay} used when debug mode is
   * enabled. This can be called either in the launcher (before the game starts) or in the
   * {@link FlixelGame#create()} method itself.
   *
   * <p>A factory is used instead of a new instance directly for timing, so that way the
   * debug overlay can be set even before GL context is created.
   *
   * <p>The default factory builds {@link FlixelHeadlessDebugOverlay} (no extra UI panels). Desktop
   * launchers normally replace this with a richer overlay (for example, Dear ImGui) before
   * {@link Flixel#start}.
   *
   * <p>Example:
   * <pre>{@code
   * Flixel.setDebugOverlay(MyCustomOverlay::new);
   * }</pre>
   *
   * @param factory A supplier that creates a new {@link FlixelDebugOverlay} (or subclass).
   */
  public static void setDebugOverlay(@NotNull Supplier<FlixelDebugOverlay> factory) {
    debugOverlayFactory = factory;
  }

  /**
   * Creates the debug overlay using the registered factory. Called internally by
   * {@link FlixelGame} during startup when debug mode is enabled.
   */
  static FlixelDebugOverlay createDebugOverlay() {
    debug.overlay = debugOverlayFactory.get();
    return debug.overlay;
  }

  /**
   * The camera currently being drawn in {@link FlixelDrawable#draw(FlixelBatch)},
   * or {@code null} if not in a camera pass.
   */
  @Nullable
  public static FlixelCamera getDrawCamera() {
    return drawCamera;
  }

  static void setDrawCamera(@Nullable FlixelCamera camera) {
    drawCamera = camera;
  }

  /**
   * Whether something with the given {@code cameras} list should render during the current draw pass.
   * {@code null} or an empty array means all cameras; otherwise, the object is drawn only if {@link #getDrawCamera()}
   * is reference-equal to an entry.
   */
  public static boolean isOnDrawCamera(@Nullable FlixelCamera[] cameras) {
    FlixelCamera active = drawCamera;
    if (active == null) {
      return true;
    }
    if (cameras == null || cameras.length == 0) {
      return true;
    }
    for (FlixelCamera c : cameras) {
      if (c == active) {
        return true;
      }
    }
    return false;
  }

  public static boolean isAntialiasing() {
    return antialiasing;
  }

  /**
   * Sets antialiasing to be applied to all {@link FlixelSprite} / {@link FlixelAntialiasable} objects.
   *
   * <p>Calling this function automatically updates the current state. Note that if you want this
   * value to actually apply to any {@link FlixelSprite} / {@link FlixelAntialiasable} object being
   * added to a {@link FlixelState}, set {@link #applyAntialiasingOnStateAdd} to {@code true}.
   *
   * <p>After propagating the new value to every state member, this method calls
   * {@link #refreshAntialiasing()} to guarantee that members sharing the same underlying texture
   * end up with a consistent filter. See that method's documentation for details.
   *
   * @param enabled If antialiasing should be applied to all current
   *     {@link FlixelSprite} / {@link FlixelAntialiasable} objects.
   */
  public static void setAntialiasing(boolean enabled) {
    if (enabled == antialiasing) {
      return;
    }
    antialiasing = enabled;

    if (state == null) {
      return;
    }

    var members = state.getMembers();
    if (members == null) {
      return;
    }
    var mbrs = members.begin();
    for (int i = 0; i < members.getSize(); i++) {
      var member = mbrs[i];
      if (member == null) {
        continue;
      }
      if (member instanceof FlixelAntialiasable m) {
        m.setAntialiasing(enabled);
      }
    }
    members.end();
    refreshAntialiasing();
  }

  /**
   * Re-applies texture filter settings for every {@link FlixelAntialiasable} member of the current
   * state in a two-pass order that resolves conflicts on shared textures.
   *
   * <p>OpenGL texture filtering is a property of the texture object itself, not of each individual
   * sprite. When multiple sprites share the same underlying texture (for example, every arrow in a
   * rhythm game's strumline atlas), the last call sets the filter for the texture directly, regardless
   * of which sprite made the call. This can leave sprites with {@code antialiasing = true} rendering with
   * a nearest-neighbor filter if a sprite with {@code antialiasing = false} that
   * uses the same texture was set up last.
   *
   * <p>This method fixes that by always processing members in two passes:
   * <ol>
   *   <li>First, all members whose {@code antialiasing} flag is {@code false} re-apply Nearest.</li>
   *   <li>Then, all members whose {@code antialiasing} flag is {@code true} re-apply Linear.</li>
   * </ol>
   * Because the Linear pass runs last, any texture shared between a "smooth" and a "pixelated"
   * member will end up with a linear (bilinear) filter, matching what a developer who requested
   * antialiasing would expect.
   *
   * <p>Call this after finishing the setup of a state (for example, at the end of
   * {@link FlixelState#create()}), especially when some state members share textures but have
   * different antialiasing settings.
   *
   * <p>Example usage:
   * <pre>{@code
   * @Override
   * public void create() {
   *   // ... add all sprites, groups, etc. ...
   *   Flixel.refreshAntialiasing();
   * }
   * }</pre>
   */
  public static void refreshAntialiasing() {
    if (state == null) {
      return;
    }
    var members = state.getMembers();
    if (members == null) {
      return;
    }

    var mbrs = members.begin();
    for (int i = 0; i < members.getSize(); i++) {
      var member = mbrs[i];
      if (member instanceof FlixelAntialiasable m && !m.isAntialiasing()) {
        m.setAntialiasing(false);
      }
    }
    members.end();

    mbrs = members.begin();
    for (int i = 0; i < members.getSize(); i++) {
      var member = mbrs[i];
      if (member instanceof FlixelAntialiasable m && m.isAntialiasing()) {
        m.setAntialiasing(true);
      }
    }
    members.end();
  }

  /**
   * Returns the world bounds used for collision broad-phase culling.
   * The returned array is {@code [x, y, width, height]}.
   */
  public static float[] getWorldBounds() {
    return worldBounds;
  }

  /**
   * Sets the world bounds used for collision culling.
   *
   * @param x Left edge of the world.
   * @param y Top edge of the world.
   * @param width Width of the world in pixels.
   * @param height Height of the world in pixels.
   */
  public static void setWorldBounds(float x, float y, float width, float height) {
    worldBounds[0] = x;
    worldBounds[1] = y;
    worldBounds[2] = width;
    worldBounds[3] = height;
  }

  /**
   * Checks for overlaps between two objects or groups. Can be called with
   * any combination of single {@link FlixelObject}s and {@link FlixelGroupable}s.
   *
   * @param objectOrGroup1 First object or group (may be {@code null} to use the current state).
   * @param objectOrGroup2 Second object or group (may be {@code null} to use the current state).
   * @param notifyCallback Called for each overlapping pair. May be {@code null}.
   * @param processCallback If provided, must return {@code true} for the pair to count as overlapping.
   *   Pass {@code null} for simple AABB overlap.
   * @return {@code true} if any overlaps were detected.
   */
  public static boolean overlap(@Nullable FlixelBasic objectOrGroup1,
      @Nullable FlixelBasic objectOrGroup2,
      @Nullable BiConsumer<FlixelObject, FlixelObject> notifyCallback,
      @Nullable BiFunction<FlixelObject, FlixelObject, Boolean> processCallback) {
    if (objectOrGroup1 == null)
      objectOrGroup1 = state;
    if (objectOrGroup2 == null)
      objectOrGroup2 = state;
    if (objectOrGroup1 == null || objectOrGroup2 == null)
      return false;
    return overlapInternal(objectOrGroup1, objectOrGroup2, notifyCallback, processCallback);
  }

  /**
   * Shorthand for {@link #overlap(FlixelBasic, FlixelBasic, BiConsumer, BiFunction)}
   * with no callbacks.
   */
  public static boolean overlap(@Nullable FlixelBasic objectOrGroup1, @Nullable FlixelBasic objectOrGroup2) {
    return overlap(objectOrGroup1, objectOrGroup2, null, null);
  }

  /**
   * Checks for overlaps and separates colliding objects. Equivalent to calling
   * {@link #overlap} with {@link FlixelObject#separate} as the process callback.
   *
   * @param objectOrGroup1 First object or group.
   * @param objectOrGroup2 Second object or group.
   * @param notifyCallback Called for each pair that was separated. May be {@code null}.
   * @return {@code true} if any objects were separated.
   */
  public static boolean collide(@Nullable FlixelBasic objectOrGroup1,
      @Nullable FlixelBasic objectOrGroup2,
      @Nullable BiConsumer<FlixelObject, FlixelObject> notifyCallback) {
    return overlap(objectOrGroup1, objectOrGroup2, notifyCallback, FlixelObject::separate);
  }

  /**
   * Shorthand for {@link #collide(FlixelBasic, FlixelBasic, BiConsumer)} with
   * no {@code notifyCallback}.
   */
  public static boolean collide(@Nullable FlixelBasic objectOrGroup1,
      @Nullable FlixelBasic objectOrGroup2) {
    return collide(objectOrGroup1, objectOrGroup2, null);
  }

  private static boolean overlapInternal(FlixelBasic obj1, FlixelBasic obj2,
      BiConsumer<FlixelObject, FlixelObject> notifyCallback,
      BiFunction<FlixelObject, FlixelObject, Boolean> processCallback) {
    boolean result = false;

    if (obj1 instanceof FlixelGroupable<?> group1) {
      FlixelArray<?> members = group1.getMembers();
      if (members != null) {
        for (Object o : members) {
          if (o instanceof FlixelBasic member && member.isExists()) {
            result |= overlapInternal(member, obj2, notifyCallback, processCallback);
          }
        }
      }
      return result;
    }

    if (obj2 instanceof FlixelGroupable<?> group2) {
      FlixelArray<?> members = group2.getMembers();
      if (members != null) {
        for (Object o : members) {
          if (o instanceof FlixelBasic member && member.isExists()) {
            result |= overlapInternal(obj1, member, notifyCallback, processCallback);
          }
        }
      }
      return result;
    }

    if (!(obj1 instanceof FlixelObject fo1) || !(obj2 instanceof FlixelObject fo2))
      return false;
    if (obj1 == obj2)
      return false;
    if (!fo1.exists || !fo2.exists)
      return false;

    boolean overlaps = fo1.getX() < fo2.getX() + fo2.getWidth()
        && fo1.getX() + fo1.getWidth() > fo2.getX()
        && fo1.getY() < fo2.getY() + fo2.getHeight()
        && fo1.getY() + fo1.getHeight() > fo2.getY();

    if (!overlaps) {
      return false;
    }

    if (processCallback != null) {
      if (!processCallback.apply(fo1, fo2)) {
        return false;
      }
    }

    if (notifyCallback != null) {
      notifyCallback.accept(fo1, fo2);
    }

    return true;
  }

  /**
   * Returns the version of the FlixelGDX library.
   *
   * <p>The version is read from a {@code version.properties} file in the module {@code .jar} file,
   * where it is defined as {@code version=<version>}. If the file is not found, or the version is not
   * defined, the method returns {@code "Unknown"}, although this should never happen in theory.
   *
   * @return The version of the FlixelGDX library.
   */
  public static String getVersion() {
    try (InputStream in = Flixel.class.getResourceAsStream("version.properties")) {
      if (in != null) {
        Properties p = new Properties();
        p.load(in);
        String v = p.getProperty("version");
        if (v != null && !v.isEmpty())
          return v;
      }
    } catch (Exception ignored) {
      // Ignored.
    }
    return "Unknown";
  }

  /**
   * Contains all the global events that get dispatched when something happens in the game.
   *
   * <p>This includes anything from the screen being switched, the game updating every frame, and
   * just about everything you can think of.
   *
   * <p><b>NOTE:</b> Anything with the {@code pre} and {@code post} prefixes always mean the
   * same thing. If a signal has {@code pre}, then the signal gets ran BEFORE any functionality is
   * executed, and {@code post} means AFTER all functionality was executed.
   */
  public static final class Signals {

    public static final FlixelSignal<UpdateSignalData> preUpdate = new FlixelSignal<>();
    public static final FlixelSignal<UpdateSignalData> postUpdate = new FlixelSignal<>();
    public static final FlixelSignal<Void> preDraw = new FlixelSignal<>();
    public static final FlixelSignal<Void> postDraw = new FlixelSignal<>();
    public static final FlixelSignal<StateSwitchSignalData> preStateSwitch = new FlixelSignal<>();
    public static final FlixelSignal<StateSwitchSignalData> postStateSwitch = new FlixelSignal<>();
    public static final FlixelSignal<Void> preGameClose = new FlixelSignal<>();
    public static final FlixelSignal<Void> postGameClose = new FlixelSignal<>();
    public static final FlixelSignal<Void> windowFocused = new FlixelSignal<>();
    public static final FlixelSignal<Void> windowUnfocused = new FlixelSignal<>();
    public static final FlixelSignal<Void> windowMinimized = new FlixelSignal<>();

    private Signals() {}
  }

  private Flixel() {}
}
