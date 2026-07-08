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
package org.flixelgdx.backend.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowListener;

import org.flixelgdx.FlixelGame;
import org.flixelgdx.backend.lwjgl3.window.FlixelLwjgl3ChainingWindowListener;
import org.flixelgdx.backend.lwjgl3.window.FlixelLwjgl3NotifyWindowListener;
import org.flixelgdx.backend.lwjgl3.window.FlixelLwjgl3Window;
import org.flixelgdx.backend.runtime.FlixelRuntimeMode;
import org.jetbrains.annotations.Nullable;

/**
 * libGDX LWJGL3 application configuration that records the last user-set {@link Lwjgl3WindowListener} so
 * {@link FlixelLwjgl3Launcher} can wrap it for Flixel window hooks and optional close absorption without reflection.
 *
 * <p>Use this type whenever you pass a custom configuration into {@link FlixelLwjgl3Launcher#launch(
 * FlixelGame, FlixelRuntimeMode, FlixelLwjgl3ApplicationConfiguration)} and you call
 * {@link #setWindowListener(Lwjgl3WindowListener)} yourself. The convenience overloads that build a configuration
 * for you already create an instance of this class.
 *
 * <p>Example:
 *
 * <pre>{@code
 * FlixelLwjgl3ApplicationConfiguration config = new FlixelLwjgl3ApplicationConfiguration();
 * config.setTitle("My Game");
 * config.setWindowListener(myListener);
 * FlixelLwjgl3Launcher.launch(game, FlixelRuntimeMode.RELEASE, config);
 * }</pre>
 */
public class FlixelLwjgl3ApplicationConfiguration extends Lwjgl3ApplicationConfiguration {

  @Nullable
  private Lwjgl3WindowListener userWindowListener;

  @Nullable
  private FlixelLwjgl3ChainingWindowListener installedFlixelChain;

  @Override
  public void setWindowListener(@Nullable Lwjgl3WindowListener listener) {
    if (!(listener instanceof FlixelLwjgl3ChainingWindowListener)) {
      userWindowListener = listener;
    }
    super.setWindowListener(listener);
  }

  /**
   * @return The last listener you assigned with {@link #setWindowListener(Lwjgl3WindowListener)} before the Flixel chain
   * was installed, or {@code null} if none. This excludes {@link FlixelLwjgl3ChainingWindowListener}; that wrapper is
   * managed by {@link FlixelLwjgl3Launcher}.
   */
  @Nullable
  public Lwjgl3WindowListener getUserWindowListener() {
    return userWindowListener;
  }

  /**
   * Wraps {@link #getUserWindowListener()} inside Flixel GLFW hooks and installs the chaining listener used for close
   * absorption support. Safe to call more than once; repeated calls refresh the absorption hook reference.
   */
  public void attachFlixelWindowListenerChain() {
    if (installedFlixelChain != null) {
      FlixelLwjgl3Window.configureCloseHandlingHook(installedFlixelChain);
      return;
    }
    FlixelLwjgl3NotifyWindowListener notify = new FlixelLwjgl3NotifyWindowListener(userWindowListener);
    FlixelLwjgl3ChainingWindowListener chain = new FlixelLwjgl3ChainingWindowListener(notify);
    super.setWindowListener(chain);
    installedFlixelChain = chain;
    FlixelLwjgl3Window.configureCloseHandlingHook(chain);
  }
}
