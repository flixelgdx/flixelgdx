/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.animation;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import me.stringdotjar.flixelgdx.Flixel;
import me.stringdotjar.flixelgdx.graphics.FlixelFrame;
import me.stringdotjar.flixelgdx.graphics.FlixelGraphic;

import java.util.Comparator;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

/**
 * Helper class that loads Adobe/CreateJS-style spritemap JSON plus an animation definition JSON and applies them to
 * a {@link FlixelSprite} through a {@link FlixelAnimationController}. Kept out of
 * {@link FlixelAnimationController} so the controller file stays a thin timing and playback API.
 */
public final class FlixelSpritemapJsonLoader {

  private FlixelSpritemapJsonLoader() {}

  /**
   * Resolves a path the same way as other FlixelGDX file helpers, which is {@code internal} first, then
   * {@code classpath}. Reads the file as UTF-8 text.
   *
   * @param path The path to resolve.
   * @return The resolved file handle.
   */
  @NotNull
  public static FileHandle resolveAssetPath(@NotNull String path) {
    Objects.requireNonNull(path, "path cannot be null.");
    if (path.isEmpty()) {
      throw new IllegalArgumentException("path cannot be empty.");
    }
    FileHandle f = Gdx.files.internal(path);
    if (f.exists() && !f.isDirectory()) {
      return f;
    }
    f = Gdx.files.classpath(path);
    if (f != null && f.exists() && !f.isDirectory()) {
      return f;
    }
    throw new IllegalArgumentException("No readable file for path: " + path);
  }

  /**
   * Reads the file as UTF-8 and strips a leading BOM if present. Use for XML and JSON so the path does
   * not fall back to the platform default charset.
   */
  @NotNull
  public static String readUtf8Text(@NotNull FileHandle f) {
    String s = f.readString("UTF-8");
    if (s != null && s.length() > 0 && s.charAt(0) == '\uFEFF') {
      return s.substring(1);
    }
    return s != null ? s : "";
  }

  /**
   * Reads spritemap and animation JSON, applies atlas frames, then registers one clip per animation.
   * 
   * <p>Spritemap may list frames as a JSON array of numeric rows {@code [x, y, w, h, ...]} (first four
   * values are the texture rect) or as an object map of frame name to
   * {@code { "frame": { "x", "y", "w", "h" } } } (names become {@link FlixelFrame#name}).
   * 
   * <p>Animation files may be:
   * <ul>
   *   <li>Simple: {@code "animations": { "name": { "frames": [0, 1, 2] } } } with optional root {@code framerate}.</li>
   *   <li>Friday Night Funkin / Adobe BTA: root {@code AN.TL.L} with a layer whose {@code FR} entries have
   *   {@code N} (clip name), {@code I} (start frame index), and {@code DU} (length). Root {@code MD.FRT} is
   *   the frame rate. Spritemaps for that family use {@code ATLAS.SPRITES} (see
   *   <a href="https://github.com/FunkinCrew/funkin.assets">funkin.assets</a> example {@code bf}).</li>
   * </ul>
   *
   * @param controller The animation controller to load the spritemap into.
   * @param textureKey The key of the texture to load.
   * @param spritemapJsonPath The path to the spritemap JSON file.
   * @param animationJsonPath The path to the animation JSON file.
   */
  public static void load(
      @NotNull FlixelAnimationController controller,
      @NotNull String textureKey,
      @NotNull String spritemapJsonPath,
      @NotNull String animationJsonPath) {
    String smText = readUtf8Text(resolveAssetPath(spritemapJsonPath));
    String anText = readUtf8Text(resolveAssetPath(animationJsonPath));
    JsonValue spritemapRoot = new JsonReader().parse(smText);
    JsonValue animRoot = new JsonReader().parse(anText);

    FlixelGraphic g = Flixel.ensureAssets().obtainWrapper(textureKey, FlixelGraphic.class);
    Texture texture;
    try {
      texture = g.requireTexture();
    } catch (IllegalStateException e) {
      texture = g.loadNow();
    }

    Array<FlixelFrame> frames = buildFrames(spritemapRoot, texture);
    controller.getOwner().applySparrowAtlas(g, frames);

    if (animRoot.get("animations") != null) {
      loadSimpleAnimationsJson(controller, animRoot);
    } else if (animRoot.get("AN") != null) {
      loadAdobeBtaAnJson(controller, animRoot);
    } else {
      throw new IllegalArgumentException(
        "Animation JSON has no \"animations\" object and no \"AN\" (Adobe BTA) block. Unrecognized format.");
    }
  }

