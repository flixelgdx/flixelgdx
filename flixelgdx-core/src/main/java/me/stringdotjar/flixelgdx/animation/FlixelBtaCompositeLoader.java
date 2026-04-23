/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.animation;

import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;

import me.stringdotjar.flixelgdx.graphics.FlixelFrame;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves a BTA/Animate character with a symbol dictionary by composing <strong>every</strong> {@code ASI}
 * (bitmap) instance: matrix chain {@code main SI.MX} × nested {@code SI.MX} × {@code ASI.MX} per
 * <a href="https://github.com/Dot-Stuff/flxanimate">flxanimate</a>-style multi-part playback.
 */
final class FlixelBtaCompositeLoader {

  private static final int MAX_NEST = 6;

  private static final class RawPart {
    int index;
    @NotNull final Matrix3 m = new Matrix3();
  }

  private FlixelBtaCompositeLoader() {}

  static void load(
      @NotNull FlixelAnimateSprite sprite,
      @NotNull FlixelAnimationController controller,
      @NotNull JsonValue animRoot,
      float fps,
      @NotNull ObjectMap<String, Integer> nameToIndex,
      @NotNull Array<FlixelFrame> atlas) {
    FlixelBtaAnLayout layout = FlixelBtaAnLayout.parse(animRoot);
    if (fps <= 0f) {
      fps = 24f;
    }

    String anchorClip = "None";
    boolean haveIdle = false;
    for (int i = 0; i < layout.labelFr.size; i++) {
      JsonValue nn = layout.labelFr.get(i).get("N");
      if (nn != null && "Idle".equals(nn.asString())) {
        haveIdle = true;
        break;
      }
    }
    if (!haveIdle) {
      JsonValue n0 = layout.labelFr.get(0).get("N");
      anchorClip = n0 != null ? n0.asString() : "None";
    }

    float[] anchorBbox = new float[] { Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY,
        Float.NEGATIVE_INFINITY
    };
    for (int c = 0; c < layout.labelFr.size; c++) {
      JsonValue lab = layout.labelFr.get(c);
      if (lab.get("N") == null) {
        continue;
      }
      if (!lab.get("N").asString().equals(anchorClip)) {
        continue;
      }
      JsonValue mainKf = layout.mainFr.get(c);
      Array<RawPart> tmp = new Array<>();
      buildKeyframeParts(layout, mainKf, 0, nameToIndex, tmp);
      expandBboxForParts(tmp, atlas, anchorBbox);
      break;
    }
    if (anchorBbox[0] > anchorBbox[2] || anchorBbox[1] > anchorBbox[3]) {
      anchorBbox[0] = 0f;
      anchorBbox[1] = 0f;
      anchorBbox[2] = 1f;
      anchorBbox[3] = 1f;
    }
    float ax = anchorBbox[0];
    float ay = anchorBbox[1];
    float hitW = anchorBbox[2] - ax;
    float hitH = anchorBbox[3] - ay;

    Matrix3 anchorT = new Matrix3().idt();
    anchorT.val[Matrix3.M02] = -ax;
    anchorT.val[Matrix3.M12] = -ay;

    ObjectMap<String, FlixelBtaCompositing.NamedClip> clips = new ObjectMap<>();
    for (int c = 0; c < layout.labelFr.size; c++) {
      JsonValue lab = layout.labelFr.get(c);
      String clipName = lab.get("N").asString();
      int du = lab.getInt("DU");
      JsonValue mainKf = layout.mainFr.get(c);

      FlixelBtaCompositing.Keyframe[] kfs = new FlixelBtaCompositing.Keyframe[du];
      for (int t = 0; t < du; t++) {
        Array<RawPart> raw = new Array<>();
        buildKeyframeParts(layout, mainKf, t, nameToIndex, raw);
        FlixelBtaCompositing.Part[] parts = new FlixelBtaCompositing.Part[raw.size];
        for (int i = 0; i < raw.size; i++) {
          Matrix3 w = new Matrix3(anchorT);
          w.mul(raw.get(i).m);
          parts[i] = new FlixelBtaCompositing.Part(raw.get(i).index);
          copyMatrix3ToAffine2(w, parts[i].world);
        }
        kfs[t] = new FlixelBtaCompositing.Keyframe(parts);
      }
      clips.put(clipName, new FlixelBtaCompositing.NamedClip(clipName, kfs, hitW, hitH));
    }

    for (int c = 0; c < layout.labelFr.size; c++) {
      JsonValue lab = layout.labelFr.get(c);
      String name = lab.get("N").asString();
      int du = lab.getInt("DU");
      int[] indices = new int[du];
      for (int i = 0; i < du; i++) {
        indices[i] = 0;
      }
      if (atlas.size > 0) {
        controller.addAnimationFromAtlas(name, indices, 1f / fps, true);
      }
    }

    FlixelBtaCompositing comp = new FlixelBtaCompositing(clips, anchorClip, hitW, hitH);
    sprite.installBtaCompositing(comp, hitW, hitH);
  }

