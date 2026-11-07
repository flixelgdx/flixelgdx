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
package org.flixelgdx.debug.panels;

import org.flixelgdx.debug.remote.FlixelDebugMessageReader;
import org.flixelgdx.debug.remote.FlixelDebugMessageWriter;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.jetbrains.annotations.NotNull;

/**
 * A ready-made {@link FlixelDebugPanel} that holds the boring parts: the id, the display name, the kind,
 * the enabled flag, and the revision counter.
 *
 * <p>Subclasses only have to write their payload in
 * {@link #write(FlixelDebugMessageWriter)} and, if they sample data, override
 * {@link #update(float)}. Call {@link #markChanged()} whenever the payload has new content.
 *
 * @see FlixelDebugPanel
 */
public abstract class FlixelDebugPanelBase implements FlixelDebugPanel {

  private long revision;

  private final int kind;
  private final String id;
  private final String displayName;

  private boolean enabled = true;

  /**
   * Creates a panel.
   *
   * @param id The stable id, for example {@code "custom.player"}.
   * @param displayName The name shown on the panel's tab.
   * @param kind One of the {@code KIND_*} constants of {@link FlixelDebugProtocol}.
   */
  protected FlixelDebugPanelBase(@NotNull String id, @NotNull String displayName, int kind) {
    this.id = id;
    this.displayName = displayName;
    this.kind = kind;
  }

  /** Marks the payload as changed so the network agent sends it again. */
  protected final void markChanged() {
    revision++;
  }

  @Override
  public void update(float elapsed) {}

  @Override
  public void handleAction(int action, @NotNull FlixelDebugMessageReader reader) {}

  @Override
  public void reset() {}

  @Override
  public long getRevision() {
    return revision;
  }

  @Override
  @NotNull
  public String getId() {
    return id;
  }

  @Override
  @NotNull
  public String getDisplayName() {
    return displayName;
  }

  @Override
  public int getKind() {
    return kind;
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }
}
