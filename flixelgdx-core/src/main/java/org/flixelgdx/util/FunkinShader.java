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
package org.flixelgdx.util;

import com.badlogic.gdx.Gdx;

/**
 * A compiled shader with the full Friday Night Funkin' (FNF) shader environment built in.
 *
 * <p>FNF shaders extend HaxeFlixel's standard {@code #pragma header} system with additional
 * uniforms and GLSL helper functions for converting between screen space, world space, and
 * sprite-local space. This class injects all of those definitions automatically so FNF
 * {@code .frag} files compile and run without manual changes.
 *
 * <p>The injected GLSL environment provides:
 * <ul>
 *   <li>{@code uniform vec2 uScreenResolution} - screen size in pixels; controlled by
 *       {@link #screenWidth} and {@link #screenHeight}</li>
 *   <li>{@code uniform vec2 uCameraPos} - camera world position; controlled by
 *       {@link #cameraX} and {@link #cameraY}</li>
 *   <li>{@code uniform vec2 uCameraScale} - camera zoom; controlled by
 *       {@link #cameraScaleX} and {@link #cameraScaleY}</li>
 *   <li>{@code uniform vec2 uSpritePosition} - sprite screen-pixel origin for sprite-mode
 *       shaders; controlled by {@link #spriteX} and {@link #spriteY}</li>
 *   <li>{@code uniform vec2 uSpriteSize} - sprite pixel dimensions for sprite-mode shaders;
 *       controlled by {@link #spriteWidth} and {@link #spriteHeight}</li>
 *   <li>{@code screenCoord} - alias for the fragment's UV ({@code openfl_TextureCoordv})</li>
 *   <li>{@code screenToWorld(vec2)} - converts a screen UV to game-world coordinates</li>
 *   <li>{@code worldToScreen(vec2)} - converts game-world coordinates to a screen UV</li>
 *   <li>{@code screenToFrame(vec2)} - converts a sprite-local UV to the screen UV of that
 *       pixel, for use in sprite-mode shaders</li>
 *   <li>{@code sampleBitmapWorld(vec2)} - samples the bitmap texture at a world position</li>
 * </ul>
 *
 * <p>Usage - loading a FNF shader and applying it to the default camera:
 *
 * <pre>{@code
 * String src = Gdx.files.internal("shaders/rain.frag").readString();
 * FunkinShader rain = new FunkinShader(src);
 * rain.cameraX = camera.scroll.x;
 * rain.cameraY = camera.scroll.y;
 * rain.cameraScaleX = camera.zoom;
 * rain.cameraScaleY = camera.zoom;
 * Flixel.cameras.first().setShader(rain);
 * }</pre>
 *
 * <p>{@link #screenWidth} and {@link #screenHeight} are pre-filled from the current graphics
 * context at construction time. Camera scale defaults to {@code 1.0} (no zoom) and all position
 * fields default to {@code 0}. For sprite-mode shaders, set {@link #spriteX}, {@link #spriteY},
 * {@link #spriteWidth}, and {@link #spriteHeight} before the first draw.
 */
public class FunkinShader extends FlixelShader {

  private static final String FNF_EXTRA_UNIFORMS =
      "uniform vec2 uScreenResolution;\n"
          + "uniform vec2 uCameraPos;\n"
          + "uniform vec2 uCameraScale;\n"
          + "uniform vec2 uSpritePosition;\n"
          + "uniform vec2 uSpriteSize;\n";

  private static final String FNF_DEFINES =
      "#define screenCoord v_texCoords\n";

  // Injected after all uniform declarations so the functions can reference them.
  private static final String FNF_HELPERS =
      "vec2 screenToWorld(vec2 screenPos) {\n"
          + "  return (screenPos - vec2(0.5)) * uScreenResolution * uCameraScale + uCameraPos;\n"
          + "}\n"
          + "vec2 worldToScreen(vec2 worldPos) {\n"
          + "  return (worldPos - uCameraPos) / (uCameraScale * uScreenResolution) + vec2(0.5);\n"
          + "}\n"
          + "vec2 screenToFrame(vec2 spriteLocalUV) {\n"
          + "  return (uSpritePosition + spriteLocalUV * uSpriteSize) / uScreenResolution;\n"
          + "}\n"
          + "vec4 sampleBitmapWorld(vec2 worldPos) {\n"
          + "  return flixel_texture2D(bitmap, worldToScreen(worldPos));\n"
          + "}\n";

  /** Screen width in pixels; controls {@code uScreenResolution.x}. */
  public float screenWidth;
  /** Screen height in pixels; controls {@code uScreenResolution.y}. */
  public float screenHeight;
  /** Camera world position X; controls {@code uCameraPos.x}. */
  public float cameraX;
  /** Camera world position Y; controls {@code uCameraPos.y}. */
  public float cameraY;
  /** Camera horizontal zoom; controls {@code uCameraScale.x}. */
  public float cameraScaleX;
  /** Camera vertical zoom; controls {@code uCameraScale.y}. */
  public float cameraScaleY;
  /** Sprite screen-pixel X origin for sprite-mode shaders; controls {@code uSpritePosition.x}. */
  public float spriteX;
  /** Sprite screen-pixel Y origin for sprite-mode shaders; controls {@code uSpritePosition.y}. */
  public float spriteY;
  /** Sprite pixel width for sprite-mode shaders; controls {@code uSpriteSize.x}. */
  public float spriteWidth;
  /** Sprite pixel height for sprite-mode shaders; controls {@code uSpriteSize.y}. */
  public float spriteHeight;

