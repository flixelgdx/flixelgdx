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
package org.flixelgdx.graphics;

import org.flixelgdx.math.FlixelMatrix;
import org.flixelgdx.math.FlixelVector;
import org.flixelgdx.util.FlixelColor;
import org.jetbrains.annotations.NotNull;

/**
 * No-op {@link FlixelShader} returned by {@link FlixelGraphicsManager#compileShader(FlixelShaderSource)}
 * on headless and pre-startup sessions.
 *
 * <p>Every operation does nothing. {@link #isValid()} always returns {@code false} so callers can
 * detect that no real GPU shader is present without needing to null-check the result of
 * {@link FlixelGraphicsManager#compileShader(FlixelShaderSource)}.
 *
 * @see FlixelGraphicsManager#compileShader(FlixelShaderSource)
 */
public enum FlixelUnsupportedShader implements FlixelShader {

  /** Shared no-op instance. */
  INSTANCE;

  @Override
  public void destroy() {}

  @Override
  public void setUniform(@NotNull String name, float value) {}

  @Override
  public void setUniform(@NotNull String name, int value) {}

  @Override
  public void setUniform(@NotNull String name, @NotNull FlixelVector value) {}

  @Override
  public void setUniform(@NotNull String name, @NotNull FlixelColor value) {}

  @Override
  public void setUniform(@NotNull String name, @NotNull FlixelMatrix value) {}

  @Override
  public boolean isValid() {
    return false;
  }
}
