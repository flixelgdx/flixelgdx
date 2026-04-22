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
import me.stringdotjar.flixelgdx.FlixelSprite;
import me.stringdotjar.flixelgdx.graphics.FlixelFrame;
import me.stringdotjar.flixelgdx.graphics.FlixelGraphic;

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
   * Reads spritemap and animation JSON, applies atlas frames, then registers one clip per animation.
   * 
   * <p>Spritemap may list frames as a JSON array of numeric rows {@code [x, y, w, h, ...]} (first four
   * values are the texture rect) or as an object map of frame name to
   * {@code { "frame": { "x", "y", "w", "h" } } } (names become {@link FlixelFrame#name}).
   * 
   * <p>Animation file shape should be a top-level object {@code "animations": { "name": { "frames": [0, 1, 2] } } }.
   * Optional {@code "framerate"} as a number on the root or on each animation object sets FPS
   * (default 24). Frame index arrays reference the ordered list from the spritemap.
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
    String smText = resolveAssetPath(spritemapJsonPath).readString("UTF-8");
    String anText = resolveAssetPath(animationJsonPath).readString("UTF-8");
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
    JsonValue framesNode = root.get("frames");
    if (framesNode == null) {
      throw new IllegalArgumentException("Spritemap JSON must contain a top-level \"frames\" field.");
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