  private static void loadSimpleAnimationsJson(
      @NotNull FlixelAnimationController controller, @NotNull JsonValue animRoot) {
    float defaultFps = readFloat(animRoot, "framerate", 24f);
    JsonValue anims = animRoot.get("animations");
    if (anims == null || !anims.isObject()) {
      throw new IllegalArgumentException("Animation JSON must have an object \"animations\" field.");
    }
    for (JsonValue anim = anims.child; anim != null; anim = anim.next) {
      String name = anim.name;
      if (name == null || name.isEmpty()) {
        continue;
      }
      float fps = readFloat(anim, "framerate", defaultFps);
      JsonValue framesNode = anim.get("frames");
      if (framesNode == null || !framesNode.isArray()) {
        throw new IllegalArgumentException("Animation \"" + name + "\" needs a \"frames\" array of indices.");
      }
      int n = 0;
      for (JsonValue c = framesNode.child; c != null; c = c.next) {
        n++;
      }
      int[] indices = new int[n];
      int w = 0;
      for (JsonValue f = framesNode.child; f != null; f = f.next) {
        indices[w++] = f.asInt();
      }
      boolean loop = readBoolean(anim, "loop", true);
      if (fps <= 0f) {
        fps = 24f;
      }
      controller.addAnimationFromAtlas(name, indices, 1f / fps, loop);
    }
  }

  /**
   * Adobe Animate (Better TA) / FNF-style {@code Animation.json}: first timeline layer where {@code FR} entries
   * include {@code N} (name), {@code I} (start index), {@code DU} (inclusive length).
   */
  private static void loadAdobeBtaAnJson(
      @NotNull FlixelAnimationController controller, @NotNull JsonValue animRoot) {
    JsonValue md = animRoot.get("MD");
    float fps = (md != null) ? readFloat(md, "FRT", 24f) : 24f;
    if (fps <= 0f) {
      fps = 24f;
    }
    JsonValue an = animRoot.get("AN");
    if (an == null) {
      throw new IllegalArgumentException("Adobe BTA JSON missing \"AN\".");
    }
    JsonValue tl = an.get("TL");
    if (tl == null) {
      throw new IllegalArgumentException("Adobe BTA JSON missing \"AN.TL\".");
    }
    JsonValue layers = tl.get("L");
    if (layers == null || !layers.isArray()) {
      throw new IllegalArgumentException("Adobe BTA JSON missing \"AN.TL.L\" array.");
    }
    for (JsonValue layer = layers.child; layer != null; layer = layer.next) {
      JsonValue frs = layer.get("FR");
      if (frs == null || !frs.isArray()) {
        continue;
      }
      boolean hasNamed = false;
      for (JsonValue fr = frs.child; fr != null; fr = fr.next) {
        if (fr.get("N") != null) {
          hasNamed = true;
          break;
        }
      }
      if (!hasNamed) {
        continue;
      }
      for (JsonValue fr = frs.child; fr != null; fr = fr.next) {
        JsonValue nameNode = fr.get("N");
        if (nameNode == null) {
          continue;
        }
        String name = nameNode.asString();
        int start = fr.getInt("I");
        int du = fr.getInt("DU");
        if (du < 1) {
          continue;
        }
        int[] indices = new int[du];
        for (int i = 0; i < du; i++) {
          indices[i] = start + i;
        }
        controller.addAnimationFromAtlas(name, indices, 1f / fps, true);
      }
      return;
    }
    throw new IllegalArgumentException(
      "Adobe BTA JSON: no layer in AN.TL.L with named FR entries (N, I, DU).");
  }

  private static float readFloat(JsonValue o, String key, float dflt) {
    JsonValue v = o.get(key);
    return (v == null) ? dflt : v.asFloat();
  }

  private static boolean readBoolean(JsonValue o, String key, boolean dflt) {
    JsonValue v = o.get(key);
    if (v == null) {
      return dflt;
    }
    if (v.isBoolean()) {
      return v.asBoolean();
    }
    if (v.isNumber()) {
      return v.asInt() != 0;
    }
    return dflt;
  }

