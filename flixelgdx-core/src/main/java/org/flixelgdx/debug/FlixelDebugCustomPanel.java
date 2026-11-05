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
import org.flixelgdx.debug.inspector.FlixelDebugValue;
import org.flixelgdx.debug.inspector.FlixelPanelBuilder;
import org.flixelgdx.debug.remote.FlixelDebugMessageReader;
import org.flixelgdx.debug.remote.FlixelDebugMessageWriter;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * A debugger panel that you describe with a few lines of code: some live rows (values you can watch and edit) and
 * some buttons that run your code.
 *
 * <p>Think of building a small remote control for your game. Each row is a gauge or a dial, and each button
 * triggers something. You say what each one reads or does, and the debugger app draws it.
 *
 * <pre>{@code
 * Flixel.debug.register(new FlixelDebugCustomPanel("ai", "Enemy AI", p -> p
 *     .intProp("Active enemies", () -> enemies.countLiving())
 *     .floatProp("Aggression", () -> ai.aggression, v -> ai.aggression = v)
 *     .boolProp("Paused", () -> ai.paused, v -> ai.paused = v)
 *     .action("Kill all", enemies::killAll)));
 * }</pre>
 *
 * <p>A row with a setter can be edited in the app. A row without one is read-only. Values are read about ten times
 * a second and reading never allocates. If a getter throws, the row shows the exception message and the rest of
 * the panel keeps working. A button runs its code on the game's main thread, and an exception in it is logged and
 * does not crash the game.
 *
 * <p>The panel's id should be unique. Using a prefix such as {@code "custom."} keeps it away from the built-in
 * panels.
 *
 * @see FlixelPanelBuilder
 * @see FlixelDebugManager#register(FlixelDebugPanel)
 * @see FlixelDebugProtocol#KIND_CUSTOM
 */
public class FlixelDebugCustomPanel extends FlixelDebugPanelBase {

  /** Seconds between revision bumps, which is how often the app is offered fresh values. */
  private static final float REFRESH_INTERVAL = 0.1f;

  private static final String TAG = "FlixelDebug";

  private final FlixelPanelBuilder builder = new FlixelPanelBuilder();
  private final FlixelDebugValue value = new FlixelDebugValue();

  private float timer;

  /**
   * Creates a custom panel with no rows or buttons. Add them through {@link #getBuilder()}.
   *
   * @param id The stable id of the panel, for example {@code "custom.ai"}.
   * @param displayName The name on the panel's tab.
   */
  public FlixelDebugCustomPanel(@NotNull String id, @NotNull String displayName) {
    super(id, displayName, FlixelDebugProtocol.KIND_CUSTOM);
    markChanged();
  }

  /**
   * Creates a custom panel and fills it.
   *
   * @param id The stable id of the panel, for example {@code "custom.ai"}.
   * @param displayName The name on the panel's tab.
   * @param build Adds the rows and buttons to the builder.
   */
  public FlixelDebugCustomPanel(@NotNull String id, @NotNull String displayName,
      @NotNull Consumer<FlixelPanelBuilder> build) {
    this(id, displayName);
    build.accept(builder);
  }

  /** Makes rows marked {@code once()} read their getters again, and sends the panel again. */
  public void refresh() {
    builder.refresh();
    markChanged();
  }

  @Override
  public void update(float elapsed) {
    timer += elapsed;
    if (timer >= REFRESH_INTERVAL) {
      timer = 0f;
      if (builder.getRowCount() > 0) {
        markChanged();
      }
    }
  }

  @Override
  public void write(@NotNull FlixelDebugMessageWriter writer) {
    builder.write(writer);
  }

  @Override
  public void handleAction(int action, @NotNull FlixelDebugMessageReader reader) {
    switch (action) {
      case FlixelDebugProtocol.CUSTOM_PRESS -> press(reader.readU16());
      case FlixelDebugProtocol.CUSTOM_SET_PROPERTY -> {
        int index = reader.readU16();
        value.read(reader);
        if (builder.setProperty(index, value)) {
          markChanged();
        }
      }
      default -> {
      }
    }
  }

  private void press(int index) {
    try {
      builder.press(index);
    } catch (Exception e) {
      Flixel.log.tagged(TAG).error("The button {} of the panel '{}' failed: {}", index, getId(), e.getMessage());
    }
    markChanged();
  }

  /**
   * Returns the builder that holds this panel's rows and buttons, so more can be added after construction.
   *
   * @return The builder.
   */
  @NotNull
  public FlixelPanelBuilder getBuilder() {
    return builder;
  }
}
