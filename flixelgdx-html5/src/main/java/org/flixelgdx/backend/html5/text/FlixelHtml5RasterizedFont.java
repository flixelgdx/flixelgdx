package org.flixelgdx.backend.html5.text;

import org.flixelgdx.text.FlixelGlyphBitmap;
import org.flixelgdx.text.FlixelRasterizedFont;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;

public class FlixelHtml5RasterizedFont implements FlixelRasterizedFont {

  private final float scale;
  private final float ascent;
  private final float descent;
  private final float lineHeight;

  FlixelHtml5RasterizedFont(ByteBuffer fontData, float scale, float ascent, float descent, float lineHeight) {
    this.scale = scale;
    this.ascent = ascent;
    this.descent = descent;
    this.lineHeight = lineHeight;
  }

  @Override
  public float getAscent() {
    return ascent;
  }

  @Override
  public float getDescent() {
    return descent;
  }

  @Override
  public float getLineHeight() {
    return lineHeight;
  }

  @Override
  public @Nullable FlixelGlyphBitmap rasterize(int codepoint) {
    return null;
  }

  @Override
  public void destroy() {

  }
}
