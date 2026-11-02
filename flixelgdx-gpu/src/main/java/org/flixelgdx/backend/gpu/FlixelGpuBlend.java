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
package org.flixelgdx.backend.gpu;

import org.flixelgdx.util.FlixelBlendMode;
import org.jetbrains.annotations.NotNull;

import io.github.stringdotjar.jwebgpu.GpuBlendFactor;
import io.github.stringdotjar.jwebgpu.GpuBlendOperation;
import io.github.stringdotjar.jwebgpu.GpuBlendState;

/**
 * Turns FlixelGDX blend modes into WebGPU blend state.
 *
 * <p>Shaders return straight color, but the framework adds a fragment entry point that premultiplies it (see
 * {@code FlixelWgsl}), so the blender normally receives premultiplied color. That is the only form in which every
 * mode respects alpha: fully transparent pixels contribute nothing, whatever color they hide. The formulas, with
 * {@code P} the premultiplied source color, {@code a} the source alpha and {@code D} the destination:
 *
 * <ul>
 *   <li>Normal: {@code P + D * (1 - a)}.</li>
 *   <li>Add: {@code P + D}.</li>
 *   <li>Multiply: {@code P * D + D * (1 - a)}, a pure multiply where opaque and no change where clear.</li>
 *   <li>Screen: {@code P + D * (1 - P)}.</li>
 *   <li>Subtract: {@code D - P}.</li>
 *   <li>Lighten and darken: {@code max(P, D)} and {@code min(P', D)}, where darken's entry point outputs
 *       {@code P' = P + (1 - a)} so transparent pixels count as white. Both are exact where the source is opaque
 *       or clear.</li>
 * </ul>
 *
 * <p>A shader the framework cannot wrap still outputs straight color; {@link #fill(int, boolean, GpuBlendState)}
 * then uses the straight-alpha factors, which respect alpha for normal, add and subtract only.
 *
 * <p>The alpha channel always accumulates with a standard "over" ({@code srcA = ONE}, {@code dstA = 1 - srcA})
 * no matter what the color channels do. That keeps the alpha a camera or global shader later reads out of a
 * render target correct, so blended sprites composite properly instead of punching holes or squaring their own
 * alpha.
 */
public final class FlixelGpuBlend {

  /** Blending is off: the source replaces the destination. */
  public static final int NONE = 0;

  /** Standard straight-alpha "over". */
  public static final int NORMAL = 1;

  /** Adds the source, scaled by its alpha, to the destination. */
  public static final int ADD = 2;

  /** Multiplies the destination by the source color. */
  public static final int MULTIPLY = 3;

  /** Inverts, multiplies and inverts again, which only ever brightens. */
  public static final int SCREEN = 4;

  /** Subtracts the source, scaled by its alpha, from the destination. */
  public static final int SUBTRACT = 5;

  /** Keeps the lighter of the source and destination in each channel. */
  public static final int LIGHTEN = 6;

  /** Keeps the darker of the source and destination in each channel. */
  public static final int DARKEN = 7;

  /** Internal: keeps the destination color and writes the source alpha. Used to force an opaque back buffer. */
  public static final int FORCE_ALPHA = 8;

  private FlixelGpuBlend() {}

  /**
   * Returns the blend id for a blend mode.
   *
   * @param mode The blend mode.
   * @return One of the id constants of this class.
   */
  public static int id(@NotNull FlixelBlendMode mode) {
    return switch (mode) {
      case NONE -> NONE;
      case NORMAL -> NORMAL;
      case ADD -> ADD;
      case MULTIPLY -> MULTIPLY;
      case SCREEN -> SCREEN;
      case SUBTRACT -> SUBTRACT;
      case LIGHTEN -> LIGHTEN;
      case DARKEN -> DARKEN;
    };
  }

  /**
   * Fills a blend state for a blend id and premultiplied shader output.
   *
   * @param id The blend id.
   * @param out The blend state to fill. Every field is overwritten.
   * @return {@code false} when blending is off ({@link #NONE}), in which case the color target should have no
   *     blend state at all; {@code true} when {@code out} is filled and should be used.
   */
  public static boolean fill(int id, @NotNull GpuBlendState out) {
    return fill(id, true, out);
  }