  @NotNull
  private static Array<FlixelFrame> buildFrames(@NotNull JsonValue root, @NotNull Texture texture) {
    JsonValue atlas = root.get("ATLAS");
    if (atlas != null) {
      return buildFnfAtlasFrames(atlas, texture);
    }
    JsonValue framesNode = root.get("frames");
    if (framesNode == null) {
      throw new IllegalArgumentException(
        "Spritemap JSON must contain \"ATLAS.SPRITES\" (FNF) or a top-level \"frames\" field.");
    }
    Array<FlixelFrame> out = new Array<>();
    if (framesNode.isArray()) {
      int index = 0;
      for (JsonValue row = framesNode.child; row != null; row = row.next) {
        if (!row.isArray() || row.size < 4) {
          throw new IllegalArgumentException("Each spritemap frame row must be an array with at least 4 numbers.");
        }
        int x = row.get(0).asInt();
        int y = row.get(1).asInt();
        int w = row.get(2).asInt();
        int h = row.get(3).asInt();
        FlixelFrame frame = buildFrame(texture, x, y, w, h, "frame" + index);
        out.add(frame);
        index++;
      }
      return out;
    }
    if (framesNode.isObject()) {
      for (JsonValue v = framesNode.child; v != null; v = v.next) {
        String fname = v.name;
        if (fname == null) {
          continue;
        }
        if (!v.isObject()) {
          continue;
        }
        JsonValue fr = v.get("frame");
        if (fr == null) {
          fr = v;
        }
        int x = getIntField(fr, "x");
        int y = getIntField(fr, "y");
        int w = getIntField(fr, "w", "width");
        int h2 = getIntField(fr, "h", "height");
        FlixelFrame frame = buildFrame(texture, x, y, w, h2, fname);
        out.add(frame);
      }
      return out;
    }
    throw new IllegalArgumentException("Spritemap \"frames\" must be a JSON array or object.");
  }

  @NotNull
  private static Array<FlixelFrame> buildFnfAtlasFrames(@NotNull JsonValue atlas, @NotNull Texture texture) {
    JsonValue sprites = atlas.get("SPRITES");
    if (sprites == null || !sprites.isArray()) {
      throw new IllegalArgumentException("ATLAS.SPRITES must be a JSON array.");
    }
    Array<JsonSprite> list = new Array<>();
    for (JsonValue el = sprites.child; el != null; el = el.next) {
      JsonValue sp = el.get("SPRITE");
      if (sp == null) {
        sp = el;
      }
      if (sp == null || !sp.isObject()) {
        continue;
      }
      JsonValue nameN = sp.get("name");
      String name = (nameN != null) ? nameN.asString() : "";
      int x = sp.getInt("x");
      int y = sp.getInt("y");
      int w = sp.getInt("w");
      int h = sp.getInt("h");
      list.add(new JsonSprite(name, x, y, w, h));
    }
    if (list.size == 0) {
      throw new IllegalArgumentException("ATLAS.SPRITES contained no valid SPRITE entries.");
    }
    list.sort(Comparator.comparingInt(JsonSprite::sortKey));
    Array<FlixelFrame> out = new Array<>();
    for (int i = 0; i < list.size; i++) {
      JsonSprite s = list.get(i);
      out.add(buildFrame(texture, s.x, s.y, s.w, s.h, s.name));
    }
    return out;
  }

  private static final class JsonSprite {
    final String name;
    final int x, y, w, h;

    JsonSprite(String name, int x, int y, int w, int h) {
      this.name = name;
      this.x = x;
      this.y = y;
      this.w = w;
      this.h = h;
    }

    int sortKey() {
      if (name == null) {
        return 0;
      }
      try {
        return Integer.parseInt(name.trim());
      } catch (NumberFormatException e) {
        return 0;
      }
    }
  }

  private static int getIntField(JsonValue o, String a, String b) {
    JsonValue u = o.get(a);
    if (u == null) {
      u = o.get(b);
    }
    if (u == null) {
      throw new IllegalArgumentException("Missing int field: " + a);
    }
    return u.asInt();
  }

  private static int getIntField(JsonValue o, String a) {
    JsonValue u = o.get(a);
    if (u == null) {
      throw new IllegalArgumentException("Missing int field: " + a);
    }
    return u.asInt();
  }

  @NotNull
  private static FlixelFrame buildFrame(
      @NotNull Texture texture,
      int x, int y, int w, int h,
      @org.jetbrains.annotations.Nullable String name) {
    TextureRegion region = new TextureRegion(texture, x, y, w, h);
    FlixelFrame f = new FlixelFrame(region);
    f.name = name;
    f.offsetX = 0;
    f.offsetY = 0;
    f.originalWidth = w;
    f.originalHeight = h;
    if (f.name == null) {
      f.name = "frame";
    }
    return f;
  }
}
