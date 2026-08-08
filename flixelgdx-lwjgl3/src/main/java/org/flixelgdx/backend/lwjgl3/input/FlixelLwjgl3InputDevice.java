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

import org.flixelgdx.input.FlixelInputDevice;
import org.flixelgdx.input.FlixelInputProcessor;
import org.jetbrains.annotations.Nullable;

/**
 * Desktop (LWJGL3) implementation of {@link FlixelInputDevice}, delegating to {@code Gdx.input}.
 *
 * <p>This is the transitional backend the framework installs while the desktop platform still runs
 * on libGDX. Polling calls forward straight to {@code Gdx.input}; the live-event side wraps the
 * framework's {@link FlixelInputProcessor} in a libGDX {@link InputProcessor} adapter and registers
 * it, so the rest of FlixelGDX never has to name libGDX to read the keyboard or pointer.
 *
 * <p>When a processor is installed, any processor libGDX already had is preserved (the two run side
 * by side through an {@link InputMultiplexer}) so tools that register their own libGDX processor,
 * such as a debug overlay, keep working.
 */
public final class FlixelLwjgl3InputDevice implements FlixelInputDevice {

  @Nullable
  private FlixelInputProcessor processor;

  @Nullable
  private InputProcessor installedAdapter;

  /** Creates a device bound to the shared {@code Gdx.input} for this session. */
  public FlixelLwjgl3InputDevice() {}

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
  public void setInputProcessor(@Nullable FlixelInputProcessor processor) {
    this.processor = processor;
    InputProcessor adapter = processor == null ? null : new GdxProcessorAdapter(processor);
    InputProcessor current = Gdx.input.getInputProcessor();
    if (current instanceof InputMultiplexer multiplexer) {
      if (installedAdapter != null) {
        multiplexer.removeProcessor(installedAdapter);
      }
      if (adapter != null) {
        multiplexer.addProcessor(0, adapter);
      }
    } else if (current == null || current == installedAdapter) {
      Gdx.input.setInputProcessor(adapter);
    } else {
      InputMultiplexer multiplexer = new InputMultiplexer();
      if (adapter != null) {
        multiplexer.addProcessor(adapter);
      }
      multiplexer.addProcessor(current);
      Gdx.input.setInputProcessor(multiplexer);
    }
    installedAdapter = adapter;
  }

  @Override
  @Nullable
  public FlixelInputProcessor getInputProcessor() {
    return processor;
  }

  /** Bridges libGDX input events onto a {@link FlixelInputProcessor}. */
  private static final class GdxProcessorAdapter implements InputProcessor {

    private final FlixelInputProcessor delegate;

    GdxProcessorAdapter(FlixelInputProcessor delegate) {
      this.delegate = delegate;
    }

    @Override
    public boolean keyDown(int keycode) {
      return delegate.keyDown(keycode);
    }

    @Override
    public boolean keyUp(int keycode) {
      return delegate.keyUp(keycode);
    }

    @Override
    public boolean keyTyped(char character) {
      return delegate.keyTyped(character);
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
      return delegate.touchDown(screenX, screenY, pointer, button);
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
      return delegate.touchUp(screenX, screenY, pointer, button);
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
      return delegate.touchCancelled(screenX, screenY, pointer, button);
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
      return delegate.touchDragged(screenX, screenY, pointer);
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
      return delegate.mouseMoved(screenX, screenY);
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
      return delegate.scrolled(amountX, amountY);
    }
  }
}