  /**
   * Fills a blend state for a blend id.
   *
   * @param id The blend id.
   * @param premultiplied Whether the shader output is premultiplied (the normal case) or straight.
   * @param out The blend state to fill. Every field is overwritten.
   * @return {@code false} when blending is off ({@link #NONE}), in which case the color target should have no
   *     blend state at all; {@code true} when {@code out} is filled and should be used.
   */
  public static boolean fill(int id, boolean premultiplied, @NotNull GpuBlendState out) {
    if (premultiplied) {
      return fillPremultiplied(id, out);
    }
    return fillStraight(id, out);
  }

  private static boolean fillPremultiplied(int id, GpuBlendState out) {
    if (id == NONE) {
      return false;
    }
    GpuBlendFactor srcColor = GpuBlendFactor.ONE;
    GpuBlendFactor dstColor = GpuBlendFactor.ONE_MINUS_SRC_ALPHA;
    GpuBlendOperation colorOp = GpuBlendOperation.ADD;
    GpuBlendFactor srcAlpha = GpuBlendFactor.ONE;
    GpuBlendFactor dstAlpha = GpuBlendFactor.ONE_MINUS_SRC_ALPHA;
    switch (id) {
      case ADD -> dstColor = GpuBlendFactor.ONE;
      case MULTIPLY -> srcColor = GpuBlendFactor.DST;
      case SCREEN -> dstColor = GpuBlendFactor.ONE_MINUS_SRC;
      case SUBTRACT -> {
        dstColor = GpuBlendFactor.ONE;
        colorOp = GpuBlendOperation.REVERSE_SUBTRACT;
      }
      case LIGHTEN -> {
        dstColor = GpuBlendFactor.ONE;
        colorOp = GpuBlendOperation.MAX;
      }
      case DARKEN -> {
        dstColor = GpuBlendFactor.ONE;
        colorOp = GpuBlendOperation.MIN;
      }
      case FORCE_ALPHA -> {
        srcColor = GpuBlendFactor.ZERO;
        dstColor = GpuBlendFactor.ONE;
        dstAlpha = GpuBlendFactor.ZERO;
      }
      default -> {
      }
    }
    out.color.srcFactor = srcColor;
    out.color.dstFactor = dstColor;
    out.color.operation = colorOp;
    out.alpha.srcFactor = srcAlpha;
    out.alpha.dstFactor = dstAlpha;
    out.alpha.operation = GpuBlendOperation.ADD;
    return true;
  }

  private static boolean fillStraight(int id, GpuBlendState out) {
    if (id == NONE) {
      return false;
    }
    GpuBlendFactor srcColor = GpuBlendFactor.SRC_ALPHA;
    GpuBlendFactor dstColor = GpuBlendFactor.ONE_MINUS_SRC_ALPHA;
    GpuBlendOperation colorOp = GpuBlendOperation.ADD;
    GpuBlendFactor srcAlpha = GpuBlendFactor.ONE;
    GpuBlendFactor dstAlpha = GpuBlendFactor.ONE_MINUS_SRC_ALPHA;
    switch (id) {
      case ADD -> dstColor = GpuBlendFactor.ONE;
      case MULTIPLY -> {
        srcColor = GpuBlendFactor.DST;
        dstColor = GpuBlendFactor.ONE_MINUS_SRC_ALPHA;
      }
      case SCREEN -> {
        srcColor = GpuBlendFactor.ONE;
        dstColor = GpuBlendFactor.ONE_MINUS_SRC;
      }
      case SUBTRACT -> {
        dstColor = GpuBlendFactor.ONE;
        colorOp = GpuBlendOperation.REVERSE_SUBTRACT;
      }
      case LIGHTEN -> {
        srcColor = GpuBlendFactor.ONE;
        dstColor = GpuBlendFactor.ONE;
        colorOp = GpuBlendOperation.MAX;
      }
      case DARKEN -> {
        srcColor = GpuBlendFactor.ONE;
        dstColor = GpuBlendFactor.ONE;
        colorOp = GpuBlendOperation.MIN;
      }
      case FORCE_ALPHA -> {
        srcColor = GpuBlendFactor.ZERO;
        dstColor = GpuBlendFactor.ONE;
        srcAlpha = GpuBlendFactor.ONE;
        dstAlpha = GpuBlendFactor.ZERO;
      }
      default -> {
      }
    }
    out.color.srcFactor = srcColor;
    out.color.dstFactor = dstColor;
    out.color.operation = colorOp;
    out.alpha.srcFactor = srcAlpha;
    out.alpha.dstFactor = dstAlpha;
    out.alpha.operation = GpuBlendOperation.ADD;
    return true;
  }
}