  private static void buildKeyframeParts(
      @NotNull FlixelBtaAnLayout layout,
      @NotNull JsonValue mainKf,
      int frameTime,
      @NotNull ObjectMap<String, Integer> nameToIndex,
      @NotNull Array<RawPart> out) {
    JsonValue e = mainKf.get("E");
    if (e == null || !e.isArray() || e.size == 0) {
      return;
    }
    JsonValue e0 = e.child;
    if (e0 == null) {
      e0 = e.get(0);
    }
    JsonValue si = e0 != null ? e0.get("SI") : null;
    if (si == null) {
      return;
    }
    String childSn = si.get("SN").asString();
    Matrix3 root = matrixFromFrSi(si);
    visitSymbol(layout.symByName, nameToIndex, childSn, frameTime, root, out, 0);
  }

  private static void expandBboxForParts(
      @NotNull Array<RawPart> parts,
      @NotNull Array<FlixelFrame> atlas,
      @NotNull float[] minmax) {
    for (int i = 0; i < parts.size; i++) {
      RawPart p = parts.get(i);
      if (p.index < 0 || p.index >= atlas.size) {
        continue;
      }
      FlixelFrame f = atlas.get(p.index);
      float w = f.getRegionWidth();
      float h = f.getRegionHeight();
      transformCorner(p.m, 0, 0, minmax);
      transformCorner(p.m, w, 0, minmax);
      transformCorner(p.m, w, h, minmax);
      transformCorner(p.m, 0, h, minmax);
    }
  }

  private static void transformCorner(@NotNull Matrix3 m, float x, float y, @NotNull float[] minmax) {
    float x2 = m.val[0] * x + m.val[1] * y + m.val[2];
    float y2 = m.val[3] * x + m.val[4] * y + m.val[5];
    if (x2 < minmax[0]) {
      minmax[0] = x2;
    }
    if (y2 < minmax[1]) {
      minmax[1] = y2;
    }
    if (x2 > minmax[2]) {
      minmax[2] = x2;
    }
    if (y2 > minmax[3]) {
      minmax[3] = y2;
    }
  }

  private static void visitSymbol(
      @NotNull ObjectMap<String, JsonValue> symByName,
      @NotNull ObjectMap<String, Integer> nameToIndex,
      @NotNull String sn,
      int localT,
      @NotNull Matrix3 world,
      @NotNull Array<RawPart> out,
      int depth) {
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
              int idx = resolveNameToIndex(n.asString(), nameToIndex);
              if (idx < 0) {
                continue;
              }
              Matrix3 combined = new Matrix3(world);
              combined.mul(matrixFromAsi(asi));
              RawPart p = new RawPart();
              p.index = idx;
              p.m.set(combined);
              out.add(p);
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
              if (ffp != null && ffp.isNumber()) {
                ff = ffp.asInt();
              }
            }
            int childT = within + ff;
            Matrix3 childWorld = new Matrix3(world);
            childWorld.mul(matrixFromFrSi(si));
            int sizeBefore = out.size;
            visitSymbol(symByName, nameToIndex, csn.asString(), childT, childWorld, out, depth + 1);
            if (out.size == sizeBefore) {
              visitSymbol(symByName, nameToIndex, csn.asString(), localT, childWorld, out, depth + 1);
            }
            if (out.size == sizeBefore) {
              visitSymbol(symByName, nameToIndex, csn.asString(), 0, childWorld, out, depth + 1);
            }
          }
        }
      }
    }
  }

  private static int resolveNameToIndex(
      @NotNull String name, @NotNull ObjectMap<String, Integer> nameToIndex) {
    if (name.isEmpty()) {
      return -1;
    }
    Integer idx = nameToIndex.get(name);
    if (idx != null) {
      return idx;
    }
    try {
      int k = name.indexOf('.');
      String s = (k < 0) ? name : name.substring(0, k);
      Integer idx2 = nameToIndex.get(s);
      if (idx2 != null) {
        return idx2;
      }
      int n = (int) Double.parseDouble(name);
      Integer idx3 = nameToIndex.get(String.valueOf(n));
      if (idx3 != null) {
        return idx3;
      }
    } catch (NumberFormatException ignored) {
      // Fall through.
    }
    return -1;
  }

  /**
   * Flash-style {@code [a, b, c, d, tx, ty]}: {@code x' = a x + c y + tx}, {@code y' = b x + d y + ty}.
   */
  @NotNull
  private static Matrix3 matrixFrom6(@Nullable JsonValue mx) {
    Matrix3 o = new Matrix3();
    if (mx == null || !mx.isArray() || mx.size < 6) {
      o.idt();
      return o;
    }
    float a = mx.get(0).asFloat();
    float b = mx.get(1).asFloat();
    float c = mx.get(2).asFloat();
    float d = mx.get(3).asFloat();
    float tx = mx.get(4).asFloat();
    float ty = mx.get(5).asFloat();
    o.val[0] = a;
    o.val[1] = c;
    o.val[2] = tx;
    o.val[3] = b;
    o.val[4] = d;
    o.val[5] = ty;
    o.val[6] = 0f;
    o.val[7] = 0f;
    o.val[8] = 1f;
    return o;
  }

  @NotNull
  private static Matrix3 matrixFromFrSi(@NotNull JsonValue si) {
    return matrixFrom6(si.get("MX"));
  }

  @NotNull
  private static Matrix3 matrixFromAsi(@NotNull JsonValue asi) {
    return matrixFrom6(asi.get("MX"));
  }

  private static void copyMatrix3ToAffine2(@NotNull Matrix3 m, @NotNull Affine2 a) {
    a.set(m);
  }
}
