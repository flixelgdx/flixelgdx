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
package org.flixelgdx.tilemap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

import org.flixelgdx.Flixel;
import org.flixelgdx.graphics.FlixelGraphic;

import java.lang.reflect.Proxy;

/**
 * Shared helpers for the tilemap tests.
 *
 * <p>The headless backend does not provide an OpenGL context, so {@link Texture} creation would
 * normally fail. The tilemap tests never actually render, so a no-op {@link GL20} proxy is enough:
 * texture uploads become harmless no-ops while CPU-side data such as width and height still works,
 * which is all the slicing logic needs.
 */
final class TilemapTestSupport {

  private TilemapTestSupport() {}

  /** Installs a no-op {@link GL20} so textures can be constructed without a real GL context. */
  static void installMockGl() {
    if (Gdx.gl20 != null) {
      return;
    }
    GL20 gl = (GL20) Proxy.newProxyInstance(
        TilemapTestSupport.class.getClassLoader(),
        new Class<?>[] { GL20.class },
        (proxy, method, args) -> defaultReturnValue(method.getReturnType()));
    Gdx.gl = gl;
    Gdx.gl20 = gl;
  }

  /**
   * Builds a tileset backed by a solid-color texture of the given size.
   *
   * @param texWidth The backing texture width in pixels.
   * @param texHeight The backing texture height in pixels.
   * @param tileWidth The tile width in pixels.
   * @param tileHeight The tile height in pixels.
   * @param margin The sheet border margin in pixels.
   * @param spacing The inter-tile spacing in pixels.
   * @return A ready-to-use tileset.
   */
  static FlixelTileset newTileset(
      int texWidth, int texHeight, int tileWidth, int tileHeight, int margin, int spacing) {
    installMockGl();
    Pixmap pixmap = new Pixmap(texWidth, texHeight, Pixmap.Format.RGBA8888);
    pixmap.setColor(1f, 1f, 1f, 1f);
    pixmap.fill();
    Texture texture = new Texture(pixmap);
    pixmap.dispose();
    FlixelGraphic graphic = new FlixelGraphic(Flixel.ensureAssets(), "test-tileset-" + texture.hashCode(), texture);
    return new FlixelTileset(graphic, tileWidth, tileHeight, margin, spacing);
  }

  private static Object defaultReturnValue(Class<?> returnType) {
    if (returnType == boolean.class) {
      return Boolean.FALSE;
    }
    if (returnType == char.class) {
      return (char) 0;
    }
    if (returnType == byte.class) {
      return (byte) 0;
    }
    if (returnType == short.class) {
      return (short) 0;
    }
    if (returnType == int.class) {
      return 0;
    }
    if (returnType == long.class) {
      return 0L;
    }
    if (returnType == float.class) {
      return 0f;
    }
    if (returnType == double.class) {
      return 0d;
    }
    return null;
  }
}
