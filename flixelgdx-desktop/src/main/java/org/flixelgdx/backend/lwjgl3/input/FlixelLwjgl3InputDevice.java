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
package org.flixelgdx.backend.lwjgl3.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;

import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.input.FlixelInputDevice;
import org.flixelgdx.input.FlixelKeyboardListener;
import org.flixelgdx.input.FlixelMouseListener;
import org.flixelgdx.input.FlixelTouchListener;

/**
 * Desktop (LWJGL3) implementation of {@link FlixelInputDevice}, delegating to {@code Gdx.input}.
 *
 * <p>Polling calls forward straight to {@code Gdx.input}. On construction a single libGDX
 * {@link InputProcessor} adapter is installed, which fans keyboard, mouse, and touch events out to
 * every registered {@link FlixelKeyboardListener}, {@link FlixelMouseListener}, and
 * {@link FlixelTouchListener} in order.
 *
 * <p>On desktop, GLFW reports pointer 0 for mouse events, so any libGDX {@code touchDown} or
 * {@code touchUp} with {@code pointer == 0} is routed as a mouse click; pointers beyond 0 (from a
 * touchscreen attached to the machine) are routed as touch events.
 *
 * <p>Any libGDX {@link InputProcessor} already installed before this device is created is
 * preserved alongside the adapter in an {@link InputMultiplexer}, so tools such as debug overlays
 * that register their own processor keep working.
 */
public final class FlixelLwjgl3InputDevice implements FlixelInputDevice {

  private final FlixelArray<FlixelKeyboardListener> keyboardListeners =
      new FlixelArray<>(FlixelKeyboardListener[]::new);
  private final FlixelArray<FlixelMouseListener> mouseListeners =
      new FlixelArray<>(FlixelMouseListener[]::new);
  private final FlixelArray<FlixelTouchListener> touchListeners =
      new FlixelArray<>(FlixelTouchListener[]::new);

  /** Creates a device bound to the shared {@code Gdx.input} for this session. */
  public FlixelLwjgl3InputDevice() {
    InputProcessor adapter = new GdxEventDispatcher();
    InputProcessor current = Gdx.input.getInputProcessor();
    if (current == null) {
      Gdx.input.setInputProcessor(adapter);
    } else if (current instanceof InputMultiplexer multiplexer) {
      multiplexer.addProcessor(0, adapter);
    } else {
      InputMultiplexer mux = new InputMultiplexer();
      mux.addProcessor(adapter);
      mux.addProcessor(current);
      Gdx.input.setInputProcessor(mux);
    }
  }

  @Override
  public boolean isKeyPressed(int key) {
    return Gdx.input.isKeyPressed(key);
  }

  @Override
  public boolean isButtonPressed(int button) {
    return Gdx.input.isButtonPressed(button);
  }

  @Override
  public int getX() {
    return Gdx.input.getX();
  }

  @Override
  public int getY() {
    return Gdx.input.getY();
  }

  @Override
  public int getX(int pointer) {
    return Gdx.input.getX(pointer);
  }

  @Override
  public int getY(int pointer) {
    return Gdx.input.getY(pointer);
  }

  @Override
  public void addKeyboardListener(FlixelKeyboardListener listener) {
    if (listener != null && !keyboardListeners.contains(listener, true)) {
      keyboardListeners.add(listener);
    }
  }

  @Override
  public void removeKeyboardListener(FlixelKeyboardListener listener) {
    keyboardListeners.removeValue(listener, true);
  }

  @Override
  public void addMouseListener(FlixelMouseListener listener) {
    if (listener != null && !mouseListeners.contains(listener, true)) {
      mouseListeners.add(listener);
    }
  }

  @Override
  public void removeMouseListener(FlixelMouseListener listener) {
    mouseListeners.removeValue(listener, true);
  }

  @Override
  public void addTouchListener(FlixelTouchListener listener) {
    if (listener != null && !touchListeners.contains(listener, true)) {
      touchListeners.add(listener);
    }
  }

