package org.flixelgdx.backend.html5.text;

import org.flixelgdx.text.FlixelGlyphBitmap;
import org.flixelgdx.text.FlixelRasterizedFont;
import org.jetbrains.annotations.Nullable;

public class FlixelHtml5RasterizedFont implements FlixelRasterizedFont {

  @Override
  public float getAscent() {
    return 0;
  }

  @Override
  public float getDescent() {
    return 0;
  }

  @Override
  public float getLineHeight() {
    return 0;
  }

  @Override
  public @Nullable FlixelGlyphBitmap rasterize(int codepoint) {
    return null;
  }

  @Override
  public void destroy() {

  }
}
