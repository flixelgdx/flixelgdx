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
package org.flixelgdx.debug.feeds;

import org.flixelgdx.debug.FlixelDebugManager;
import org.flixelgdx.debug.remote.FlixelDebugMessageReader;
import org.flixelgdx.debug.remote.FlixelDebugMessageWriter;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.debug.remote.FlixelDebugProtocolException;
import org.flixelgdx.debug.ui.FlixelDebugPanel;
import org.jetbrains.annotations.NotNull;

/**
 * A stream of typed game data for programs, such as the remote debugger app's tools for AI agents.
 *
 * <p>The debugger has two separate channels. {@linkplain FlixelDebugPanel Panels} are the user interface: widgets a
 * person looks at and clicks. Feeds are the data underneath: the scene tree, the save values, the camera's boxes, and
 * so on, each in a fixed binary format that {@code DEBUG_PROTOCOL.md} describes. A program (for example the app's
 * {@code get_scene} tool) reads a feed instead of a panel, so a game can redesign or replace any panel without
 * breaking the tools.
 *
 * <p>Think of a feed as a newswire. Each frame the feed quietly gathers facts ({@link #update(float)}). When a
 * subscriber asks for the story, the feed writes it in the agreed format ({@link #write(FlixelDebugMessageWriter)}).
 * When a subscriber sends a request, {@link #handleAction(int, FlixelDebugMessageReader)} answers it. The feed never
 * talks to the network itself, so the same feed works on every platform.
 *
 * <p>Feeds also hold the logic: the built-in panels read and change the game through the same feed objects, which
 * game code reaches as fields such as {@code Flixel.debug.watches} or {@code Flixel.debug.save}. A feed only sends
 * data while the app subscribes to it, but it keeps updating either way, because the panels rely on it.
 *
 * <p>All methods are called from the game's main thread. {@link #update(float)} runs every frame, so it must not
 * allocate objects.
 *
 * @see FlixelDebugFeedBase
 * @see FlixelDebugManager
 */
public interface FlixelDebugFeed {

  /**
   * Returns the stable identifier of this feed, for example {@code "stats"} or {@code "scene"}.
   *
   * @return The id, never {@code null}.
   */
  @NotNull
  String getId();

  /**
   * Returns the kind of payload this feed writes.
   *
   * @return One of the {@code KIND_*} constants of {@link FlixelDebugProtocol}.
   */
  int getKind();

  /**
   * Returns a number that goes up whenever this feed has new content worth sending.
   *
   * <p>The network agent remembers the last revision it sent and sends the feed again when the number differs. A
   * feed that changes every frame can simply bump it every time it samples.
   *
   * @return The current revision. It never goes down.
   */
  long getRevision();

  /**
   * Samples the game once. Called every frame while the game runs in debug mode and the feed is enabled.
   *
   * <p>This must not allocate objects.
   *
   * @param elapsed Real seconds since the last frame, not scaled by {@code Flixel.timeScale}.
   */
  void update(float elapsed);

  /**
   * Returns whether the manager updates this feed.
   *
   * @return {@code true} when the feed is enabled.
   */
  boolean isEnabled();

  /**
   * Turns the feed on or off. A disabled feed is skipped by {@link FlixelDebugManager#update(float)}, and the panels
   * that rely on it stop changing.
   *
   * @param enabled {@code true} to enable the feed.
   */
  void setEnabled(boolean enabled);

  /**
   * Writes this feed's payload into the writer.
   *
   * <p>The caller has already started a {@code FEED_DATA} message and written the feed index, so write only the
   * fields the feed's kind defines in {@code DEBUG_PROTOCOL.md}. Do not call
   * {@link FlixelDebugMessageWriter#beginMessage(int)} or {@link FlixelDebugMessageWriter#finish()}.
   *
   * @param writer The writer positioned right after the feed index.
   */
  void write(@NotNull FlixelDebugMessageWriter writer);

  /**
   * Writes the payload sent right after the app subscribes. The default writes the regular payload; the log feed
   * writes its whole backlog instead.
   *
   * @param writer The writer positioned right after the feed index.
   */
  default void writeFirst(@NotNull FlixelDebugMessageWriter writer) {
    write(writer);
  }

  /**
   * Returns whether the feed has an extra payload waiting that is not part of its regular state, such as a finished
   * thumbnail or the result of a console command.
   *
   * <p>The network agent asks every subscribed feed each frame. While this is {@code true} it sends one
   * {@code FEED_DATA} message whose payload is written by {@link #writePendingPayload(FlixelDebugMessageWriter)},
   * then asks again. The default has nothing waiting.
   *
   * @return {@code true} when {@link #writePendingPayload(FlixelDebugMessageWriter)} has something to write.
   */
  default boolean hasPendingPayload() {
    return false;
  }

  /**
   * Writes the next extra payload and removes it from the feed's queue.
   *
   * <p>Like {@link #write(FlixelDebugMessageWriter)}, the caller has already written the message header and the
   * feed index. The payload starts with the subtype byte that the feed's kind defines in {@code DEBUG_PROTOCOL.md}.
   * Does nothing when {@link #hasPendingPayload()} is {@code false}.
   *
   * @param writer The writer positioned right after the feed index.
   */
  default void writePendingPayload(@NotNull FlixelDebugMessageWriter writer) {}

  /**
   * Handles an action sent by a tool in the debugger app.
   *
   * <p>The feed index and the action code have already been read, so the reader is positioned at the action's
   * fields. Unknown action codes must be ignored without reading anything. A truncated payload makes the reader throw
   * a {@link FlixelDebugProtocolException}, which the caller treats as "drop this message".
   *
   * @param action The action code, scoped to this feed's kind.
   * @param reader The reader positioned after the action code.
   */
  void handleAction(int action, @NotNull FlixelDebugMessageReader reader);

  /**
   * Returns the feed to its initial runtime state, for example by clearing recorded history.
   *
   * <p>Registrations made by game code (watches, commands, and so on) are kept.
   */
  void reset();
}
