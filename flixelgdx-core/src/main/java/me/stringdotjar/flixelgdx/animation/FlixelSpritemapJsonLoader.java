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
import com.badlogic.gdx.utils.ObjectMap;

import me.stringdotjar.flixelgdx.Flixel;
import me.stringdotjar.flixelgdx.graphics.FlixelFrame;
import me.stringdotjar.flixelgdx.graphics.FlixelGraphic;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Helper class for spritemap-style JSON assets. Handles two separate shapes:
 *
 * <ol>
 *   <li><strong>Simple animation index</strong> - a top-level {@code "animations": { name: { "frames":
 *   [0, 1, 2] } }} block (with optional {@code framerate}). Frames may be declared as a JSON array of
 *   {@code [x, y, w, h, ...]} rows, as an object map of frame name to {@code {"frame": {"x", "y", "w",
 *   "h"}}}, or as an Adobe "TexturePacker" {@code ATLAS.SPRITES} block.</li>
 *   <li><strong>Adobe Animate texture atlas</strong> - a top-level {@code "AN"} / {@code "SD"} / {@code
 *   "MD"} structure as produced by Adobe Animate's texture-atlas export. These are routed to
 *   {@link FlixelAnimateRigLoader}, which requires the owning sprite to be a
 *   {@link FlixelAnimateSprite} so the multi-part rig can be attached.</li>
 * </ol>
 *
 * <p>Game code does not usually call this helper directly. A {@link FlixelAnimateSprite} accepts a
 * BTA/Animate pair through {@link FlixelAnimateSprite#loadSpritemapAndAnimation}, and a plain
 * {@link me.stringdotjar.flixelgdx.FlixelSprite} accepts a simple-format pair through
 * {@link FlixelAnimationController#loadSpritemapFromJson}.
 */
public final class FlixelSpritemapJsonLoader {

  private FlixelSpritemapJsonLoader() {}

  /**
   * Resolves an asset path the same way the rest of FlixelGDX does: first {@code Gdx.files.internal},
   * then {@code Gdx.files.classpath}. The first hit that exists and is not a directory is returned.
   *
   * @param path The path to resolve. Must not be {@code null} or empty.
   * @return The resolved {@link FileHandle}. Never {@code null}.
   * @throws IllegalArgumentException If the path resolves to nothing readable.
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
   * Reads a text file as UTF-8 and strips a leading byte-order mark if present. Use for XML and JSON so
   * the path does not fall back to the platform default charset.
   *
   * @param f The file handle to read from. Must not be {@code null}.
   * @return The text content; never {@code null} (an empty file yields the empty string).
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
   * Parses a spritemap JSON's {@code ATLAS.SPRITES} block (the Adobe Animate "BTA" shape) into a list of
   * {@link FlixelFrame} instances, and fills {@code nameToIndexOut} with a lookup from sprite name to
   * atlas index.
   *
   * <p>This method is shared between the simple and Animate loaders so that only one place knows how
   * to unpack Adobe's {@code name}/{@code x}/{@code y}/{@code w}/{@code h} fields.
   *
   * @param spritemapRoot The root JSON value of the spritemap file. Must not be {@code null}.
   * @param texture The backing texture to wrap each region around. Must not be {@code null}.
   * @param nameToIndexOut An output map. Cleared on entry and filled with sprite name to atlas index
   *   for each entry that has a non-empty {@code name}. Must not be {@code null}.
   * @return The list of built {@link FlixelFrame} regions, in declaration order.
   * @throws IllegalArgumentException If {@code ATLAS.SPRITES} is missing, not an array, or contains zero entries.
   */
  @NotNull
  public static Array<FlixelFrame> parseAtlasSprites(
      @NotNull JsonValue spritemapRoot,
      @NotNull Texture texture,
      @NotNull ObjectMap<String, Integer> nameToIndexOut) {
    nameToIndexOut.clear();
    JsonValue atlas = spritemapRoot.get("ATLAS");
    JsonValue sprites = (atlas != null) ? atlas.get("SPRITES") : null;
    if (sprites == null || !sprites.isArray()) {
      throw new IllegalArgumentException("Spritemap JSON is missing an \"ATLAS.SPRITES\" array.");
    }
    Array<FlixelFrame> out = new Array<>();
    for (JsonValue el = sprites.child; el != null; el = el.next) {
      JsonValue sp = el.get("SPRITE");
      if (sp == null) {
        sp = el;
      }
      if (sp == null || !sp.isObject()) {
        continue;
      }
      JsonValue nameNode = sp.get("name");
      String name = (nameNode != null) ? nameNode.asString() : "";
      int x = sp.getInt("x");
      int y = sp.getInt("y");
      int w = sp.getInt("w");
      int h = sp.getInt("h");
      int idx = out.size;
      out.add(buildFrame(texture, x, y, w, h, name));
      if (name != null && !name.isEmpty()) {
        nameToIndexOut.put(name, idx);
      }
    }
    if (out.size == 0) {
      throw new IllegalArgumentException("ATLAS.SPRITES contained no valid SPRITE entries.");
    }
    return out;
  }

  /**
   * Top-level entry point used by {@link FlixelAnimationController#loadSpritemapFromJson}. Reads both
   * files, figures out which flavor the animation JSON is, and dispatches to the appropriate handler.
   *
   * @param controller The animation controller whose owning sprite should receive the loaded clips.
   * Must not be {@code null}.
   * @param textureKey The asset key of the backing PNG. Must not be {@code null}.
   * @param spritemapJsonPath The path to the spritemap JSON. Must not be {@code null}.
   * @param animationJsonPath The path to the animation JSON. Must not be {@code null}.
   * @throws IllegalArgumentException If either file is malformed, or if an Adobe Animate JSON is being
   * loaded onto a sprite that is not a {@link FlixelAnimateSprite}.
   */
  public static void load(
      @NotNull FlixelAnimationController controller,
      @NotNull String textureKey,
      @NotNull String spritemapJsonPath,
      @NotNull String animationJsonPath) {
    String animText = readUtf8Text(resolveAssetPath(animationJsonPath));
    JsonValue animRoot = new JsonReader().parse(animText);

    // Adobe Animate exports always expose an AN block. Route those through the rig loader, which also
    // handles the spritemap parsing itself (it needs the name-to-index map that stays local to the rig).
    if (animRoot.get("AN") != null) {
      if (controller.getOwner() instanceof FlixelAnimateSprite animateSprite) {
        FlixelAnimateRigLoader.load(animateSprite, controller, textureKey, spritemapJsonPath, animationJsonPath);
        return;
      }
      throw new IllegalArgumentException(
        "Adobe Animate texture-atlas JSON (with top-level \"AN\") can only be loaded onto a "
          + "FlixelAnimateSprite, because it requires multi-part compositing. The current owner is "
          + controller.getOwner().getClass().getName() + ".");
    }

    // Simple-format animation JSON. Parse the spritemap, apply it on the sprite, then register clips
    // declared in the "animations" block.
    String smText = readUtf8Text(resolveAssetPath(spritemapJsonPath));
    JsonValue spritemapRoot = new JsonReader().parse(smText);

    FlixelGraphic g = Flixel.ensureAssets().obtainWrapper(textureKey, FlixelGraphic.class);
    Texture texture;
    try {
      texture = g.requireTexture();
    } catch (IllegalStateException notLoaded) {
      texture = g.loadNow();
    }

    Array<FlixelFrame> frames = buildSimpleFrames(spritemapRoot, texture);
    controller.getOwner().applySparrowAtlas(g, frames);

    if (animRoot.get("animations") == null) {
      throw new IllegalArgumentException(
        "Animation JSON has no \"animations\" object and no \"AN\" block. Unrecognized format.");
    }
    loadSimpleAnimationsJson(controller, animRoot);
  }

  /**
   * Registers clips from a simple {@code "animations"} JSON onto {@code controller}. Each entry must
   * have a {@code "frames"} array of integer indices into the atlas; optional per-clip {@code framerate}
   * and {@code loop} fields are honoured.
   */
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
        throw new IllegalArgumentException(
          "Animation \"" + name + "\" needs a \"frames\" array of indices.");
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
   * Builds an atlas frame list for the simple-format spritemap JSON (either an {@code ATLAS.SPRITES}
   * block, a top-level {@code frames} array, or a top-level {@code frames} object keyed by name).
   */
  @NotNull
  private static Array<FlixelFrame> buildSimpleFrames(@NotNull JsonValue root, @NotNull Texture texture) {
    JsonValue atlas = root.get("ATLAS");
    if (atlas != null) {
      ObjectMap<String, Integer> ignore = new ObjectMap<>();
      return parseAtlasSprites(root, texture, ignore);
    }
    JsonValue framesNode = root.get("frames");
    if (framesNode == null) {
      throw new IllegalArgumentException(
        "Spritemap JSON must contain \"ATLAS.SPRITES\" or a top-level \"frames\" field.");
    }
    Array<FlixelFrame> out = new Array<>();
    if (framesNode.isArray()) {
      int index = 0;
      for (JsonValue row = framesNode.child; row != null; row = row.next) {
        if (!row.isArray() || row.size < 4) {
          throw new IllegalArgumentException(
            "Each spritemap frame row must be an array with at least 4 numbers.");
        }
        int x = row.get(0).asInt();
        int y = row.get(1).asInt();
        int w = row.get(2).asInt();
        int h = row.get(3).asInt();
        out.add(buildFrame(texture, x, y, w, h, "frame" + index));
        index++;
      }
      return out;
    }
    if (framesNode.isObject()) {
      for (JsonValue v = framesNode.child; v != null; v = v.next) {
        String fname = v.name;
        if (fname == null || !v.isObject()) {
          continue;
        }
        JsonValue fr = v.get("frame");
        if (fr == null) {
          fr = v;
        }
        int x = getIntField(fr, "x");
        int y = getIntField(fr, "y");
        int w = getIntField(fr, "w", "width");
        int h = getIntField(fr, "h", "height");
        out.add(buildFrame(texture, x, y, w, h, fname));
      }
      return out;
    }
    throw new IllegalArgumentException("Spritemap \"frames\" must be a JSON array or object.");
  }

  private static float readFloat(@NotNull JsonValue o, @NotNull String key, float dflt) {
    JsonValue v = o.get(key);
    return (v == null) ? dflt : v.asFloat();
  }

  private static boolean readBoolean(@NotNull JsonValue o, @NotNull String key, boolean dflt) {
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

  private static int getIntField(@NotNull JsonValue o, @NotNull String a, @NotNull String b) {
    JsonValue u = o.get(a);
    if (u == null) {
      u = o.get(b);
    }
    if (u == null) {
      throw new IllegalArgumentException("Missing int field: " + a);
    }
    return u.asInt();
  }

  private static int getIntField(@NotNull JsonValue o, @NotNull String a) {
    JsonValue u = o.get(a);
    if (u == null) {
      throw new IllegalArgumentException("Missing int field: " + a);
    }
    return u.asInt();
  }

  @NotNull
  private static FlixelFrame buildFrame(
      @NotNull Texture texture, int x, int y, int w, int h, @Nullable String name) {
    TextureRegion region = new TextureRegion(texture, x, y, w, h);
    FlixelFrame f = new FlixelFrame(region);
    f.name = (name != null && !name.isEmpty()) ? name : "frame";
    f.offsetX = 0;
    f.offsetY = 0;
    f.originalWidth = w;
    f.originalHeight = h;
    return f;
  }
}
