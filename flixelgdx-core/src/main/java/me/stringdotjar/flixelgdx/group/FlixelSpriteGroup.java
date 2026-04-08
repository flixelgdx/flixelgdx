/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.group;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.SnapshotArray;

import me.stringdotjar.flixelgdx.FlixelSprite;
import me.stringdotjar.flixelgdx.util.FlixelConstants;

import java.util.Comparator;
import java.util.Objects;
import java.util.Random;
import java.util.function.Predicate;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A special {@link FlixelSprite} that can be treated like a single sprite even when
 * made up of several member sprites. It implements {@link FlixelBasicGroupable}
 * for managing members while inheriting all sprite properties from {@link FlixelSprite}.
 * <p>
 * Because FlixelSpriteGroup extends {@link me.stringdotjar.flixelgdx.FlixelSprite}, groups can be nested
 * inside other groups, enabling complex hierarchical sprite compositions. Any property
 * change on the group (position, alpha, color, scale, rotation, flip) automatically
 * propagates to all members.
 * <p>
 * Sprites added to the group are automatically offset by the group's current position
 * and have their alpha multiplied by the group's alpha. When a sprite is removed, its
 * position offset is subtracted to restore local coordinates.
 * <p>
 * Rotation behavior is controlled by {@link RotationMode}:
 * <ul>
 *   <li>{@link RotationMode#INDIVIDUAL} (default): the rotation delta is applied
 *       to each sprite's own rotation; no positional changes occur.</li>
 *   <li>{@link RotationMode#WHEEL}: sprites are arranged radially around the center
 *       like a wheel, repositioned absolutely each frame in {@link #update(float)}.</li>
 *   <li>{@link RotationMode#ORBIT}: sprites orbit around the group origin as a rigid body;
 *       both position and individual rotation are adjusted by the delta.</li>
 * </ul>
 *
 * <p>{@link #remove} and {@link #detach} restore local coordinates and unlink the sprite; they do not call
 * {@link FlixelSprite#destroy()}. Use {@link me.stringdotjar.flixelgdx.FlixelBasic#kill()} / {@link me.stringdotjar.flixelgdx.FlixelBasic#revive()} or
 * {@link #recycle()} for reuse. {@link #clear()} unlinks all members without destroying them.
 * {@link #destroy()} on this group destroys every member (releases graphics) and resets group state.
 */
public class FlixelSpriteGroup extends FlixelSprite implements FlixelBasicGroupable<FlixelSprite> {

  /** The members belonging to this group. */
  protected final SnapshotArray<FlixelSprite> members;

  /** Maximum members allowed. When {@code 0}, the group can grow without limit. */
  protected int maxSize;

  /** Distance of each sprite from the center when using {@link RotationMode#WHEEL}. */
  private float rotationRadius;

  private static final Random RANDOM = new Random();

  /** Reusable rectangle for internal calculations that would otherwise allocate. */
  private final Rectangle tmpBoundsRect = new Rectangle();

  private RotationMode rotationMode = RotationMode.INDIVIDUAL;
  private boolean visible = true;
  private boolean antialiasing = false;
  private int facing = FlixelConstants.Graphics.FACING_RIGHT;

  /** Creates a sprite group with no member limit and default wheel radius {@code 100}. */
  public FlixelSpriteGroup() {
    this(0, 100f, 0f);
  }

  /**
   * Creates a sprite group with the given maximum size, rotation radius, and rotation.
   *
   * @param maxSize The maximum size of the group ({@code 0} = unlimited).
   * @param rotationRadius The radius used by {@link RotationMode#WHEEL}.
   * @param rotation The group's initial angle in degrees.
   */
  public FlixelSpriteGroup(int maxSize, float rotationRadius, float rotation) {
    super();
    this.maxSize = Math.max(0, maxSize);
    this.rotationRadius = rotationRadius;
    super.setAngle(rotation);
    members = new SnapshotArray<>(FlixelSprite[]::new);
  }

  @Override
  public void setX(float x) {
    float dx = x - getX();
    super.setX(x);
    if (rotationMode != RotationMode.WHEEL && dx != 0) {
      transformMembersX(dx);
    }
  }

  @Override
  public void setY(float y) {
    float dy = y - getY();
    super.setY(y);
    if (rotationMode != RotationMode.WHEEL && dy != 0) {
      transformMembersY(dy);
    }
  }

  @Override
  public void setPosition(float x, float y) {
    float dx = x - getX();
    float dy = y - getY();
    super.setPosition(x, y);
    if (rotationMode != RotationMode.WHEEL && (dx != 0 || dy != 0)) {
      transformMembersPosition(dx, dy);
    }
  }

  @Override
  public final FlixelSprite loadGraphic(Texture texture, int frameWidth, int frameHeight) {
    throw new UnsupportedOperationException("Loading a texture for a group is not supported. Use add() instead.");
  }

  @Override
  public final FlixelSprite makeGraphic(int width, int height, Color color) {
    throw new UnsupportedOperationException("Creating a graphic for a group is not supported. Use add() instead.");
  }

  /**
   * Sets the group's rotation in degrees. The behavior depends on the current {@link RotationMode}:
   * <ul>
   *   <li>{@link RotationMode#INDIVIDUAL}: the delta is applied to each sprite's
   *       own rotation.</li>
   *   <li>{@link RotationMode#WHEEL}: the value is stored and applied during the
   *       next {@link #update(float)} call.</li>
   *   <li>{@link RotationMode#ORBIT}: each sprite's position is rotated around the group
   *       origin by the delta, and its individual rotation is adjusted by the same amount.</li>
   * </ul>
   *
   * @param degrees the new rotation in degrees.
   */
  @Override
  public void setAngle(float degrees) {
    float delta = degrees - getAngle();
    super.setAngle(degrees);

    if (delta != 0) {
      switch (rotationMode) {
        case INDIVIDUAL -> transformMembersIndividualRotation(delta);
        case ORBIT -> orbitMembersAroundCenter(delta);
        case WHEEL -> { /* Wheel rotation is applied during update. */ }
      }
    }
  }

  public RotationMode getRotationMode() {
    return rotationMode;
  }

  public void setRotationMode(RotationMode rotationMode) {
    this.rotationMode = rotationMode;
  }

  public float getRotationRadius() {
    return rotationRadius;
  }

  public void setRotationRadius(float rotationRadius) {
    this.rotationRadius = rotationRadius;
  }

  /**
   * Sets the opacity of the group and all of its current members. Members added later via
   * {@link #add(FlixelSprite)} will have their alpha multiplied by this value rather than
   * overwritten.
   *
   * @param a Alpha between 0 (fully transparent) and 1 (fully opaque).
   */
  @Override
  public void setAlpha(float a) {
    super.setAlpha(a);
    FlixelSprite[] items = members.begin();
    for (int i = 0, n = members.size; i < n; i++) {
      FlixelSprite s = items[i];
      if (s != null) {
        s.setAlpha(a);
      }
    }
    members.end();
  }

  public float getAlpha() {
    return getColor().a;
  }

  /** Sets a color tint on the group and propagates it to all current members. */
  @Override
  public void setColor(Color tint) {
    super.setColor(tint);
    FlixelSprite[] items = members.begin();
    for (int i = 0, n = members.size; i < n; i++) {
      FlixelSprite s = items[i];
      if (s != null) {
        s.setColor(tint);
      }
    }
    members.end();
  }

  /** Sets a color tint on the group and propagates it to all current members. */
  @Override
  public void setColor(float r, float g, float b, float a) {
    super.setColor(r, g, b, a);
    FlixelSprite[] items = members.begin();
    for (int i = 0, n = members.size; i < n; i++) {
      FlixelSprite s = items[i];
      if (s != null) {
        s.setColor(r, g, b, a);
      }
    }
    members.end();
  }

  /** Sets a uniform scale on the group and propagates it to all current members. */
  @Override
  public void setScale(float scaleXY) {
    super.setScale(scaleXY);
    FlixelSprite[] items = members.begin();
    for (int i = 0, n = members.size; i < n; i++) {
      FlixelSprite s = items[i];
      if (s != null) {
        s.setScale(scaleXY);
      }
    }
    members.end();
  }

  /** Sets a non-uniform scale on the group and propagates it to all current members. */
  @Override
  public void setScale(float scaleX, float scaleY) {
    super.setScale(scaleX, scaleY);
    FlixelSprite[] items = members.begin();
    for (int i = 0, n = members.size; i < n; i++) {
      FlixelSprite s = items[i];
      if (s != null) {
        s.setScale(scaleX, scaleY);
      }
    }
    members.end();
  }

  /** Toggles the flip state on the X and/or Y axis for the group and all current members. */
  @Override
  public void flip(boolean x, boolean y) {
    super.flip(x, y);
    FlixelSprite[] items = members.begin();
    for (int i = 0, n = members.size; i < n; i++) {
      FlixelSprite s = items[i];
      if (s != null) {
        s.flip(x, y);
      }
    }
    members.end();
  }

  /**
   * Sets the X-axis flip state on the group and all members to the desired value.
   * Unlike {@link #flip(boolean, boolean)}, which toggles, this method ensures a
   * specific state.
   *
   * @param flipX {@code true} to flip horizontally, {@code false} to un-flip.
   */
  public void setFlipX(boolean flipX) {
    if (isFlipX() != flipX) {
      super.flip(true, false);
    }
    FlixelSprite[] items = members.begin();
    for (int i = 0, n = members.size; i < n; i++) {
      FlixelSprite s = items[i];
      if (s == null || s.isFlipX() == flipX) {
        continue;
      }
      s.flip(true, false);
    }
    members.end();
  }

  /**
   * Sets the Y-axis flip state on the group and all members to the desired value.
   * Unlike {@link #flip(boolean, boolean)}, which toggles, this method ensures a
   * specific state.
   *
   * @param flipY {@code true} to flip vertically, {@code false} to un-flip.
   */
  public void setFlipY(boolean flipY) {
    if (isFlipY() != flipY) {
      super.flip(false, true);
    }
    FlixelSprite[] items = members.begin();
    for (int i = 0, n = members.size; i < n; i++) {
      FlixelSprite s = items[i];
      if (s == null || s.isFlipY() == flipY) {
        continue;
      }
      s.flip(false, true);
    }
    members.end();
  }

  public boolean isVisible() {
    return visible;
  }

  public void setVisible(boolean visible) {
    this.visible = visible;
  }

  @Override
  public void setAntialiasing(boolean antialiasing) {
    this.antialiasing = antialiasing;
    FlixelSprite[] items = members.begin();
    for (int i = 0, n = members.size; i < n; i++) {
      FlixelSprite s = items[i];
      if (s != null) {
        s.setAntialiasing(antialiasing);
      }
    }
    members.end();
  }

  public boolean isAntialiasing() {
    return antialiasing;
  }

  @Override
  public int getFacing() {
    return facing;
  }

  @Override
  public void setFacing(int facing) {
    this.facing = facing;
    FlixelSprite[] items = members.begin();
    for (int i = 0, n = members.size; i < n; i++) {
      FlixelSprite s = items[i];
      if (s != null) {
        s.setFacing(facing);
      }
    }
    members.end();
  }

  /** Sets the rotation and scale pivot point on every current member. */
  @Override
  public void setOrigin(float originX, float originY) {
    super.setOrigin(originX, originY);
    FlixelSprite[] items = members.begin();
    for (int i = 0, n = members.size; i < n; i++) {
      FlixelSprite s = items[i];
      if (s != null) {
        s.setOrigin(originX, originY);
      }
    }
    members.end();
  }

  /** Centers the origin on every current member based on each member's own dimensions. */
  @Override
  public void setOriginCenter() {
    super.setOriginCenter();
    FlixelSprite[] items = members.begin();
    for (int i = 0, n = members.size; i < n; i++) {
      FlixelSprite s = items[i];
      if (s != null) {
        s.setOriginCenter();
      }
    }
    members.end();
  }

  /**
   * Adds a sprite to the group. The sprite's position is automatically offset by the
   * group's current position, and its alpha is multiplied by the group's alpha. If the
   * group has a {@link #maxSize} and is already at capacity, the sprite is not added.
   *
   * @param sprite The sprite to add.
   */
  @Override
  public void add(@NotNull FlixelSprite sprite) {
    Objects.requireNonNull(sprite);
    if (maxSize > 0 && members.size >= maxSize) {
      return;
    }
    preAdd(sprite);
    members.add(sprite);
  }

  /**
   * Adds a sprite to the group and returns it, allowing for method chaining.
   *
   * @param sprite The sprite to add.
   * @return The added sprite.
   */
  public FlixelSprite addAndReturn(@NotNull FlixelSprite sprite) {
    Objects.requireNonNull(sprite);
    add(sprite);
    return sprite;
  }

  /**
   * Inserts a sprite at the given index, offset by the group's current position. The
   * index is clamped to the valid range {@code [0, length]}.
   *
   * @param index The index to insert the sprite at.
   * @param sprite The sprite to insert.
   */
  public void insert(int index, FlixelSprite sprite) {
    if (sprite == null) {
      return;
    }
    if (maxSize > 0 && members.size >= maxSize) {
      return;
    }
    preAdd(sprite);
    index = MathUtils.clamp(index, 0, members.size);
    members.insert(index, sprite);
  }

  /**
   * Removes the sprite from the group and restores local coordinates. Does not call {@link FlixelSprite#destroy()}.
   */
  @Override
  public void remove(FlixelSprite sprite) {
    if (sprite == null || !members.removeValue(sprite, true)) {
      return;
    }
    restoreLocalCoordinates(sprite);
  }

  /** Same as {@link #remove(FlixelSprite)}: restores local coordinates and unlinks the sprite. */
  @Override
  public void detach(FlixelSprite sprite) {
    if (sprite == null || !members.removeValue(sprite, true)) {
      return;
    }
    restoreLocalCoordinates(sprite);
  }

  /**
   * Revives the first dead member, or creates a new sprite, applies {@link #preAdd}, and adds it when under
   * {@link #maxSize}. When at capacity and no dead slot exists, returns {@code null}.
   */
  @Nullable
  public FlixelSprite recycle() {
    FlixelSprite dead = getFirstDead();
    if (dead != null) {
      dead.revive();
      dead.active = true;
      dead.visible = true;
      return dead;
    }
    if (maxSize > 0 && members.size >= maxSize) {
      return null;
    }
    FlixelSprite fresh = new FlixelSprite();
    fresh.revive();
    fresh.active = true;
    fresh.visible = true;
    preAdd(fresh);
    members.add(fresh);
    return fresh;
  }

  /**
   * Replaces an existing member with a new sprite. The new sprite is offset by the group's
   * current position. If {@code oldSprite} is not found, {@code newSprite} is simply added
   * to the end of the group instead.
   *
   * @param oldSprite The member to replace.
   * @param newSprite The replacement sprite.
   * @return The replacement sprite.
   */
  public FlixelSprite replace(FlixelSprite oldSprite, FlixelSprite newSprite) {
    if (oldSprite == null || newSprite == null) {
      return newSprite;
    }
    int idx = members.indexOf(oldSprite, true);
    if (idx < 0) {
      add(newSprite);
      return newSprite;
    }
    restoreLocalCoordinates(oldSprite);
    preAdd(newSprite);
    members.set(idx, newSprite);
    return newSprite;
  }

  /** Unlinks every member and restores local coordinates. Does not call {@link FlixelSprite#destroy()}. */
  @Override
  public void clear() {
    FlixelSprite[] items = members.begin();
    try {
      for (int i = 0, n = members.size; i < n; i++) {
        FlixelSprite s = items[i];
        if (s != null) {
          restoreLocalCoordinates(s);
        }
      }
    } finally {
      members.end();
    }
    members.clear();
  }

  /**
   * Returns the member at the given index, or {@code null} if the index is out of bounds.
   *
   * @param index The index of the member to get.
   * @return The member at the given index, or {@code null} if the index is out of bounds.
   */
  public FlixelSprite get(int index) {
    if (index < 0 || index >= members.size) {
      return null;
    }
    return members.get(index);
  }

  public int getLength() {
    return members.size;
  }

  /** Returns the number of non-null members, which may differ from {@link #getLength()}. */
  public int countMembers() {
    int count = 0;
    for (int i = 0, n = members.size; i < n; i++) {
      if (members.get(i) != null) {
        count++;
      }
    }
    return count;
  }

  public boolean isEmpty() {
    return members.size == 0;
  }

  @Override
  public int getMaxSize() {
    return maxSize;
  }

  @Override
  public void setMaxSize(int maxSize) {
    this.maxSize = Math.max(0, maxSize);
  }

  @Override
  @NotNull
  public SnapshotArray<FlixelSprite> getMembers() {
    return members;
  }

  /** Returns a random member, or {@code null} if the group is empty. */
  public FlixelSprite getRandom() {
    return getRandom(0, members.size);
  }

  /**
   * Returns a random member from the range [{@code startIndex}, {@code startIndex + length}).
   *
   * @param startIndex The first index (inclusive).
   * @param length The number of elements to consider. If {@code <= 0}, the entire group is used.
   * @return A random member from the range, or {@code null} if the range is empty.
   */
  public FlixelSprite getRandom(int startIndex, int length) {
    if (members.size == 0) {
      return null;
    }
    startIndex = MathUtils.clamp(startIndex, 0, members.size - 1);
    if (length <= 0) {
      length = members.size;
    }
    int end = Math.min(startIndex + length, members.size);
    int span = end - startIndex;
    if (span <= 0) {
      return null;
    }
    return members.get(startIndex + RANDOM.nextInt(span));
  }

  /**
   * Sorts the members of the group using the given comparator.
   *
   * @param comparator The comparator to use to sort the members.
   */
  public void sort(@NotNull Comparator<FlixelSprite> comparator) {
    Objects.requireNonNull(comparator);
    members.sort(comparator);
  }

  /**
   * Returns the first member that satisfies the predicate, or {@code null} if none match.
   *
   * @param predicate The predicate to test the members against.
   * @return The first member that satisfies the predicate, or {@code null} if none match.
   */
  public FlixelSprite getFirstMatching(Predicate<FlixelSprite> predicate) {
    if (predicate == null) {
      return null;
    }
    FlixelSprite[] items = members.begin();
    for (int i = 0, n = members.size; i < n; i++) {
      FlixelSprite s = items[i];
      if (s != null && predicate.test(s)) {
        return s;
      }
    }
    members.end();
    return null;
  }

  /**
   * Returns the index of the given sprite in the group. If the sprite is not a member of the group, returns {@code -1}.
   *
   * @param sprite The sprite to get the index of.
   * @return The index of the given sprite in the group, or {@code -1} if the sprite is not a member of the group.
   */
  public int indexOf(FlixelSprite sprite) {
    return members.indexOf(sprite, true);
  }

  /**
   * Returns {@code true} if the group contains the given sprite, {@code false} otherwise.
   *
   * @param sprite The sprite to check.
   * @return {@code true} if the group contains the given sprite, {@code false} otherwise.
   */
  public boolean contains(FlixelSprite sprite) {
    return members.indexOf(sprite, true) >= 0;
  }

  /**
   * Moves a member to the end of the draw list so that it renders on top of all other
   * members. Has no effect if the sprite is not a member of this group.
   *
   * @param sprite The sprite to bring to the front of the group's list.
   */
  public void bringToFront(FlixelSprite sprite) {
    if (members.removeValue(sprite, true)) {
      members.add(sprite);
    }
  }

  /**
   * Moves a member to the beginning of the draw list so that it renders behind all other
   * members. Has no effect if the sprite is not a member of this group.
   *
   * @param sprite The sprite to send to the back of the group's list.
   */
  public void sendToBack(FlixelSprite sprite) {
    if (members.removeValue(sprite, true)) {
      members.insert(0, sprite);
    }
  }

  /**
   * Swaps the draw order of two members by their indices. Out-of-bounds indices are
   * silently ignored.
   *
   * @param index1 The index of the first sprite to swap.
   * @param index2 The index of the second sprite to swap.
   */
  public void swapMembers(int index1, int index2) {
    if (index1 < 0 || index1 >= members.size || index2 < 0 || index2 >= members.size) {
      return;
    }
    members.swap(index1, index2);
  }

  /**
   * Computes the axis-aligned bounding rectangle that encloses all members, taking each
   * member's position, size, and scale into account.
   *
   * @param out An optional output rectangle. If {@code null}, a new one is created.
   * @return The bounding rectangle.
   */
  public Rectangle getBounds(Rectangle out) {
    if (out == null) {
      out = new Rectangle();
    }
    if (members.size == 0) {
      out.set(getX(), getY(), 0, 0);
      return out;
    }

    float minX = Float.MAX_VALUE;
    float minY = Float.MAX_VALUE;
    float maxX = -Float.MAX_VALUE;
    float maxY = -Float.MAX_VALUE;

    for (int i = 0, n = members.size; i < n; i++) {
      FlixelSprite s = members.get(i);
      if (s == null) {
        continue;
      }
      float sx = s.getX();
      float sy = s.getY();
      float sw = s.getWidth() * Math.abs(s.getScaleX());
      float sh = s.getHeight() * Math.abs(s.getScaleY());

      if (sx < minX) minX = sx;
      if (sy < minY) minY = sy;
      if (sx + sw > maxX) maxX = sx + sw;
      if (sy + sh > maxY) maxY = sy + sh;
    }

    out.set(minX, minY, maxX - minX, maxY - minY);
    return out;
  }

  /**
   * Returns the center point of the bounding rectangle that encompasses all members.
   *
   * @param out An optional output vector. If {@code null}, a new one is created.
   * @return The midpoint of the group's bounds.
   */
  public Vector2 getMidpoint(Vector2 out) {
    if (out == null) {
      out = new Vector2();
    }
    Rectangle bounds = getBounds(tmpBoundsRect);
    out.set(bounds.x + bounds.width / 2f, bounds.y + bounds.height / 2f);
    return out;
  }

  @Override
  public void update(float delta) {
    super.update(delta);

    FlixelSprite[] items = members.begin();
    for (int i = 0, n = members.size; i < n; i++) {
      FlixelSprite member = items[i];
      if (member != null) {
        member.update(delta);
      }
    }
    members.end();

    if (rotationMode == RotationMode.WHEEL) {
      applyWheelRotation();
    }
  }

  /**
   * Draws all members in insertion order. The group itself does not render its own graphic;
   * only its members are drawn. Nothing is rendered when {@link #isVisible()} is {@code false}.
   */
  @Override
  public void draw(Batch batch) {
    if (!visible || !isOnDrawCamera()) {
      return;
    }

    FlixelSprite[] items = members.begin();
    for (int i = 0, n = members.size; i < n; i++) {
      FlixelSprite member = items[i];
      if (member != null) {
        member.draw(batch);
      }
    }
    members.end();
  }

  @Override
  public void destroy() {
    FlixelSprite[] items = members.begin();
    try {
      for (int i = 0, n = members.size; i < n; i++) {
        FlixelSprite s = items[i];
        if (s != null) {
          restoreLocalCoordinates(s);
          s.destroy();
        }
      }
    } finally {
      members.end();
    }
    members.clear();
    super.destroy();
    rotationMode = RotationMode.INDIVIDUAL;
    rotationRadius = 100f;
    visible = true;
  }

  private void restoreLocalCoordinates(FlixelSprite sprite) {
    sprite.setX(sprite.getX() - getX());
    sprite.setY(sprite.getY() - getY());
  }

  /**
   * Offsets a sprite by the group's current position and multiplies its alpha by the
   * group's alpha before it is inserted into the members list.
   */
  private void preAdd(FlixelSprite sprite) {
    sprite.setX(sprite.getX() + getX());
    sprite.setY(sprite.getY() + getY());
    sprite.setAlpha(sprite.getColor().a * getColor().a);
    sprite.setAntialiasing(antialiasing);
    sprite.setFacing(facing);
  }

  private void transformMembersX(float dx) {
    FlixelSprite[] items = members.begin();
    for (int i = 0, n = members.size; i < n; i++) {
      FlixelSprite s = items[i];
      if (s != null) {
        s.setX(s.getX() + dx);
      }
    }
    members.end();
  }

  private void transformMembersY(float dy) {
    FlixelSprite[] items = members.begin();
    for (int i = 0, n = members.size; i < n; i++) {
      FlixelSprite s = items[i];
      if (s != null) {
        s.setY(s.getY() + dy);
      }
    }
    members.end();
  }

  private void transformMembersPosition(float dx, float dy) {
    FlixelSprite[] items = members.begin();
    for (int i = 0, n = members.size; i < n; i++) {
      FlixelSprite s = items[i];
      if (s != null) {
        s.setPosition(s.getX() + dx, s.getY() + dy);
      }
    }
    members.end();
  }

  /**
   * Applies the rotation delta to each sprite's own rotation without changing
   * any positions. Used by {@link RotationMode#INDIVIDUAL}.
   */
  private void transformMembersIndividualRotation(float delta) {
    FlixelSprite[] items = members.begin();
    for (int i = 0, n = members.size; i < n; i++) {
      FlixelSprite s = items[i];
      if (s != null) {
        s.setAngle(s.getAngle() + delta);
      }
    }
    members.end();
  }

  /**
   * Rotates every member's position around the group origin by {@code angleDelta} degrees
   * and adjusts each sprite's own rotation by the same amount. Used by
   * {@link RotationMode#ORBIT}.
   */
  private void orbitMembersAroundCenter(float angleDelta) {
    float cos = MathUtils.cosDeg(angleDelta);
    float sin = MathUtils.sinDeg(angleDelta);

    for (int i = 0, n = members.size; i < n; i++) {
      FlixelSprite s = members.get(i);
      if (s == null) {
        continue;
      }

      float localX = s.getX() - getX();
      float localY = s.getY() - getY();
      float rotatedX = localX * cos - localY * sin;
      float rotatedY = localX * sin + localY * cos;
      s.setPosition(getX() + rotatedX, getY() + rotatedY);
      s.setAngle(s.getAngle() + angleDelta);
    }
  }

  /**
   * Positions and rotates each sprite radially around the group center. Called every
   * frame from {@link #update(float)} when using {@link RotationMode#WHEEL}.
   */
  private void applyWheelRotation() {
    int n = members.size;
    if (n == 0) {
      return;
    }

    float angleStep = 360f / n;
    for (int i = 0; i < n; i++) {
      FlixelSprite s = members.get(i);
      if (s == null) {
        continue;
      }

      float angleDeg = getAngle() + angleStep * i;
      float px = getX() + rotationRadius * MathUtils.cosDeg(angleDeg);
      float py = getY() + rotationRadius * MathUtils.sinDeg(angleDeg);
      s.setPosition(px, py);
      s.setAngle(angleDeg);
    }
  }

  /** Controls how a {@link FlixelSpriteGroup}'s rotation affects its members. */
  public enum RotationMode {

    /**
     * Rotation delta is applied to each sprite's individual rotation.
     * No positional changes occur.
     */
    INDIVIDUAL,

    /**
     * Sprites are arranged in a radial pattern around the group center. Each sprite is
     * positioned at {@link FlixelSpriteGroup#rotationRadius} from the center, spaced
     * evenly around 360 degrees. Positions and rotations are set absolutely each frame
     * in {@link FlixelSpriteGroup#update(float)}.
     * <p>
     * Individual sprite rotations cannot be changed independently in this mode.
     */
    WHEEL,

    /**
     * All sprites orbit around the group origin as a rigid body. When the rotation
     * changes, each sprite's position is rotated around the center by the delta, and
     * its individual rotation is adjusted by the same amount.
     */
    ORBIT
  }
}
