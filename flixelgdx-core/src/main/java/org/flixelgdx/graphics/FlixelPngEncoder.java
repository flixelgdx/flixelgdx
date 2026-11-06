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
package org.flixelgdx.graphics;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

/**
 * Turns RGBA pixels into a small PNG file, with no help from the platform.
 *
 * <p>Think of packing a picture into a flat parcel: the encoder does not squeeze the picture (that would take
 * a compression library), it only wraps it in the envelope that every PNG reader expects. The pixel rows are
 * stored as "uncompressed" deflate blocks, and the checksums (CRC32 for each chunk and Adler32 for the zlib stream)
 * are computed by hand. That makes the files bigger than a real encoder's, which does not matter for a debugger
 * thumbnail, and it means the encoder runs everywhere, including the browser and Android, without
 * {@code java.util.zip}, {@code java.awt}, or {@code javax.imageio}.
 *
 * <p>The encoder can also shrink a picture while it writes it. It picks every n-th pixel (nearest neighbor), so
 * pixel art stays sharp. One encoder reuses its buffers, so encoding many thumbnails in a row creates little
 * garbage. The buffers grow when a picture needs more room and never shrink.
 *
 * <p>Example:
 *
 * <pre>{@code
 * FlixelPngEncoder png = new FlixelPngEncoder();
 * png.encode(rgbaBuffer, width, height, width * 4, 256); // At most 256 pixels on the longest side.
 * send(png.getBytes(), 0, png.getLength());              // The valid bytes are the first getLength() of them.
 * }</pre>
 *
 * <p>This class is not thread safe.
 */
public final class FlixelPngEncoder {

  private static final int[] CRC_TABLE = new int[256];

  /** The largest number of data bytes in one stored deflate block. */
  private static final int MAX_BLOCK = 65535;

  private static final int ADLER_MOD = 65521;

  /** The most bytes Adler32 adds up before reducing its sums, small enough that a signed int never overflows. */
  private static final int ADLER_CHUNK = 3800;

