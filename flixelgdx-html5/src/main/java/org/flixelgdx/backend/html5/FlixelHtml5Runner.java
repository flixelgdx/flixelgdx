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
package org.flixelgdx.backend.html5;

import org.flixelgdx.FlixelGame;
import org.flixelgdx.backend.FlixelGameRunner;
import org.flixelgdx.backend.html5.graphics.FlixelHtml5Graphics;
import org.jetbrains.annotations.NotNull;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLDocument;

public class FlixelHtml5Runner implements FlixelGameRunner {

  @NotNull
  private final FlixelHtml5Graphics graphics;

  @NotNull
  private final FlixelHtml5Canvas canvas;

  private final String title;
  private final int width;
  private final int height;

  public FlixelHtml5Runner(String title, int width, int height, @NotNull FlixelHtml5Graphics graphics,
      @NotNull FlixelHtml5Canvas canvas) {
    this.title = title;
    this.width = width;
    this.height = height;
    this.graphics = graphics;
    this.canvas = canvas;
  }

  @Override
  public void run(@NotNull FlixelGame game) {
    HTMLDocument document = HTMLDocument.current();
    HTMLCanvasElement teavmCanvas = (HTMLCanvasElement) document.createElement("canvas");
    document.appendChild(teavmCanvas);
    canvas.setCanvas(teavmCanvas);

    document.setTitle(title);
    teavmCanvas.setWidth(width);
    teavmCanvas.setHeight(height);

    graphics.initialize(teavmCanvas);
  }
}