  @Override
  public void removeTouchListener(FlixelTouchListener listener) {
    touchListeners.removeValue(listener, true);
  }

  /** Fans libGDX input events out to registered FlixelGDX listeners. */
  private final class GdxEventDispatcher implements InputProcessor {

    @Override
    public boolean keyDown(int keycode) {
      FlixelKeyboardListener[] items = keyboardListeners.getItems();
      for (int i = 0, n = keyboardListeners.getSize(); i < n; i++) {
        if (items[i].keyDown(keycode)) {
          return true;
        }
      }
      return false;
    }

    @Override
    public boolean keyUp(int keycode) {
      FlixelKeyboardListener[] items = keyboardListeners.getItems();
      for (int i = 0, n = keyboardListeners.getSize(); i < n; i++) {
        if (items[i].keyUp(keycode)) {
          return true;
        }
      }
      return false;
    }

    @Override
    public boolean keyTyped(char character) {
      FlixelKeyboardListener[] items = keyboardListeners.getItems();
      for (int i = 0, n = keyboardListeners.getSize(); i < n; i++) {
        if (items[i].keyTyped(character)) {
          return true;
        }
      }
      return false;
    }

    @Override
    public boolean touchDown(int x, int y, int pointer, int button) {
      if (pointer == 0) {
        FlixelMouseListener[] items = mouseListeners.getItems();
        for (int i = 0, n = mouseListeners.getSize(); i < n; i++) {
          if (items[i].mouseDown(button, x, y)) {
            return true;
          }
        }
      } else {
        FlixelTouchListener[] items = touchListeners.getItems();
        for (int i = 0, n = touchListeners.getSize(); i < n; i++) {
          if (items[i].touched(pointer, x, y)) {
            return true;
          }
        }
      }
      return false;
    }

    @Override
    public boolean touchUp(int x, int y, int pointer, int button) {
      if (pointer == 0) {
        FlixelMouseListener[] items = mouseListeners.getItems();
        for (int i = 0, n = mouseListeners.getSize(); i < n; i++) {
          if (items[i].mouseUp(button, x, y)) {
            return true;
          }
        }
      } else {
        FlixelTouchListener[] items = touchListeners.getItems();
        for (int i = 0, n = touchListeners.getSize(); i < n; i++) {
          if (items[i].touchReleased(pointer, x, y)) {
            return true;
          }
        }
      }
      return false;
    }

    @Override
    public boolean touchCancelled(int x, int y, int pointer, int button) {
      if (pointer == 0) {
        return false;
      }
      FlixelTouchListener[] items = touchListeners.getItems();
      for (int i = 0, n = touchListeners.getSize(); i < n; i++) {
        if (items[i].touchCancelled(pointer, x, y)) {
          return true;
        }
      }
      return false;
    }

    @Override
    public boolean touchDragged(int x, int y, int pointer) {
      if (pointer == 0) {
        FlixelMouseListener[] items = mouseListeners.getItems();
        for (int i = 0, n = mouseListeners.getSize(); i < n; i++) {
          if (items[i].mouseDragged(x, y)) {
            return true;
          }
        }
      } else {
        FlixelTouchListener[] items = touchListeners.getItems();
        for (int i = 0, n = touchListeners.getSize(); i < n; i++) {
          if (items[i].touchDragged(pointer, x, y)) {
            return true;
          }
        }
      }
      return false;
    }

    @Override
    public boolean mouseMoved(int x, int y) {
      FlixelMouseListener[] items = mouseListeners.getItems();
      for (int i = 0, n = mouseListeners.getSize(); i < n; i++) {
        if (items[i].mouseMoved(x, y)) {
          return true;
        }
      }
      return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
      FlixelMouseListener[] items = mouseListeners.getItems();
      for (int i = 0, n = mouseListeners.getSize(); i < n; i++) {
        if (items[i].scrolled(amountX, amountY)) {
          return true;
        }
      }
      return false;
    }
  }
}
