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

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.CRC32;
import java.util.zip.Inflater;

import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Decodes the encoder's output with the JDK's own PNG reader, which only the tests may use. */
class FlixelPngEncoderTest {

  private static final byte[] SIGNATURE = { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n' };

  @Test
  void theFileStartsWithTheSignatureAndEveryChunkHasAValidCrc() {
    FlixelPngEncoder png = new FlixelPngEncoder();
    png.encode(gradient(7, 5, 7 * 4), 7, 5, 7 * 4, 256);
    byte[] bytes = Arrays.copyOf(png.getBytes(), png.getLength());

    assertArrayEquals(SIGNATURE, Arrays.copyOf(bytes, 8));
    int at = 8;
    String[] expected = { "IHDR", "IDAT", "IEND" };
    for (String type : expected) {
      int size = readInt(bytes, at);
      assertEquals(type, new String(bytes, at + 4, 4, StandardCharsets.US_ASCII));
      CRC32 crc = new CRC32();
      crc.update(bytes, at + 4, 4 + size);
      assertEquals((int) crc.getValue(), readInt(bytes, at + 8 + size), type + " CRC");
      at += 12 + size;
    }
    assertEquals(bytes.length, at, "no bytes after IEND");
  }

  @Test
  void theHeaderDescribesAnEightBitRgbaPicture() {
    FlixelPngEncoder png = new FlixelPngEncoder();
    png.encode(gradient(7, 5, 7 * 4), 7, 5, 7 * 4, 256);
    byte[] b = png.getBytes();
    assertEquals(7, readInt(b, 16));
    assertEquals(5, readInt(b, 20));
    assertEquals(8, b[24], "bit depth");
    assertEquals(6, b[25], "color type RGBA");
    assertEquals(0, b[26] | b[27] | b[28], "no compression variants, filters, or interlacing");
    assertEquals(7, png.getWidth());
    assertEquals(5, png.getHeight());
  }

  @Test
  void theJdkDecodesTheExactPixels() throws IOException {
    int w = 13;
    int h = 9;
    ByteBuffer rgba = gradient(w, h, w * 4);
    FlixelPngEncoder png = new FlixelPngEncoder();
    png.encode(rgba, w, h, w * 4, 256);

    BufferedImage image = decode(png);

    assertEquals(w, image.getWidth());
    assertEquals(h, image.getHeight());
    for (int y = 0; y < h; y++) {
      for (int x = 0; x < w; x++) {
        assertEquals(argb(rgba, (y * w + x) * 4), image.getRGB(x, y), "pixel " + x + "," + y);
      }
    }
  }

  @Test
  void aPictureLargerThanOneStoredBlockUsesSeveralBlocksAndStillDecodes() throws IOException {
    int w = 200;
    int h = 100;
    ByteBuffer rgba = gradient(w, h, w * 4);
    FlixelPngEncoder png = new FlixelPngEncoder();
    png.encode(rgba, w, h, w * 4, 256);

    // The rows add up to 80100 bytes, which is more than the 65535 a stored block holds.
    assertTrue(w * 4 * h > 65535);
    BufferedImage image = decode(png);
    assertEquals(argb(rgba, ((h - 1) * w + (w - 1)) * 4), image.getRGB(w - 1, h - 1));
    assertEquals(argb(rgba, (50 * w + 120) * 4), image.getRGB(120, 50));
  }

  @Test
  void rowPaddingIsSkippedLikeAGpuReadbackNeedsIt() throws IOException {
    int w = 5;
    int h = 3;
    int rowBytes = 256;
    ByteBuffer padded = ByteBuffer.allocate(rowBytes * h);
    ByteBuffer tight = gradient(w, h, w * 4);
    for (int y = 0; y < h; y++) {
      for (int i = 0; i < w * 4; i++) {
        padded.put(y * rowBytes + i, tight.get(y * w * 4 + i));
      }
      for (int i = w * 4; i < rowBytes; i++) {
        padded.put(y * rowBytes + i, (byte) 0x7F);
      }
    }
    FlixelPngEncoder png = new FlixelPngEncoder();
    png.encode(padded, w, h, rowBytes, 256);

    BufferedImage image = decode(png);
    for (int y = 0; y < h; y++) {
      for (int x = 0; x < w; x++) {
        assertEquals(argb(tight, (y * w + x) * 4), image.getRGB(x, y));
      }
    }
  }

  @Test
  void aBigPictureShrinksToTheLongestSideAndKeepsItsShapeWithNearestNeighbor() throws IOException {
    int w = 1000;
    int h = 500;
    ByteBuffer rgba = ByteBuffer.allocate(w * h * 4);
    for (int y = 0; y < h; y++) {
      for (int x = 0; x < w; x++) {
        int i = (y * w + x) * 4;
        // Left half red, right half blue, so a bad sampling shows up as a wrong color.
        rgba.put(i, (byte) (x < w / 2 ? 255 : 0));
        rgba.put(i + 2, (byte) (x < w / 2 ? 0 : 255));
        rgba.put(i + 3, (byte) 255);
      }
    }
    FlixelPngEncoder png = new FlixelPngEncoder();
    png.encode(rgba, w, h, w * 4, 256);

    assertEquals(256, png.getWidth());
    assertEquals(128, png.getHeight());
    BufferedImage image = decode(png);
    assertEquals(256, image.getWidth());
    assertEquals(128, image.getHeight());
    assertEquals(0xFFFF0000, image.getRGB(10, 10));
    assertEquals(0xFF0000FF, image.getRGB(250, 100));
    // Every sampled color is one of the two source colors, never a blend.
    for (int x = 0; x < 256; x++) {
      int rgb = image.getRGB(x, 64);
      assertTrue(rgb == 0xFFFF0000 || rgb == 0xFF0000FF, "blended color at " + x);
    }
  }

  @Test
  void aSmallPictureIsNotEnlargedAndTheBuffersAreReused() {
    FlixelPngEncoder png = new FlixelPngEncoder();
    png.encode(gradient(4, 4, 16), 4, 4, 16, 256);
    byte[] first = png.getBytes();
    int firstLength = png.getLength();
    assertEquals(4, png.getWidth());

    png.encode(gradient(4, 4, 16), 4, 4, 16, 256);

    assertTrue(first == png.getBytes(), "the output array is reused for a picture of the same size");
    assertEquals(firstLength, png.getLength());
  }

  @Test
  void badArgumentsAreRejected() {
    FlixelPngEncoder png = new FlixelPngEncoder();
    ByteBuffer pixels = ByteBuffer.allocate(64);
    assertThrows(IllegalArgumentException.class, () -> png.encode(pixels, 0, 4, 16, 256));
    assertThrows(IllegalArgumentException.class, () -> png.encode(pixels, 4, 0, 16, 256));
    assertThrows(IllegalArgumentException.class, () -> png.encode(pixels, 4, 4, 15, 256));
    assertThrows(IllegalArgumentException.class, () -> png.encode(pixels, 4, 4, 16, 0));
  }

  @Test
  void theZlibStreamIsStoredBlocksWithAValidAdler32() throws Exception {
    FlixelPngEncoder png = new FlixelPngEncoder();
    ByteBuffer rgba = gradient(3, 2, 12);
    png.encode(rgba, 3, 2, 12, 256);
    byte[] b = png.getBytes();
    int ihdr = readInt(b, 8);
    int idat = 8 + 12 + ihdr;
    int size = readInt(b, idat);
    byte[] zlib = Arrays.copyOfRange(b, idat + 8, idat + 8 + size);

    assertEquals(0x78, zlib[0] & 0xFF);
    assertEquals(0, ((zlib[0] & 0xFF) * 256 + (zlib[1] & 0xFF)) % 31, "a valid zlib header");
    assertEquals(1, zlib[2], "one final stored block");
    Inflater inflater = new Inflater();
    inflater.setInput(zlib);
    byte[] raw = new byte[(3 * 4 + 1) * 2];
    assertEquals(raw.length, inflater.inflate(raw));
    assertTrue(inflater.finished(), "the Adler32 check passed");
    inflater.end();
    assertEquals(0, raw[0], "filter type none");
    assertEquals(rgba.get(0), raw[1]);
  }

  private static BufferedImage decode(FlixelPngEncoder png) throws IOException {
    return ImageIO.read(new ByteArrayInputStream(png.getBytes(), 0, png.getLength()));
  }

  private static ByteBuffer gradient(int w, int h, int rowBytes) {
    ByteBuffer buffer = ByteBuffer.allocate(rowBytes * h);
    for (int y = 0; y < h; y++) {
      for (int x = 0; x < w; x++) {
        int i = y * rowBytes + x * 4;
        buffer.put(i, (byte) (x * 19 + y));
        buffer.put(i + 1, (byte) (y * 29 + x));
        buffer.put(i + 2, (byte) (x * 7 + y * 3));
        buffer.put(i + 3, (byte) (200 + (x + y) % 56));
      }
    }
    return buffer;
  }

  private static int argb(ByteBuffer rgba, int index) {
    return ((rgba.get(index + 3) & 0xFF) << 24) | ((rgba.get(index) & 0xFF) << 16)
        | ((rgba.get(index + 1) & 0xFF) << 8) | (rgba.get(index + 2) & 0xFF);
  }

  private static int readInt(byte[] b, int at) {
    return ((b[at] & 0xFF) << 24) | ((b[at + 1] & 0xFF) << 16) | ((b[at + 2] & 0xFF) << 8) | (b[at + 3] & 0xFF);
  }
}
