/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.animation;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;

import me.stringdotjar.flixelgdx.graphics.FlixelFrame;

import org.jetbrains.annotations.NotNull;

/**
 * Resolves Adobe BTA/Animate JSON with an {@code SD} (symbol) dictionary.
 *
 * <p>Linear {@code I}/{@code DU} on the main label layer is only the outer timeline, the art is in nested
 * {@code SD.S} symbols with per-layer keyframes, {@code ASI.N} pointing at {@code spritemap} cell names, and
 * {@code SI} nesting more symbols.
 *
 * <p>For a plain {@link me.stringdotjar.flixelgdx.FlixelSprite} (not {@link FlixelAnimateSprite}), this helper walks
 * that graph and picks <strong>one</strong> raster per frame (largest by pixel area) so a single sprite can
 * roughly approximate a keyframe. Use {@link FlixelAnimateSprite} for true multi-part composition.
 */
final class FlixelBtaSymbolAnimationResolver {

  private static final int MAX_NEST = 6;

  private FlixelBtaSymbolAnimationResolver() {}

  static void loadAnimations(
      @NotNull FlixelAnimationController controller,
      @NotNull JsonValue animRoot,
      float fps,
      @NotNull ObjectMap<String, Integer> spritemapNameToIndex,
      @NotNull Array<FlixelFrame> atlas) {
    FlixelBtaAnLayout layout = FlixelBtaAnLayout.parse(animRoot);

    IntArray cands = new IntArray(32);
    for (int c = 0; c < layout.labelFr.size; c++) {
      JsonValue lab = layout.labelFr.get(c);
      String clipName = lab.get("N").asString();
      int du = lab.getInt("DU");
      JsonValue mainKf = layout.mainFr.get(c);
      JsonValue e = mainKf.get("E");

      if (e == null || !e.isArray() || e.size == 0) {
        throw new IllegalArgumentException("BTA SD: main keyframe for \"" + clipName + "\" has no E[] block.");
      }

      JsonValue e0 = e.child;
      if (e0 == null) {
        e0 = e.get(0);
      }

      JsonValue si = (e0 != null) ? e0.get("SI") : null;
      if (si == null) {
        throw new IllegalArgumentException("BTA SD: expected SI in main keyframe for \"" + clipName + "\".");
      }

      String childSn = si.get("SN").asString();
      int[] frames = new int[du];
      for (int t = 0; t < du; t++) {
        cands.size = 0;
        visitSymbol(layout.symByName, childSn, t, cands, 0, spritemapNameToIndex);
        frames[t] = pickLargest(cands, atlas);
      }
      if (fps <= 0f) {
        fps = 24f;
      }
      controller.addAnimationFromAtlas(clipName, frames, 1f / fps, true);
    }
  }

  private static void visitSymbol(
      @NotNull ObjectMap<String, JsonValue> symByName,
      @NotNull String sn,
      int localT,
      @NotNull IntArray cands,
      int depth,
      @NotNull ObjectMap<String, Integer> spritemapNameToIndex) {
    if (depth > MAX_NEST) {
      return;
    }
    JsonValue sym = symByName.get(sn);
    if (sym == null) {
      return;
    }
    JsonValue symTl = sym.get("TL");
    if (symTl == null) {
      return;
    }
    JsonValue lArr = symTl.get("L");
    if (lArr == null || !lArr.isArray()) {
      return;
    }

    for (JsonValue layer = lArr.child; layer != null; layer = layer.next) {
      JsonValue frs = layer.get("FR");
      if (frs == null || !frs.isArray()) {
        continue;
      }

      for (JsonValue fr = frs.child; fr != null; fr = fr.next) {
        int i0 = fr.getInt("I");
        int d = fr.getInt("DU");
        if (localT < i0 || localT >= i0 + d) {
          continue;
        }
        int within = localT - i0;
        JsonValue eArr = fr.get("E");
        if (eArr == null || !eArr.isArray()) {
          continue;
        }

        for (JsonValue ev = eArr.child; ev != null; ev = ev.next) {
          JsonValue asi = ev.get("ASI");
          if (asi != null) {
            JsonValue n = asi.get("N");
            if (n != null) {
              addNamedRaster(n.asString(), cands, spritemapNameToIndex);
            }
          }

          JsonValue si = ev.get("SI");
          if (si != null) {
            JsonValue csn = si.get("SN");
            if (csn == null) {
              continue;
            }

            int ff = 0;
            if (si.has("FF")) {
              JsonValue ffp = si.get("FF");
              if (ffp != null) {
                ff = ffp.isNumber() ? ffp.asInt() : 0;
              }
            }

            int childT = within + ff;
            visitSymbol(symByName, csn.asString(), childT, cands, depth + 1, spritemapNameToIndex);
            if (cands.size < 1) {
              visitSymbol(symByName, csn.asString(), localT, cands, depth + 1, spritemapNameToIndex);
            }
            if (cands.size < 1) {
              visitSymbol(symByName, csn.asString(), 0, cands, depth + 1, spritemapNameToIndex);
            }
          }
        }
      }
    }
  }

  private static void addNamedRaster(
      @NotNull String name, @NotNull IntArray cands, @NotNull ObjectMap<String, Integer> nameToIndex) {
    if (name.isEmpty()) {
      return;
    }

    Integer idx = nameToIndex.get(name);
    if (idx == null) {
      try {
        int k = name.indexOf('.');
        String s = (k < 0) ? name : name.substring(0, k);
        idx = nameToIndex.get(s);
        if (idx == null) {
          int n = (int) Double.parseDouble(name);
          idx = nameToIndex.get(String.valueOf(n));
        }
      } catch (NumberFormatException ignored) {
        // Keep index null.
      }
    }
    if (idx != null && idx >= 0) {
      cands.add(idx);
    }
  }

  private static int pickLargest(@NotNull IntArray cands, @NotNull Array<FlixelFrame> atlas) {
    if (cands.size == 0) {
      return 0;
    }
    int best = -1;
    int bestArea = -1;
    for (int i = 0; i < cands.size; i++) {
      int idx = cands.get(i);
      if (idx < 0 || idx >= atlas.size) {
        continue;
      }
      FlixelFrame f = atlas.get(idx);
      int a = f.getRegionWidth() * f.getRegionHeight();
      if (a > bestArea) {
        bestArea = a;
        best = idx;
      }
    }
    return (best >= 0) ? best : 0;
  }
}
