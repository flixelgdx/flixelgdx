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
package org.flixelgdx.backend.gpu;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelKtx2Test {

  private static final int RGBA8_ONLY = FlixelGpuBlockFormat.RGBA8.bit();
  private static final int MOBILE = RGBA8_ONLY | FlixelGpuBlockFormat.ASTC_4X4.bit()
      | FlixelGpuBlockFormat.ETC2_RGBA8.bit() | FlixelGpuBlockFormat.ETC2_RGB8.bit();
  private static final int DESKTOP = RGBA8_ONLY | FlixelGpuBlockFormat.BC7.bit() | FlixelGpuBlockFormat.BC3.bit()
      | FlixelGpuBlockFormat.BC1.bit();

  @Test
  void encoderOutputIsRecognizedAsBasisUniversal() {
    FlixelKtx2 etc1s = FlixelKtx2.parse(FlixelKtx2Files.fixture("etc1s_alpha.ktx2"));
    assertNotNull(etc1s);
    assertTrue(etc1s.isBasis());
    assertFalse(etc1s.isUastc());
    assertEquals(FlixelKtx2.SUPERCOMPRESSION_BASIS_LZ, etc1s.getSupercompression());
    assertEquals(50, etc1s.getWidth());
    assertEquals(37, etc1s.getHeight());
    assertEquals(6, etc1s.getLevels());

    FlixelKtx2 uastc = FlixelKtx2.parse(FlixelKtx2Files.fixture("uastc_alpha.ktx2"));
    assertNotNull(uastc);
    assertTrue(uastc.isBasis());
    assertTrue(uastc.isUastc());
    assertEquals(FlixelKtx2.SUPERCOMPRESSION_ZSTD, uastc.getSupercompression());
    assertEquals(0, uastc.getVkFormat());
  }

  @Test
  void parsingLeavesTheBufferUntouched() {
    ByteBuffer file = FlixelKtx2Files.fixture("etc1s_opaque.ktx2");
    file.position(7);
    assertNotNull(FlixelKtx2.parse(file));
    assertEquals(7, file.position());
  }

  @Test
  void invalidFilesAreRejected() {
    ByteBuffer good = FlixelKtx2Files.raw(FlixelGpuBlockFormat.RGBA8, 4, 4, 1, 0);
    assertNotNull(FlixelKtx2.parse(good));

    ByteBuffer badMagic = FlixelKtx2Files.raw(FlixelGpuBlockFormat.RGBA8, 4, 4, 1, 0);
    badMagic.put(1, (byte) 'X');
    assertNull(FlixelKtx2.parse(badMagic));

    ByteBuffer truncated = FlixelKtx2Files.raw(FlixelGpuBlockFormat.RGBA8, 4, 4, 1, 0);
    truncated.limit(truncated.limit() - 1);
    assertNull(FlixelKtx2.parse(truncated), "a level running past the end of the file");

    assertNull(FlixelKtx2.parse(ByteBuffer.allocate(20)), "shorter than the header");

    ByteBuffer cube = FlixelKtx2Files.raw(FlixelGpuBlockFormat.RGBA8, 4, 4, 1, 0);
    cube.putInt(36, 6);
    assertNull(FlixelKtx2.parse(cube), "cube maps are not supported");

    ByteBuffer oneDimensional = FlixelKtx2Files.raw(FlixelGpuBlockFormat.RGBA8, 4, 4, 1, 0);
    oneDimensional.putInt(24, 0);
    assertNull(FlixelKtx2.parse(oneDimensional), "1D textures are not supported");
  }

  @Test
  void aRawFileIsUploadedStraightFromTheContainer() {
    ByteBuffer file = FlixelKtx2Files.raw(FlixelGpuBlockFormat.RGBA8, 8, 4, 4, 0);
    FlixelGpuCompressedImage image = FlixelKtx2.decode(file, RGBA8_ONLY, null);
    assertSame(file, image.getData(), "no copy is made");
    assertEquals(FlixelGpuBlockFormat.RGBA8, image.getFormat());
    assertEquals(4, image.getLevels());
    int[] expected = { 8 * 4 * 4, 4 * 2 * 4, 2 * 4, 4 };
    FlixelKtx2 header = FlixelKtx2.parse(file);
    assertNotNull(header);
    for (int level = 0; level < 4; level++) {
      assertEquals(expected[level], image.getSize(level));
      assertEquals(header.getLevelOffset(level), image.getOffset(level));
      assertEquals(level + 1, file.get(image.getOffset(level)));
    }
    assertEquals(8, image.getGpuWidth());
    assertEquals(4, image.getGpuHeight());
  }

  @Test
  void anSrgbFileUsesTheUnormFormat() {
    int size = FlixelGpuBlockFormat.ASTC_4X4.levelBytes(8, 8);
    ByteBuffer file = FlixelKtx2Files.build(158, 8, 8, 1, 0, new int[] { size });
    assertEquals(FlixelGpuBlockFormat.ASTC_4X4, FlixelKtx2.decode(file, MOBILE, null).getFormat());
  }

  @Test
  void aRawFileNeedsAFormatTheGpuSupports() {
    ByteBuffer astc = FlixelKtx2Files.raw(FlixelGpuBlockFormat.ASTC_4X4, 16, 16, 1, 0);
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> FlixelKtx2.decode(astc, DESKTOP, null));
    assertTrue(e.getMessage().contains("ASTC_4X4"), e.getMessage());
    assertEquals(FlixelGpuBlockFormat.ASTC_4X4, FlixelKtx2.decode(astc, MOBILE, null).getFormat());
  }

  @Test
  void aRawFileMayNotBeSupercompressedOrUseAnUnknownFormat() {
    ByteBuffer zstd = FlixelKtx2Files.raw(FlixelGpuBlockFormat.BC7, 8, 8, 1, FlixelKtx2.SUPERCOMPRESSION_ZSTD);
    assertThrows(IllegalArgumentException.class, () -> FlixelKtx2.decode(zstd, DESKTOP, null));

    ByteBuffer unknown = FlixelKtx2Files.build(100, 4, 4, 1, 0, new int[] { 64 });
    assertThrows(IllegalArgumentException.class, () -> FlixelKtx2.decode(unknown, DESKTOP, null));
  }

  @Test
  void aRawLevelOfTheWrongSizeIsRejected() {
    ByteBuffer file = FlixelKtx2Files.build(37, 4, 4, 1, 0, new int[] { 60 });
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> FlixelKtx2.decode(file, RGBA8_ONLY, null));
    assertTrue(e.getMessage().contains("64"), e.getMessage());
  }

  @Test
  void aBasisFileNeedsATranscoder() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> FlixelKtx2.decode(FlixelKtx2Files.fixture("etc1s_alpha.ktx2"), MOBILE, null));
    assertTrue(e.getMessage().contains("transcoder"), e.getMessage());
  }

  @Test
  void aBasisFileIsTranscodedLevelByLevelIntoOneBuffer() {
    FakeTranscoder transcoder = new FakeTranscoder(false, true);
    FlixelGpuCompressedImage image = FlixelKtx2.decode(FlixelKtx2Files.fixture("etc1s_alpha.ktx2"), MOBILE,
        transcoder);
    assertEquals(FlixelGpuBlockFormat.ETC2_RGBA8, image.getFormat());
    assertEquals(50, image.getWidth());
    assertEquals(37, image.getHeight());
    assertEquals(52, image.getGpuWidth());
    assertEquals(40, image.getGpuHeight());
    // 52x40 halves to 26x20 (7x5 blocks, like 25x18), but then to 13x10 (4x3 blocks, unlike 12x9 with 3x3).
    assertEquals(2, image.getLevels());
    assertEquals(13 * 10 * 16, image.getSize(0));
    assertEquals(7 * 5 * 16, image.getSize(1));
    assertEquals(image.getSize(0), image.getOffset(1));
    assertEquals(1, image.getData().get(0));
    assertEquals(2, image.getData().get(image.getOffset(1)));
    assertEquals(0, image.getData().position());
    assertEquals(image.getData().capacity(), image.getData().limit());
    assertTrue(transcoder.closed, "the handle is always released");
  }

  @Test
  void aFailedTranscodeStillClosesTheHandle() {
    FakeTranscoder transcoder = new FakeTranscoder(true, true);
    transcoder.fail = true;
    assertThrows(IllegalArgumentException.class,
        () -> FlixelKtx2.decode(FlixelKtx2Files.fixture("uastc_alpha.ktx2"), MOBILE, transcoder));
    assertTrue(transcoder.closed);
  }

  @Test
  void transcodingToRgba8KeepsEveryLevel() {
    FakeTranscoder transcoder = new FakeTranscoder(false, true);
    FlixelGpuCompressedImage image = FlixelKtx2.decode(FlixelKtx2Files.fixture("etc1s_alpha.ktx2"), RGBA8_ONLY,
        transcoder);
    assertEquals(FlixelGpuBlockFormat.RGBA8, image.getFormat());
    assertEquals(6, image.getLevels());
    assertEquals(50 * 37 * 4, image.getSize(0));
    assertEquals(1 * 1 * 4, image.getSize(5));
  }

  @Test
  void basisTargetsKeepTheMostQualityTheGpuAllows() {
    assertEquals(FlixelGpuBlockFormat.ASTC_4X4, FlixelKtx2.chooseBasisTarget(true, true, MOBILE));
    assertEquals(FlixelGpuBlockFormat.ETC2_RGBA8, FlixelKtx2.chooseBasisTarget(false, true, MOBILE));
    assertEquals(FlixelGpuBlockFormat.ETC2_RGB8, FlixelKtx2.chooseBasisTarget(false, false, MOBILE));
    assertEquals(FlixelGpuBlockFormat.BC7, FlixelKtx2.chooseBasisTarget(true, true, DESKTOP));
    assertEquals(FlixelGpuBlockFormat.BC7, FlixelKtx2.chooseBasisTarget(false, false, DESKTOP));
    int astcOnly = RGBA8_ONLY | FlixelGpuBlockFormat.ASTC_4X4.bit();
    assertEquals(FlixelGpuBlockFormat.ASTC_4X4, FlixelKtx2.chooseBasisTarget(false, true, astcOnly));
    int etc2Only = RGBA8_ONLY | FlixelGpuBlockFormat.ETC2_RGBA8.bit() | FlixelGpuBlockFormat.ETC2_RGB8.bit();
    assertEquals(FlixelGpuBlockFormat.ETC2_RGBA8, FlixelKtx2.chooseBasisTarget(true, true, etc2Only));
    assertEquals(FlixelGpuBlockFormat.RGBA8, FlixelKtx2.chooseBasisTarget(true, true, RGBA8_ONLY));
    assertEquals(FlixelGpuBlockFormat.RGBA8, FlixelKtx2.chooseBasisTarget(false, false, RGBA8_ONLY));
  }

  @Test
  void usableLevelsStopWhereThePaddedChainDisagrees() {
    assertEquals(7, FlixelKtx2.usableLevels(FlixelGpuBlockFormat.ASTC_4X4, 64, 64, 7));
    assertEquals(2, FlixelKtx2.usableLevels(FlixelGpuBlockFormat.ASTC_4X4, 50, 37, 6));
    assertEquals(6, FlixelKtx2.usableLevels(FlixelGpuBlockFormat.RGBA8, 50, 37, 6));
    assertEquals(1, FlixelKtx2.usableLevels(FlixelGpuBlockFormat.BC7, 64, 64, 1));
    assertEquals(3, FlixelKtx2.usableLevels(FlixelGpuBlockFormat.RGBA8, 4, 4, 16), "never more than the size allows");
  }

  /** Pretends to transcode the 50x37 fixtures, filling each level with its number plus one. */
  private static final class FakeTranscoder implements FlixelBasisTranscoder {

    private final boolean uastc;
    private final boolean alpha;

    private boolean closed;
    private boolean fail;

    FakeTranscoder(boolean uastc, boolean alpha) {
      this.uastc = uastc;
      this.alpha = alpha;
    }

    @Override
    public long open(@NotNull ByteBuffer container) {
      assertTrue(container.isDirect());
      return 42L;
    }

    @Override
    public void close(long handle) {
      assertEquals(42L, handle);
      closed = true;
    }

    @Override
    public int getLevels(long handle) {
      return 6;
    }

    @Override
    public int getWidth(long handle) {
      return 50;
    }

    @Override
    public int getHeight(long handle) {
      return 37;
    }

    @Override
    public boolean hasAlpha(long handle) {
      return alpha;
    }

    @Override
    public boolean isUastc(long handle) {
      return uastc;
    }

    @Override
    public boolean transcode(long handle, int level, @NotNull FlixelGpuBlockFormat target,
        @NotNull ByteBuffer output) {
      if (fail) {
        return false;
      }
      assertEquals(target.levelBytes(50 >> level, 37 >> level), output.remaining());
      for (int i = output.position(); i < output.limit(); i++) {
        output.put(i, (byte) (level + 1));
      }
      return true;
    }
  }
}
