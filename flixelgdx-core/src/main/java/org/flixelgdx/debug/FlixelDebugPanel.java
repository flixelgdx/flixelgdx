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

import org.flixelgdx.debug.remote.FlixelDebugMessageReader;
import org.flixelgdx.debug.remote.FlixelDebugMessageWriter;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.debug.remote.FlixelDebugProtocolException;
import org.jetbrains.annotations.NotNull;

/**
 * One tab of the remote debugger: it samples some data from the running game and knows how to encode
 * that data for the debugger app.
 *
 * <p>Think of a panel as a reporter in a newsroom. Each frame the reporter quietly gathers facts
 * ({@link #update(float)}). When the editor (the network agent) asks for the story, the reporter writes
 * it out in the agreed format ({@link #write(FlixelDebugMessageWriter)}). When a reader (the debugger
 * app) pokes the reporter with a question, {@link #handleAction(int, FlixelDebugMessageReader)} answers it.
 * The reporter never talks to the network itself, so the same panel works on every platform.
 *
 * <p>Panels are registered on {@link FlixelDebugManager} and are looked up by their stable
 * {@linkplain #getId() id}. The built-in panels (stats, logs, watches, trackers, controls, and console)
 * are already registered and exposed as fields such as {@code Flixel.debug.stats}. A game can add its own
 * panels with {@link FlixelDebugManager#register(FlixelDebugPanel)}.
 *
 * <p>Example of a custom panel that shows the player's score:
 *
 * <pre>{@code
 * public class ScorePanel extends FlixelDebugPanelBase {
 *
 *   public ScorePanel() {
 *     super("custom.score", "Score", FlixelDebugProtocol.KIND_CUSTOM);
 *   }
 *
 *   @Override
 *   public void write(@NotNull FlixelDebugMessageWriter w) {
 *     w.beginArray();                          // The rows: name, value, editable.
 *     w.writeString("Score").writeValue(score).writeBool(false);
 *     w.endArray(1);
 *     w.beginArray().endArray(0);              // No buttons.
 *   }
 * }
 *
 * Flixel.debug.register(new ScorePanel());
 * }</pre>
 *
 * <p>All methods are called from the game's main thread. {@link #update(float)} runs every frame, so it must
 * not allocate objects.
 *
 * @see FlixelDebugPanelBase
 * @see FlixelDebugManager
 */
public interface FlixelDebugPanel {

  /**
   * Returns the stable identifier of this panel, for example {@code "stats"} or {@code "custom.player"}.
   *
   * <p>The debugger app matches panels by this id, so it must not change while the panel is registered.
   *
   * @return The id, never {@code null}.
   */
  @NotNull
  String getId();

  /**
   * Returns the name shown on the panel's tab in the debugger app.
   *
   * @return The display name, never {@code null}.
   */
  @NotNull
  String getDisplayName();

  /**
   * Returns the kind of payload this panel writes.
   *
   * @return One of the {@code KIND_*} constants of {@link FlixelDebugProtocol}. User panels return
   *   {@link FlixelDebugProtocol#KIND_CUSTOM}.
   */
  int getKind();

  /**
   * Returns a number that goes up whenever this panel has new content worth sending.
   *
   * <p>The network agent remembers the last revision it sent and sends the panel again when the number
   * differs. A panel that changes every frame can simply bump it every time it samples.
   *
   * @return The current revision. It never goes down while the panel is registered.
   */
  long getRevision();

  /**
   * Samples the game once. Called every frame while the game runs in debug mode and the panel is enabled.
   *
   * <p>This must not allocate objects.
   *
   * @param elapsed Real seconds since the last frame, not scaled by {@code Flixel.timeScale}.
   */
  void update(float elapsed);

  /**
   * Returns whether the manager updates this panel.
   *
   * @return {@code true} when the panel is enabled.
   */
  boolean isEnabled();

  /**
   * Turns the panel on or off. A disabled panel is skipped by {@link FlixelDebugManager#update(float)}.
   *
   * @param enabled {@code true} to enable the panel.
   */
  void setEnabled(boolean enabled);

  /**
   * Writes this panel's payload into the writer.
   *
   * <p>The caller has already started a {@code PANEL_DATA} message and written the panel index, so write
   * only the fields the panel's kind defines in {@code DEBUG_PROTOCOL.md}. Do not call
   * {@link FlixelDebugMessageWriter#beginMessage(int)} or {@link FlixelDebugMessageWriter#finish()}.
   *
   * @param writer The writer positioned right after the panel index.
   */
  void write(@NotNull FlixelDebugMessageWriter writer);

  /**
   * Handles an action sent by the debugger app.
   *
   * <p>The panel index and the action code have already been read, so the reader is positioned at the
   * action's fields. Unknown action codes must be ignored without reading anything. A truncated payload
   * makes the reader throw a {@link FlixelDebugProtocolException}, which the
   * caller treats as "drop this message".
   *
   * @param action The action code, scoped to this panel's kind.
   * @param reader The reader positioned after the action code.
   */
  void handleAction(int action, @NotNull FlixelDebugMessageReader reader);

  /**
   * Returns the panel to its initial runtime state, for example by clearing recorded history.
   *
   * <p>Registrations made by game code (watches, commands, and so on) are kept.
   */
  void reset();
}
