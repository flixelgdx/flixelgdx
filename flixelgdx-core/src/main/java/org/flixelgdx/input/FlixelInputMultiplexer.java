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
package org.flixelgdx.input;

import org.flixelgdx.collections.FlixelArray;
import org.jetbrains.annotations.NotNull;

/**
 * Fans a single stream of input events out to several {@link FlixelInputProcessor}s in order.
 *
 * <p>Only one processor can be registered with {@link FlixelInputDevice} at a time, but a game
 * usually has several things that each want to see input (the keyboard tracker, the mouse tracker,
 * a pause menu, and so on). This multiplexer is the fix: register <i>it</i> with the device, then
 * add each real processor to it. When an event arrives, the multiplexer offers it to each processor
 * in the order they were added and stops at the first one that consumes it (returns {@code true}).
 * A processor that only observes input returns {@code false}, so the event keeps flowing to the
 * ones behind it.
 *
 * <p>Because earlier processors get first refusal, order is priority: add the things that should be
 * able to swallow input (a modal dialog, for instance) before the things that merely watch it.
 *
 * <p>Example:
 *
 * <pre>{@code
 * FlixelInputMultiplexer mux = new FlixelInputMultiplexer();
 * mux.addProcessor(pauseMenu);   // gets events first; can consume them.
 * mux.addProcessor(keyTracker);  // only sees what the menu did not consume.
 * Flixel.input.setInputProcessor(mux);
 * }</pre>
 *
 * <p>This class allocates nothing while dispatching, so it is safe to leave installed every frame.
 *
 * @see FlixelInputProcessor
 * @see FlixelInputDevice#setInputProcessor(FlixelInputProcessor)
 */
public class FlixelInputMultiplexer implements FlixelInputProcessor {

  private final FlixelArray<FlixelInputProcessor> processors =
      new FlixelArray<>(FlixelInputProcessor[]::new);

  /** Creates an empty multiplexer. Add processors with {@link #addProcessor(FlixelInputProcessor)}. */
  public FlixelInputMultiplexer() {}

  /**
   * Adds a processor to the end of the chain, so it sees events after every processor already added.
   *
   * @param processor The processor to add; ignored when {@code null}.
   */
  public void addProcessor(@NotNull FlixelInputProcessor processor) {
    if (processor == null) {
      return;
    }
    processors.add(processor);
  }

  /**
   * Inserts a processor at a specific position in the chain, shifting later processors back. Use
   * index {@code 0} to give the new processor first refusal on every event.
   *
   * @param index The position to insert at, from {@code 0} to the current size.
   * @param processor The processor to add; ignored when {@code null}.
   */
  public void addProcessor(int index, @NotNull FlixelInputProcessor processor) {
    if (processor == null) {
      return;
    }
    processors.insert(index, processor);
  }

  /**
   * Removes a previously added processor from the chain.
   *
   * @param processor The processor to remove.
   * @return {@code true} if it was present and removed.
   */
  public boolean removeProcessor(@NotNull FlixelInputProcessor processor) {
    return processors.removeValue(processor, true);
  }

  /** Removes every processor from the chain, so this multiplexer stops dispatching to anything. */
  public void clear() {
    processors.clear();
  }

  @Override
  public boolean keyDown(int keycode) {
    FlixelInputProcessor[] items = processors.getItems();
    for (int i = 0, n = processors.getSize(); i < n; i++) {
      if (items[i].keyDown(keycode)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean keyUp(int keycode) {
    FlixelInputProcessor[] items = processors.getItems();
    for (int i = 0, n = processors.getSize(); i < n; i++) {
      if (items[i].keyUp(keycode)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean keyTyped(char character) {
    FlixelInputProcessor[] items = processors.getItems();
    for (int i = 0, n = processors.getSize(); i < n; i++) {
      if (items[i].keyTyped(character)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean touchDown(int screenX, int screenY, int pointer, int button) {
    FlixelInputProcessor[] items = processors.getItems();
    for (int i = 0, n = processors.getSize(); i < n; i++) {
      if (items[i].touchDown(screenX, screenY, pointer, button)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean touchUp(int screenX, int screenY, int pointer, int button) {
    FlixelInputProcessor[] items = processors.getItems();
    for (int i = 0, n = processors.getSize(); i < n; i++) {
      if (items[i].touchUp(screenX, screenY, pointer, button)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
    FlixelInputProcessor[] items = processors.getItems();
    for (int i = 0, n = processors.getSize(); i < n; i++) {
      if (items[i].touchCancelled(screenX, screenY, pointer, button)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean touchDragged(int screenX, int screenY, int pointer) {
    FlixelInputProcessor[] items = processors.getItems();
    for (int i = 0, n = processors.getSize(); i < n; i++) {
      if (items[i].touchDragged(screenX, screenY, pointer)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean mouseMoved(int screenX, int screenY) {
    FlixelInputProcessor[] items = processors.getItems();
    for (int i = 0, n = processors.getSize(); i < n; i++) {
      if (items[i].mouseMoved(screenX, screenY)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean scrolled(float amountX, float amountY) {
    FlixelInputProcessor[] items = processors.getItems();
    for (int i = 0, n = processors.getSize(); i < n; i++) {
      if (items[i].scrolled(amountX, amountY)) {
        return true;
      }
    }
    return false;
  }

  /**
   * @return How many processors are currently in the chain.
   */
  public int getSize() {
    return processors.getSize();
  }
}
