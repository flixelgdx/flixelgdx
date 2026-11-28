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
package org.flixelgdx.testing;

import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.collections.FlixelList;
import org.flixelgdx.graphics.FlixelDisplayMode;
import org.flixelgdx.graphics.FlixelGraphicsManager;
import org.jetbrains.annotations.NotNull;

public final class RecordingGraphics implements FlixelGraphicsManager {

  public final StringBuilder log = new StringBuilder();
  public int targetFps = 60;
  public boolean globalShaders;

  @NotNull
  @Override
  public FlixelList<FlixelDisplayMode> getDisplayModes() {
    return new FlixelArray<>();
  }

  @Override
  public void clear(float r, float g, float b, float a) {
    log.append("clear ");
  }

  @Override
  public void beginScene() {
    log.append("beginScene ");
  }

  @Override
  public void endScene() {
    log.append("endScene ");
  }

  @Override
  public boolean hasGlobalShaders() {
    return globalShaders;
  }

  @Override
  public void beginGlobalShaderCapture() {
    log.append("beginShaders ");
  }

  @Override
  public void endGlobalShaderCapture() {
    log.append("endShaders ");
  }

  @Override
  public void beginCameraPass() {
    log.append("cameraPass ");
  }

  @Override
  public boolean fillViewOpaque(float r, float g, float b, float a) {
    return true;
  }

  @Override
  public void forceOpaqueAlpha() {
    log.append("opaque ");
  }

  @Override
  public void resizeGlobalShaders() {
    log.append("resizeShaders ");
  }

  @Override
  public void disposeGlobalShaders() {
    log.append("disposeShaders ");
  }

  @Override
  public int getTargetFps() {
    return targetFps;
  }

  @Override
  public void setTargetFps(int fps) {
    targetFps = fps;
  }
}