  private static final byte[] SIGNATURE = { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n' };
  private static final byte[] IHDR = { 'I', 'H', 'D', 'R' };
  private static final byte[] IDAT = { 'I', 'D', 'A', 'T' };
  private static final byte[] IEND = { 'I', 'E', 'N', 'D' };

  static {
    for (int n = 0; n < 256; n++) {
      int c = n;
      for (int k = 0; k < 8; k++) {
        c = (c & 1) != 0 ? 0xEDB88320 ^ (c >>> 1) : c >>> 1;
      }
      CRC_TABLE[n] = c;
    }
  }

  private byte[] raw = new byte[0];
  private byte[] out = new byte[0];

  private int length;
  private int width;
  private int height;

  /** Creates an encoder with empty buffers. */
  public FlixelPngEncoder() {}

  /**
   * Encodes a picture, shrinking it first when it is larger than {@code maxSide}.
   *
   * <p>The result is read with {@link #getBytes()} and {@link #getLength()}. It stays valid until the next call.
   *
   * @param rgba The source pixels as 8-bit RGBA. Only absolute reads are used, so its position and limit are not
   *     changed.
   * @param srcWidth The width of the source picture in pixels.
   * @param srcHeight The height of the source picture in pixels.
   * @param rowBytes The distance in bytes between the starts of two source rows, at least {@code srcWidth * 4}.
   * @param maxSide The most pixels the longest side of the result may have. A smaller source is not enlarged.
   * @return The size of the PNG in bytes.
   * @throws IllegalArgumentException If the picture is empty, {@code rowBytes} is too small, or {@code maxSide} is
   *     not positive.
   */
  public int encode(@NotNull ByteBuffer rgba, int srcWidth, int srcHeight, int rowBytes, int maxSide) {
    if (srcWidth <= 0 || srcHeight <= 0 || maxSide <= 0 || rowBytes < srcWidth * 4) {
      throw new IllegalArgumentException("Bad picture: " + srcWidth + "x" + srcHeight + ", row " + rowBytes
          + ", max side " + maxSide + ".");
    }
    int longest = Math.max(srcWidth, srcHeight);
    int outW = srcWidth;
    int outH = srcHeight;
    if (longest > maxSide) {
      outW = Math.max(1, (int) ((long) srcWidth * maxSide / longest));
      outH = Math.max(1, (int) ((long) srcHeight * maxSide / longest));
    }
    width = outW;
    height = outH;

    int stride = outW * 4 + 1;
    int rawSize = stride * outH;
    if (raw.length < rawSize) {
      raw = new byte[rawSize];
    }
    int at = 0;
    for (int y = 0; y < outH; y++) {
      raw[at++] = 0; // Filter type "none".
      int srcRow = (int) ((2L * y + 1) * srcHeight / (2L * outH)) * rowBytes;
      for (int x = 0; x < outW; x++) {
        int src = srcRow + (int) ((2L * x + 1) * srcWidth / (2L * outW)) * 4;
        raw[at++] = rgba.get(src);
        raw[at++] = rgba.get(src + 1);
        raw[at++] = rgba.get(src + 2);
        raw[at++] = rgba.get(src + 3);
      }
    }
    return write(rawSize, outW, outH);
  }

  /**
   * Returns the buffer that holds the PNG after {@link #encode}. Only the first {@link #getLength()} bytes belong
   * to the picture, and the array is replaced when a larger picture is encoded.
   *
   * @return The encoder's output buffer.
   */
  public byte @NotNull [] getBytes() {
    return out;
  }

  /**
   * Returns how many bytes of {@link #getBytes()} the last picture uses.
   *
   * @return The PNG size in bytes, or {@code 0} before the first {@link #encode}.
   */
  public int getLength() {
    return length;
  }

  /**
   * Returns the width of the last encoded picture after shrinking.
   *
   * @return The width in pixels.
   */
  public int getWidth() {
    return width;
  }

  /**
   * Returns the height of the last encoded picture after shrinking.
   *
   * @return The height in pixels.
   */
  public int getHeight() {
    return height;
  }

  private int write(int rawSize, int outW, int outH) {
    int blocks = (rawSize + MAX_BLOCK - 1) / MAX_BLOCK;
    // 2 bytes zlib header, 5 bytes of header per block, the data, and 4 bytes Adler32.
    int idatSize = 2 + blocks * 5 + rawSize + 4;
    int total = SIGNATURE.length + (12 + 13) + (12 + idatSize) + 12;
    if (out.length < total) {
      out = new byte[total];
    }
    int at = 0;
    for (byte b : SIGNATURE) {
      out[at++] = b;
    }

    at = beginChunk(at, 13, IHDR);
    int dataStart = at;
    at = putInt(at, outW);
    at = putInt(at, outH);
    out[at++] = 8; // Bit depth.
    out[at++] = 6; // Color type: RGBA.
    out[at++] = 0; // Compression method.
    out[at++] = 0; // Filter method.
    out[at++] = 0; // No interlacing.
    at = endChunk(at, dataStart - 4);

    at = beginChunk(at, idatSize, IDAT);
    dataStart = at;
    out[at++] = 0x78; // Deflate with a 32 KB window.
    out[at++] = 0x01; // No preset dictionary, fastest level; the header is a multiple of 31.
    for (int offset = 0; offset < rawSize; offset += MAX_BLOCK) {
      int size = Math.min(MAX_BLOCK, rawSize - offset);
      out[at++] = (byte) (offset + size >= rawSize ? 1 : 0); // Final block flag, block type 00 (stored).
      out[at++] = (byte) size;
      out[at++] = (byte) (size >>> 8);
      out[at++] = (byte) ~size;
      out[at++] = (byte) (~size >>> 8);
      System.arraycopy(raw, offset, out, at, size);
      at += size;
    }
    at = putInt(at, adler32(raw, rawSize));
    at = endChunk(at, dataStart - 4);

    at = beginChunk(at, 0, IEND);
    at = endChunk(at, at - 4);
    length = at;
    return length;
  }

  private int beginChunk(int at, int size, byte[] type) {
    at = putInt(at, size);
    for (int i = 0; i < 4; i++) {
      out[at++] = type[i];
    }
    return at;
  }

  /** Writes the CRC32 of everything from {@code typeStart} (the chunk type) up to {@code at}. */
  private int endChunk(int at, int typeStart) {
    int crc = ~0;
    for (int i = typeStart; i < at; i++) {
      crc = CRC_TABLE[(crc ^ out[i]) & 0xFF] ^ (crc >>> 8);
    }
    return putInt(at, ~crc);
  }

  private int putInt(int at, int value) {
    out[at++] = (byte) (value >>> 24);
    out[at++] = (byte) (value >>> 16);
    out[at++] = (byte) (value >>> 8);
    out[at++] = (byte) value;
    return at;
  }

  private static int adler32(byte[] data, int size) {
    int a = 1;
    int b = 0;
    for (int offset = 0; offset < size; offset += ADLER_CHUNK) {
      int end = Math.min(size, offset + ADLER_CHUNK);
      for (int i = offset; i < end; i++) {
        a += data[i] & 0xFF;
        b += a;
      }
      a %= ADLER_MOD;
      b %= ADLER_MOD;
    }
    return (b << 16) | a;
  }
}
