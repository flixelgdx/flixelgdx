/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.animation;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Parsed {@code AN.TL} (named clip labels + main symbol layer) and {@code SD} symbol index
 * for Adobe BTA/Animate character atlases. Shared by the single-sprite and compositing resolvers.
 */
final class FlixelBtaAnLayout {

  @NotNull final Array<JsonValue> labelFr;
  @NotNull final Array<JsonValue> mainFr;
  @NotNull final ObjectMap<String, JsonValue> symByName;

  private FlixelBtaAnLayout(
      @NotNull Array<JsonValue> labelFr,
      @NotNull Array<JsonValue> mainFr,
      @NotNull ObjectMap<String, JsonValue> symByName) {
    this.labelFr = labelFr;
    this.mainFr = mainFr;
    this.symByName = symByName;
  }

  @NotNull
  static FlixelBtaAnLayout parse(@NotNull JsonValue animRoot) {
    JsonValue an = animRoot.get("AN");
    JsonValue tl = an != null ? an.get("TL") : null;
    JsonValue layers = tl != null ? tl.get("L") : null;

    if (layers == null || !layers.isArray()) {
      throw new IllegalArgumentException("BTA SD: missing AN.TL.L.");
    }

    JsonValue labelFrLayer = null;
    for (JsonValue layer = layers.child; layer != null; layer = layer.next) {
      JsonValue ln = layer.get("LN");
      String lname = (ln != null) ? ln.asString() : "";
      if (!"Layer_3".equals(lname)) {
        continue;
      }
      JsonValue frs = layer.get("FR");
      if (frs == null || !frs.isArray() || frs.size == 0) {
        continue;
      }
      for (JsonValue fr = frs.child; fr != null; fr = fr.next) {
        if (fr.get("N") != null) {
          labelFrLayer = frs;
          break;
        }
      }
      if (labelFrLayer != null) {
        break;
      }
    }

    if (labelFrLayer == null) {
      for (JsonValue layer = layers.child; layer != null; layer = layer.next) {
        JsonValue frs = layer.get("FR");
        if (frs == null || !frs.isArray() || frs.size == 0) {
          continue;
        }
        for (JsonValue fr = frs.child; fr != null; fr = fr.next) {
          if (fr.get("N") != null) {
            labelFrLayer = frs;
            break;
          }
        }
        if (labelFrLayer != null) {
          break;
        }
      }
    }

    JsonValue mainFrLayer = null;
    for (JsonValue layer = layers.child; layer != null; layer = layer.next) {
      JsonValue ln = layer.get("LN");
      String lname = (ln != null) ? ln.asString() : "";
      JsonValue frs = layer.get("FR");
      if (frs == null || !frs.isArray() || frs.size == 0) {
        continue;
      }
      if ("Layer_1".equals(lname)) {
        mainFrLayer = frs;
        break;
      }
    }

    if (mainFrLayer == null) {
      for (JsonValue layer = layers.child; layer != null; layer = layer.next) {
        JsonValue frs = layer.get("FR");
        if (frs == null || !frs.isArray() || frs.size == 0) {
          continue;
        }
        JsonValue first = frs.child;
        if (first == null) {
          continue;
        }
        JsonValue e0 = first.get("E");
        if (e0 != null && e0.isArray() && e0.size > 0) {
          JsonValue ev0 = e0.child;
          if (ev0 != null && ev0.get("SI") != null) {
            mainFrLayer = frs;
            break;
          }
        }
      }
    }

    if (labelFrLayer == null || mainFrLayer == null) {
      throw new IllegalArgumentException(
        "BTA SD: need a label layer (FR with N) and a main layer (FR with E.SI or LN Layer_1).");
    }

    ObjectMap<String, JsonValue> symByName = new ObjectMap<>();
    indexSymbols(animRoot.get("SD"), symByName);
    if (symByName.size == 0) {
      throw new IllegalArgumentException("BTA SD: SD.S is empty or missing.");
    }

    Array<JsonValue> labelFr = new Array<>();
    for (JsonValue fr = labelFrLayer.child; fr != null; fr = fr.next) {
      if (fr.get("N") != null) {
        labelFr.add(fr);
      }
    }

    Array<JsonValue> mainFr = new Array<>();
    for (JsonValue fr = mainFrLayer.child; fr != null; fr = fr.next) {
      mainFr.add(fr);
    }

    if (labelFr.size != mainFr.size) {
      throw new IllegalArgumentException(
        "BTA SD: label layer has " + labelFr.size + " named clips but main layer has " + mainFr.size
          + " keyframes.");
    }

    return new FlixelBtaAnLayout(labelFr, mainFr, symByName);
  }

  private static void indexSymbols(@Nullable JsonValue sd, @NotNull ObjectMap<String, JsonValue> out) {
    if (sd == null) {
      return;
    }
    JsonValue s = sd.get("S");
    if (s == null || !s.isArray()) {
      return;
    }
    for (JsonValue sym = s.child; sym != null; sym = sym.next) {
      JsonValue sn = sym.get("SN");
      if (sn == null) {
        continue;
      }
      out.put(sn.asString(), sym);
    }
  }
}