  private int screenResLoc;
  private int cameraPosLoc;
  private int cameraScaleLoc;
  private int spritePosLoc;
  private int spriteSizeLoc;

  /**
   * Compiles a FNF-compatible shader from a fragment source string.
   *
   * <p>The source is preprocessed through the full FNF pipeline before compilation: HaxeFlixel
   * defines ({@code bitmap}, {@code flixel_texture2D}, etc.) and the FNF coordinate helper
   * functions are injected automatically. Write your shader using {@code #pragma header}
   * as usual.
   *
   * @param fragSrc HaxeFlixel/FNF fragment shader source.
   */
  public FunkinShader(String fragSrc) {
    this(DEFAULT_VERT, fragSrc);
  }

  /**
   * Compiles a FNF-compatible shader with a custom vertex source.
   *
   * <p>Only the fragment source is preprocessed through the FNF pipeline. The vertex source
   * is compiled as-is.
   *
   * @param vertSrc GLSL ES 2.0 vertex shader source.
   * @param fragSrc HaxeFlixel/FNF fragment shader source.
   */
  public FunkinShader(String vertSrc, String fragSrc) {
    super(vertSrc, preprocessFunkin(fragSrc));
    initUniforms();
  }

  /**
   * Creates a {@code FunkinShader} from a HaxeFlixel/FNF-style fragment source string.
   *
   * <p>Equivalent to {@code new FunkinShader(fragSrc)}, provided for consistency with
   * {@link FlixelShader#fromHaxeFlixel(String)}.
   *
   * @param fragSrc HaxeFlixel/FNF fragment shader source.
   * @return A compiled {@code FunkinShader}.
   */
  public static FunkinShader fromHaxeFlixel(String fragSrc) {
    return new FunkinShader(fragSrc);
  }

  /**
   * Runs the FNF shader preprocessing pipeline on a raw fragment source string.
   *
   * <p>The pipeline, in order:
   * <ol>
   *   <li>Removes {@code #pragma header} and {@code #pragma body} markers.</li>
   *   <li>Prepends the standard HaxeFlixel uniform declarations ({@code u_texture},
   *       {@code v_texCoords}, precision qualifier, and so on).</li>
   *   <li>Appends the FNF-specific uniforms ({@code uScreenResolution}, {@code uCameraPos},
   *       and so on).</li>
   *   <li>Appends the HaxeFlixel and FNF {@code #define} aliases.</li>
   *   <li>Appends the GLSL helper functions ({@code screenToWorld}, {@code worldToScreen},
   *       {@code screenToFrame}, {@code sampleBitmapWorld}).</li>
   *   <li>Appends the user's shader body.</li>
   * </ol>
   *
   * <p>Exposed as {@code public} so subclasses can call it from their own factory methods.
   *
   * @param src Raw HaxeFlixel/FNF fragment shader source.
   * @return Preprocessed GLSL ES 2.0 fragment source ready for libGDX compilation.
   */
  public static String preprocessFunkin(String src) {
    src = src.replace("#pragma header", "");
    src = src.replace("#pragma body", "");
    return HAXEFLIXEL_HEADER_EXPANSION
        + FNF_EXTRA_UNIFORMS
        + HAXEFLIXEL_DEFINES
        + FNF_DEFINES
        + FNF_HELPERS
        + src;
  }

  @Override
  public void applyUniforms() {
    super.applyUniforms();
    if (screenResLoc >= 0)
      Gdx.gl20.glUniform2f(screenResLoc, screenWidth, screenHeight);
    if (cameraPosLoc >= 0)
      Gdx.gl20.glUniform2f(cameraPosLoc, cameraX, cameraY);
    if (cameraScaleLoc >= 0)
      Gdx.gl20.glUniform2f(cameraScaleLoc, cameraScaleX, cameraScaleY);
    if (spritePosLoc >= 0)
      Gdx.gl20.glUniform2f(spritePosLoc, spriteX, spriteY);
    if (spriteSizeLoc >= 0)
      Gdx.gl20.glUniform2f(spriteSizeLoc, spriteWidth, spriteHeight);
  }

  private void initUniforms() {
    if (!isCompiled()) {
      return;
    }
    screenResLoc = getProgram().fetchUniformLocation("uScreenResolution", false);
    cameraPosLoc = getProgram().fetchUniformLocation("uCameraPos", false);
    cameraScaleLoc = getProgram().fetchUniformLocation("uCameraScale", false);
    spritePosLoc = getProgram().fetchUniformLocation("uSpritePosition", false);
    spriteSizeLoc = getProgram().fetchUniformLocation("uSpriteSize", false);
    screenWidth = Gdx.graphics.getWidth();
    screenHeight = Gdx.graphics.getHeight();
    cameraScaleX = 1.0f;
    cameraScaleY = 1.0f;
  }
}
