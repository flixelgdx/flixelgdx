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
package org.flixelgdx.debug;

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelGame;
import org.flixelgdx.FlixelObject;
import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.graphics.FlixelBatch;
import org.flixelgdx.graphics.FlixelGraphicsManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The single entry point for everything related to the FlixelGDX debugger.
 *
 * <p>An instance of this class is automatically created when {@link Flixel#start(org.flixelgdx.FlixelGame, org.flixelgdx.backend.FlixelGameRunner)}
 * runs and is exposed as the static field {@link Flixel#debug}, mirroring how
 * {@link Flixel#sound}, {@link Flixel#assets}, and friends work. From your game code you can do things
 * like:
 *
 * <pre>{@code
 * Flixel.debug.toggleVisible();
 * Flixel.debug.setDrawDebug(true);
 * Flixel.debug.commands.register("hello", args -> Flixel.info("Hi!"));
 * Flixel.debug.commands.execute("hello");
 *
 * // Customize keybinds via the overlay field (null-safe: only set after debug mode starts).
 * if (Flixel.debug.overlay != null) {
 *   Flixel.debug.overlay.toggleKey = FlixelKey.F1;
 * }
 * }</pre>
 *
 * <p>The manager is intentionally lightweight: it forwards visibility/hitbox toggles to the active
 * {@link FlixelDebugOverlay}, exposes the console command registry through {@link #commands}, and tracks the
 * "currently inspected sprite" for the texture inspector window. The overlay itself (and the
 * platform-specific UI) reads from the manager rather than the other way around so the manager can
 * stay platform-agnostic and reflection-free.
 *
 * <h2>Custom commands</h2>
 *
 * <p>Use {@link FlixelDebugCommandManager#register(String, Consumer)} via {@link #commands} with a handler that receives
 * {@link FlixelDebugCommandArgs}. That keeps parsing explicit and avoids reflection, which is
 * important on platforms where reflection is restricted (TeaVM, R8/ProGuard-shrunk Android
 * builds).
 *
 * <h2>Thread safety</h2>
 *
 * <p>All public methods are intended to be called from the game's main thread. Reading the
 * registered commands map outside the main thread is unsupported. If you must run it on the main
 * thread, you can use {@link FlixelGraphicsManager#queueMainThread Flixel.graphics.queueMainThread(...)} and
 * run a debug method there instead.
 */
public class FlixelDebugManager {

  private Supplier<FlixelDebugOverlay> overlayFactory = FlixelHeadlessDebugOverlay::new;

  /**
   * Extra {@link FlixelBatch} instances registered by game code via {@link #trackBatch(FlixelBatch)}.
   * The framework's own batch is counted separately in {@link FlixelDebugOverlay}, so only user-supplied
   * batches live here.
   */
  private final FlixelArray<FlixelBatch> trackedBatches = new FlixelArray<>(false, 4);

  /**
   * Custom entries shown in the overlay's Tracker panel, registered via
   * {@link #addTrackerEntry(FlixelDebugTrackerEntry)}.
   */
  private final FlixelArray<FlixelDebugTrackerEntry> trackerEntries = new FlixelArray<>(FlixelDebugTrackerEntry[]::new);

  /**
   * The active debug overlay. Defaults to {@link FlixelNoopDebugOverlay#INSTANCE} so this field
   * is never {@code null}, meaning callers do not need a null check. When debug mode starts,
   * {@link Flixel} replaces this with a real overlay instance.
   *
   * <p>Access keybinds and visibility via this field:
   * <pre>{@code
   * Flixel.debug.overlay.toggleKey = FlixelKey.F1;
   * Flixel.debug.overlay.setVisible(true);
   * }</pre>
   */
  public FlixelDebugOverlay overlay = FlixelNoopDebugOverlay.INSTANCE;

  /**
   * The console command registry. Register custom commands and run command lines through it:
   * <pre>{@code
   * Flixel.debug.commands.register("god", args -> player.setInvincible(true));
   * Flixel.debug.commands.execute("god");
   * }</pre>
   */
  public final FlixelDebugCommandManager commands;

  /** The sprite currently selected by the LMB picker, or {@code null}. */
  @Nullable
  private FlixelObject inspectedSprite;

  /** The sprite currently being dragged, or {@code null} if no drag is in progress. */
  @Nullable
  private FlixelObject draggedSprite;

  /** Creates the debug manager and its {@link #commands} registry with the always-available commands. */
  public FlixelDebugManager() {
    commands = new FlixelDebugCommandManager(this);
  }

  /**
   * Sets the factory that produces the {@link FlixelDebugOverlay} when debug mode is enabled.
   *
   * <p>Call this before {@link Flixel#start} (for example, in the launcher or in
   * {@link FlixelGame#create()}) to install a custom overlay. The factory is only invoked once,
   * when the game actually starts in debug mode; calling this after the overlay is already created
   * has no effect on the running overlay.
   *
   * <p>The default factory builds {@link FlixelHeadlessDebugOverlay}. Desktop launchers typically
   * replace this with a richer overlay before the game starts.
   *
   * <p>Example:
   * <pre>{@code
   * Flixel.debug.setOverlayFactory(MyCustomOverlay::new);
   * }</pre>
   *
   * @param factory A supplier that creates a new {@link FlixelDebugOverlay} (or subclass). Must not be null.
   */
  public void setOverlayFactory(@NotNull Supplier<FlixelDebugOverlay> factory) {
    if (factory != null) {
      overlayFactory = factory;
    }
  }

  /**
   * Creates the debug overlay using the registered factory and assigns it to {@link #overlay}.
   * Called internally by {@link FlixelGame} during startup when debug mode is enabled.
   *
   * @return The newly created overlay.
   */
  public FlixelDebugOverlay createOverlay() {
    overlay = overlayFactory.get();
    return overlay;
  }

  /**
   * Sets the sprite currently inspected by the texture inspector window.
   * Pass {@code null} to clear the selection.
   *
   * @param obj The sprite to inspect, or {@code null} to clear.
   */
  public void setInspectedSprite(@Nullable FlixelObject obj) {
    inspectedSprite = obj;
  }

  /**
   * Returns the sprite currently inspected, or {@code null} if no sprite is selected (or the previously
   * selected sprite has been destroyed). The returned sprite may be of any subclass of
   * {@link FlixelObject}.
   *
   * @return The inspected sprite, or {@code null}.
   */
  @Nullable
  public FlixelObject getInspectedSprite() {
    if (inspectedSprite != null && !inspectedSprite.exists) {
      inspectedSprite = null;
    }
    return inspectedSprite;
  }

  /** Sets the sprite that is currently being dragged via the LMB picker. Internal API. */
  void setDraggedSprite(@Nullable FlixelObject obj) {
    draggedSprite = obj;
  }

  /**
   * Returns the sprite that is currently being dragged via the LMB picker, or {@code null}.
   *
   * @return The currently dragged sprite, or {@code null} if none is being dragged.
   */
  @Nullable
  public FlixelObject getDraggedSprite() {
    if (draggedSprite != null && !draggedSprite.exists) {
      draggedSprite = null;
    }
    return draggedSprite;
  }

  /**
   * Registers a {@link FlixelBatch} whose {@link FlixelBatch#getRenderCalls()} will be included in
   * the debugger's total render-call count shown in the Stats panel. The framework's own batch is
   * already counted automatically; call this for any additional batches your game creates.
   *
   * <p>Registering a batch that is already tracked is a no-op. The batch must remain valid (not
   * disposed) for as long as it is registered. Call {@link #untrackBatch(FlixelBatch)} before
   * disposing a tracked batch.
   *
   * <p>Example:
   * <pre>{@code
   * FlixelBatch uiBatch = new FlixelSpriteBatch();
   * Flixel.debug.trackBatch(uiBatch);
   * }</pre>
   *
   * @param batch The batch to track. Must not be {@code null}.
   */
  public void trackBatch(@NotNull FlixelBatch batch) {
    if (batch == null || trackedBatches.contains(batch, true)) {
      return;
    }
    trackedBatches.add(batch);
  }

  /**
   * Removes a batch that was previously registered via {@link #trackBatch(FlixelBatch)}.
   * Passing a batch that was never registered is a no-op.
   *
   * @param batch The batch to stop tracking.
   */
  public void untrackBatch(@NotNull FlixelBatch batch) {
    if (batch != null) {
      trackedBatches.removeValue(batch, true);
    }
  }

  public FlixelArray<FlixelBatch> getTrackedBatches() {
    return trackedBatches;
  }

  /**
   * Registers a {@link FlixelDebugTrackerEntry} whose {@code label -> value} pairs are shown as a named,
   * collapsible group in the overlay's Tracker panel. Registering the same entry twice is a no-op.
   *
   * <p>Example:
   * <pre>{@code
   * Flixel.debug.addTrackerEntry(new EnemyTrackerEntry(enemyManager));
   * }</pre>
   *
   * @param entry The entry to register. Must not be {@code null}.
   */
  public void addTrackerEntry(@NotNull FlixelDebugTrackerEntry entry) {
    if (entry == null || trackerEntries.contains(entry, true)) {
      return;
    }
    trackerEntries.add(entry);
  }

  /**
   * Removes a previously registered tracker entry. Passing an entry that was never registered is a no-op.
   *
   * @param entry The entry to stop tracking.
   */
  public void removeTrackerEntry(@NotNull FlixelDebugTrackerEntry entry) {
    if (entry != null) {
      trackerEntries.removeValue(entry, true);
    }
  }

  /** Returns the live array of registered tracker entries. Package-private; consumed by the debug overlay. */
  FlixelArray<FlixelDebugTrackerEntry> getTrackerEntries() {
    return trackerEntries;
  }
}
