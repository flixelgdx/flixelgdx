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
package org.flixelgdx.debug;

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelSprite;
import org.flixelgdx.FlixelState;
import org.flixelgdx.asset.FlixelAsset;
import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.debug.remote.FlixelDebugMessageReader;
import org.flixelgdx.debug.remote.FlixelDebugMessageWriter;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.graphics.FlixelGraphic;
import org.flixelgdx.graphics.FlixelTexture;
import org.flixelgdx.group.FlixelGroupable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The texture panel of the remote debugger: every texture the game has loaded, how big it is, how many sprites use
 * it, and a small picture of it.
 *
 * <p>Think of the inventory of a warehouse. The list tells you what is on the shelves and how much space each box
 * takes, a "duplicate" tag marks two boxes that hold the same item (wasted space), and a thumbnail is a photo of
 * one box that you ask for only when you want to look inside.
 *
 * <h2>What is listed</h2>
 *
 * <p>The panel lists the textures of every loaded {@link FlixelGraphic} that the asset manager tracks
 * ({@link org.flixelgdx.Flixel#assets}): files loaded by path and graphics made in code, such as {@code makeGraphic}.
 * A texture created by hand with the graphics manager and not wrapped in a graphic, or one that belongs to the
 * framework itself (the font atlas, render targets), is not listed. Each row has:
 * <ul>
 *   <li>a stable id from {@link FlixelDebugManager#ids},</li>
 *   <li>the name (the asset path, or {@code flixel://synthetic/N} for graphics made in code),</li>
 *   <li>the width and height in pixels, the format name, and the GPU memory in bytes,</li>
 *   <li>the number of <em>users</em>: how many existing sprites in the current state (and its sub states,
 *       including sprites inside groups) are showing this texture right now,</li>
 *   <li>a <em>duplicate</em> flag: another listed texture was loaded from the same file path.</li>
 * </ul>
 *
 * <p>The asset manager already shares one texture per path, so duplicates are rare. They show up when the same file
 * is loaded under a second handle, for example through a custom loader.
 *
 * <h2>Cost</h2>
 *
 * <p>The list is rebuilt about once a second while the panel is enabled, reusing its arrays, and the panel only
 * bumps its revision (so the app receives it again) when something in the list changed. Counting users walks the
 * sprites of the current state once per rebuild.
 *
 * <p>Thumbnails are requested by id with {@code REQUEST_THUMBNAIL} and arrive later through
 * {@link #writePendingPayload(FlixelDebugMessageWriter)}; see {@link FlixelDebugThumbnails}.
 *
 * <p>Example:
 *
 * <pre>{@code
 * Flixel.debug.textures.setEnabled(false); // Stop sampling when the tab is not open.
 * int count = Flixel.debug.textures.getTextureCount();
 * long bytes = Flixel.debug.textures.getTotalBytes();
 * }</pre>
 *
 * @see FlixelDebugProtocol#KIND_TEXTURES
 */
public class FlixelDebugTexturePanel extends FlixelDebugPanelBase {

  /** Seconds between rebuilds of the list while the panel is enabled. */
  public static final float REFRESH_INTERVAL = 1f;

  private static final int MAX_DEPTH = 64;

  private long totalBytes;
  private long signature;

  private float timer = REFRESH_INTERVAL;

  private final FlixelArray<FlixelAsset<?>> assetScratch = new FlixelArray<>();
  private final FlixelArray<Row> rows = new FlixelArray<>();
  private final FlixelArray<FlixelTexture> previous = new FlixelArray<>();
  private final FlixelDebugThumbnails thumbnails = new FlixelDebugThumbnails();
  private final FlixelDebugObjectIds ids;

  private int count;
  private int duplicates;
  private int lastUserRow;

  /**
   * Creates the texture panel.
   *
   * @param ids The registry that gives textures their ids.
   */
  public FlixelDebugTexturePanel(@NotNull FlixelDebugObjectIds ids) {
    super("textures", "Textures", FlixelDebugProtocol.KIND_TEXTURES);
    this.ids = ids;
  }

  @Override
  public void update(float elapsed) {
    timer += elapsed;
    if (timer >= REFRESH_INTERVAL) {
      timer = 0f;
      refresh();
    }
  }

  /**
   * Rebuilds the list right now instead of waiting for the next refresh.
   *
   * <p>Bumps the revision when anything in the list changed.
   */
  public void refresh() {
    savePrevious();
    collect();
    markDuplicates();
    countUsers();
    releaseRemoved();
    long next = computeSignature();
    if (next != signature) {
      signature = next;
      markChanged();
    }
  }

  /**
   * Writes the list as a {@code LIST} payload: the entries, then the number of textures, their total GPU bytes, and
   * how many are duplicates.
   *
   * @param writer The writer positioned after the panel index.
   */
  @Override
  public void write(@NotNull FlixelDebugMessageWriter writer) {
    writer.writeU8(FlixelDebugProtocol.TEXTURES_LIST);
    int n = Math.min(count, FlixelDebugProtocol.MAX_ARRAY_COUNT);
    writer.beginArray();
    for (int i = 0; i < n; i++) {
      Row row = rows.get(i);
      writer.writeI32(row.id)
          .writeString(row.name)
          .writeI32(row.width)
          .writeI32(row.height)
          .writeString(row.format)
          .writeI64(row.bytes)
          .writeI32(row.users)
          .writeBool(row.duplicate);
    }
    writer.endArray(n);
    writer.writeI32(count).writeI64(totalBytes).writeI32(duplicates);
  }

  @Override
  public boolean hasPendingPayload() {
    return thumbnails.hasPending();
  }

  /**
   * Writes the oldest finished thumbnail as a {@code THUMBNAIL} payload.
   *
   * @param writer The writer positioned after the panel index.
   */
  @Override
  public void writePendingPayload(@NotNull FlixelDebugMessageWriter writer) {
    thumbnails.write(writer, FlixelDebugProtocol.TEXTURES_THUMBNAIL);
  }

  @Override
  public void handleAction(int action, @NotNull FlixelDebugMessageReader reader) {
    if (action == FlixelDebugProtocol.TEXTURES_REQUEST_THUMBNAIL) {
      requestThumbnail(reader.readI32());
    }
  }

  @Override
  public void reset() {
    for (int i = 0; i < count; i++) {
      Row row = rows.get(i);
      if (row.texture != null) {
        ids.release(row.texture);
      }
      row.clear();
    }
    count = 0;
    totalBytes = 0L;
    duplicates = 0;
    signature = 0L;
    timer = REFRESH_INTERVAL;
    thumbnails.clear();
    markChanged();
  }

  /**
   * Starts making a thumbnail of a listed texture.
   *
   * @param id A texture id from the list.
   * @return {@code true} when a read was started. It is {@code false} for an unknown id or when the graphics
   *     backend cannot read textures.
   */
  public boolean requestThumbnail(int id) {
    for (int i = 0; i < count; i++) {
      Row row = rows.get(i);
      if (row.id == id && row.texture != null) {
        return thumbnails.request(id, row.texture, 0, 0, row.width, row.height);
      }
    }
    return false;
  }

  private void savePrevious() {
    previous.clear();
    for (int i = 0; i < count; i++) {
      FlixelTexture texture = rows.get(i).texture;
      if (texture != null) {
        previous.add(texture);
      }
    }
  }

  private void collect() {
    count = 0;
    totalBytes = 0L;
    Flixel.assets.getAssets(assetScratch);
    for (int i = 0, n = assetScratch.getSize(); i < n; i++) {
      if (!(assetScratch.get(i) instanceof FlixelGraphic graphic) || !graphic.isLoaded()) {
        continue;
      }
      FlixelTexture texture;
      try {
        texture = graphic.getTexture();
      } catch (RuntimeException e) {
        continue; // Not a texture after all, or it was unloaded a moment ago.
      }
      if (indexOfTexture(texture) >= 0) {
        continue; // Two handles can share one texture; list it once.
      }
      Row row;
      if (count < rows.getSize()) {
        row = rows.get(count);
      } else {
        row = new Row();
        rows.add(row);
      }
      count++;
      row.texture = texture;
      row.id = ids.idOf(texture);
      row.name = graphic.getPath();
      row.resolved = graphic.isOwned() ? null : graphic.getResolvedPath();
      row.width = texture.getWidth();
      row.height = texture.getHeight();
      row.format = texture.getFormat();
      row.bytes = texture.getByteSize();
      row.users = 0;
      row.duplicate = false;
      totalBytes += row.bytes;
    }
    assetScratch.clear();
  }

  private void markDuplicates() {
    duplicates = 0;
    for (int i = 0; i < count; i++) {
      Row a = rows.get(i);
      if (a.resolved == null) {
        continue;
      }
      for (int j = i + 1; j < count; j++) {
        Row b = rows.get(j);
        if (b.resolved != null && a.resolved.equals(b.resolved)) {
          a.duplicate = true;
          b.duplicate = true;
        }
      }
    }
    for (int i = 0; i < count; i++) {
      if (rows.get(i).duplicate) {
        duplicates++;
      }
    }
  }

  private void countUsers() {
    lastUserRow = 0;
    FlixelState level = Flixel.state;
    for (int depth = 0; level != null && depth < MAX_DEPTH; depth++) {
      countUsers(level, 0);
      level = level.getSubState();
    }
  }

  private void countUsers(Object node, int depth) {
    if (depth >= MAX_DEPTH) {
      return;
    }
    if (node instanceof FlixelSprite sprite && sprite.isExists()) {
      FlixelTexture texture = sprite.getTexture();
      if (texture != null) {
        int row = rowOf(texture);
        if (row >= 0) {
          rows.get(row).users++;
        }
      }
    }
    if (node instanceof FlixelGroupable<?> group) {
      FlixelArray<?> members = group.getMembers();
      if (members != null) {
        for (int i = 0, n = members.getSize(); i < n; i++) {
          Object member = members.get(i);
          if (member != null) {
            countUsers(member, depth + 1);
          }
        }
      }
    }
  }

  /** Finds the row of a texture, trying the row that matched last first because neighbors share textures. */
  private int rowOf(FlixelTexture texture) {
    if (lastUserRow < count && rows.get(lastUserRow).texture == texture) {
      return lastUserRow;
    }
    int found = indexOfTexture(texture);
    if (found >= 0) {
      lastUserRow = found;
    }
    return found;
  }

  private int indexOfTexture(FlixelTexture texture) {
    for (int i = 0; i < count; i++) {
      if (rows.get(i).texture == texture) {
        return i;
      }
    }
    return -1;
  }

  private void releaseRemoved() {
    for (int i = 0, n = previous.getSize(); i < n; i++) {
      FlixelTexture texture = previous.get(i);
      if (indexOfTexture(texture) < 0) {
        ids.release(texture);
      }
    }
    previous.clear();
    for (int i = count; i < rows.getSize(); i++) {
      rows.get(i).clear();
    }
  }

  private long computeSignature() {
    long hash = 1125899906842597L;
    for (int i = 0; i < count; i++) {
      Row row = rows.get(i);
      hash = 31L * hash + row.id;
      hash = 31L * hash + row.name.hashCode();
      hash = 31L * hash + row.width;
      hash = 31L * hash + row.height;
      hash = 31L * hash + row.format.hashCode();
      hash = 31L * hash + row.bytes;
      hash = 31L * hash + row.users;
      hash = 31L * hash + (row.duplicate ? 1 : 0);
    }
    return hash;
  }

  /**
   * Returns how many textures are in the list.
   *
   * @return The texture count as of the last refresh.
   */
  public int getTextureCount() {
    return count;
  }

  public long getTotalBytes() {
    return totalBytes;
  }

  public int getDuplicateCount() {
    return duplicates;
  }

  /**
   * Returns the texture of a list entry. Together with {@link #getTextureCount()} this walks the list without an
   * iterator.
   *
   * @param index A position from 0 to {@link #getTextureCount()} minus 1.
   * @return The texture.
   */
  @Nullable
  public FlixelTexture getTexture(int index) {
    return rows.get(index).texture;
  }

  /**
   * Returns how many existing sprites show the texture of a list entry.
   *
   * @param index A position from 0 to {@link #getTextureCount()} minus 1.
   * @return The user count as of the last refresh.
   */
  public int getUsers(int index) {
    return rows.get(index).users;
  }

  /** One listed texture. Rows are reused between refreshes. */
  private static final class Row {

    long bytes;
    FlixelTexture texture;
    String name = "";
    String resolved;
    String format = "";
    int id;
    int width;
    int height;
    int users;
    boolean duplicate;

    void clear() {
      texture = null;
      resolved = null;
      name = "";
    }
  }
}
