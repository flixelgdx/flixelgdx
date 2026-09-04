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
package org.flixelgdx.text;

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelCamera;
import org.flixelgdx.FlixelSprite;
import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.file.FlixelFile;
import org.flixelgdx.graphics.FlixelBatch;
import org.flixelgdx.graphics.FlixelFrame;
import org.flixelgdx.graphics.FlixelGraphic;
import org.flixelgdx.graphics.FlixelTexture;
import org.flixelgdx.math.FlixelMatrix;
import org.flixelgdx.util.FlixelAlign;
import org.flixelgdx.util.FlixelColor;
import org.flixelgdx.util.FlixelString;
import org.flixelgdx.util.FlixelStringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A display object for rendering text on screen.
 *
 * <p>Extends {@link FlixelSprite} so that text objects can be added to sprite groups and
 * states, with full support for tinting, fading, rotation, and scaling. Text renders through
 * {@link FlixelFont} glyph atlases: scalable {@code .ttf}/{@code .otf} files are baked at the
 * exact pixel size needed via the platform's {@link FlixelFontRasterizer}, and the packaged
 * bitmap font covers the default case on every platform.
 *
 * <h2>Auto-sizing</h2>
 * <p>By default, {@code FlixelText} auto-sizes to fit its text content. To use a fixed
 * width, pass a positive {@code fieldWidth} to the constructor or call
 * {@link #setFieldWidth(float)}. A fixed height can be set via {@link #setFieldHeight(float)}.
 *
 * <h2>Fonts</h2>
 * <p>The default font is the packaged bitmap font, scaled to the requested size. For best
 * quality at any size, register a {@code .ttf} or {@code .otf} in {@link FlixelFontRegistry}
 * and select it with {@link #setFont(String)}, or point at a file directly with
 * {@link #setFont(FlixelFile)}.
 *
 * <h2>Border Styles</h2>
 * <p>Text can be rendered with borders via {@link #setBorderStyle(BorderStyle, FlixelColor, float, float)}.
 * Supported styles are {@link BorderStyle#SHADOW}, {@link BorderStyle#OUTLINE}, and
 * {@link BorderStyle#OUTLINE_FAST}.
 *
 * <h2>Sprite Methods</h2>
 * <p>Graphic-loading methods inherited from {@link FlixelSprite} are not applicable to text and will throw
 * {@link UnsupportedOperationException} if called.
 */
public class FlixelText extends FlixelSprite {

  /** The text buffer for saving memory when the text is not changing. */
  private final FlixelString textBuffer = new FlixelString(48);

  /** Font size in pixels. */
  private int size;

  /** Horizontal alignment within the field. */
  private Alignment alignment = Alignment.LEFT;

  /** The width of the text field. {@code 0} means auto-width. */
  private float fieldWidth;

  /** The height of the text field. {@code 0} means auto-height. */
  private float fieldHeight;

  /** Extra horizontal spacing between characters, in pixels. */
  private float letterSpacing = 0;

  /** The current border style. */
  private BorderStyle borderStyle = BorderStyle.NONE;

  /** The color of the border in RGBA. */
  private final FlixelColor borderColor = new FlixelColor(FlixelColor.CLEAR);

  /** The size of the border in pixels. */
  private float borderSize = 1;

  /**
   * Quality of the border rendering. {@code 0}: single iteration,
   * {@code 1}: one iteration for every pixel in {@link #borderSize}.
   */
  private float borderQuality = 1;

  /**
   * The screen-to-world pixel ratio at which the current font was last baked.
   * A value of {@code 0} forces a font rebuild on the next draw or measurement.
   */
  private float lastBakeScreenScale = 0f;

  /** Game pixels per baked font pixel for the current font. */
  private float fontScale = 1f;

  /** The font used for text rendering; resolved lazily from the registry cascade. */
  @Nullable
  private FlixelFont font;

  /** Cached text layout used for measurement and drawing. */
  private final FlixelTextLayout layout = new FlixelTextLayout();

  /** The scalable font file used for baking, or {@code null} for the registry or default font. */
  @Nullable
  private FlixelFile fontFile;

  /**
   * The {@link FlixelFontRegistry} ID for the font, or {@code null} if a direct
   * {@link FlixelFile} or the default font is used instead.
   */
  @Nullable
  private String fontRegistryId;

  /** A font supplied directly through {@link #setFont(FlixelFont)}, bypassing the cascade. */
  @Nullable
  private FlixelFont directFont;

  /** Reusable matrix to save the batch's transform before applying text transforms. */
  private final FlixelMatrix savedTransform = new FlixelMatrix();

  /** Reusable matrix for applying rotation/scale transforms during drawing. */
  private final FlixelMatrix textTransform = new FlixelMatrix();

  /** Whether text wraps at {@link #fieldWidth}. Defaults to {@code true}. */
  private boolean wordWrap = true;

  /**
   * Whether the field dimensions are determined automatically from the text
   * content. Requires {@link #wordWrap} to be {@code false} to take full effect.
   */
  private boolean autoSize = true;

  /** Whether to render bold text. Only effective when the font file provides a bold variant. */
  private boolean bold = false;

  /** Whether to render italic text. Only effective when the font file provides an italic variant. */
  private boolean italic = false;

  /** Whether the font needs to be re-resolved (size, font source, or filtering changed). */
  private boolean fontDirty = true;

  /** Whether the text layout needs to be recalculated. */
  private boolean layoutDirty = true;

  /** Creates a new text object at (0, 0) with default settings. */
  public FlixelText() {
    this(0, 0, 0, null, 8);
  }

  /**
   * Creates a new text object at the specified position.
   *
   * @param x The x position of the text.
   * @param y The y position of the text.
   */
  public FlixelText(float x, float y) {
    this(x, y, 0, null, 8);
  }

  /**
   * Creates a new text object at the specified position with a field width.
   *
   * @param x The x position of the text.
   * @param y The y position of the text.
   * @param fieldWidth The width of the text field. Auto-sizes if {@code <= 0}.
   */
  public FlixelText(float x, float y, float fieldWidth) {
    this(x, y, fieldWidth, null, 8);
  }

  /**
   * Creates a new text object with position, field width, and initial text.
   *
   * @param x The x position of the text.
   * @param y The y position of the text.
   * @param fieldWidth The width of the text field. Auto-sizes if {@code <= 0}.
   * @param text The text to display initially.
   */
  public FlixelText(float x, float y, float fieldWidth, String text) {
    this(x, y, fieldWidth, text, 8);
  }

  /**
   * Creates a new text object with all primary parameters.
   *
   * @param x The x position of the text.
   * @param y The y position of the text.
   * @param fieldWidth The width of the text field. Auto-sizes if {@code <= 0}.
   * @param text The text to display initially.
   * @param size The font size in pixels.
   */
  public FlixelText(float x, float y, float fieldWidth, String text, int size) {
    super();
    setPosition(x, y);
    setFieldWidth(fieldWidth);
    setAutoSize(fieldWidth <= 0);
    setText(text);
    setTextSize(size);
  }

  /** Returns the text currently being displayed. */
  public FlixelString getTextBuffer() {
    return textBuffer;
  }

  /**
   * Gets the string version of the text being displayed for {@code this} text object.
   *
   * <p>Note that you should <strong>not</strong> call this every frame. If you must get
   * the texting being displayed of your text object, use {@link #getTextBuffer()} instead,
   * paired with {@link FlixelStringUtil#contentEquals(CharSequence, CharSequence)} for
   * comparisons.
   *
   * @return The string version of the text being displayed on {@code this} text object.
   */
  public String getText() {
    return textBuffer.toString();
  }

  /**
   * Sets the text to display.
   *
   * @param text The new text string.
   */
  public void setText(String text) {
    setText((CharSequence) text);
  }

  /**
   * Sets the display text of {@code this} text object using a native Java {@link CharSequence}.
   *
   * @param text The {@link CharSequence} to apply to {@code this} text object. For most FlixelGDX
   *     games this is typically a {@link FlixelString}.
   */
  public void setText(CharSequence text) {
    if (FlixelStringUtil.contentEquals(text == null ? "null" : text, textBuffer)) {
      return;
    }
    textBuffer.set(text);
    layoutDirty = true;
  }

  /** Returns the font size in pixels. */
  public int getTextSize() {
    return size;
  }

  /**
   * Sets the font size in pixels. When using a scalable font, this triggers a re-bake at the
   * new size. When using the packaged default font, the atlas is scaled.
   *
   * @param size The new font size (minimum 1).
   */
  public void setTextSize(int size) {
    int newSize = Math.max(1, size);
    if (this.size != newSize) {
      this.size = newSize;
      fontDirty = true;
      layoutDirty = true;
    }
  }

  /** Returns the current text alignment. */
  public Alignment getAlignment() {
    return alignment;
  }

  /**
   * Sets the horizontal alignment of the text within the field. Only has a visible
   * effect when {@link #isAutoSize()} is {@code false} (i.e. the field has a fixed width).
   *
   * @param alignment The alignment to use.
   */
  public void setAlignment(@NotNull Alignment alignment) {
    if (this.alignment != alignment) {
      this.alignment = alignment;
      layoutDirty = true;
    }
  }

  /** Returns whether word wrapping is enabled. */
  public boolean isWordWrap() {
    return wordWrap;
  }

  /** Returns whether word wrapping is enabled. */
  public boolean getWordWrap() {
    return wordWrap;
  }

  /**
   * Enables or disables word wrapping. Defaults to {@code true}.
   *
   * @param wordWrap Whether to wrap text at the field width.
   */
  public void setWordWrap(boolean wordWrap) {
    if (this.wordWrap != wordWrap) {
      this.wordWrap = wordWrap;
      layoutDirty = true;
    }
  }

  /** Returns whether the text field auto-sizes to fit its content. */
  public boolean isAutoSize() {
    return autoSize;
  }

  /** Returns whether the text field auto-sizes to fit its content. */
  public boolean getAutoSize() {
    return autoSize;
  }

  /**
   * Sets whether the text field auto-sizes to fit content. When {@code true},
   * {@link #getFieldWidth()} and {@link #getFieldHeight()} are determined
   * automatically. Requires {@link #isWordWrap()} to be {@code false} to
   * take full effect.
   *
   * @param autoSize Whether to auto-size.
   */
  public void setAutoSize(boolean autoSize) {
    if (this.autoSize != autoSize) {
      this.autoSize = autoSize;
      layoutDirty = true;
    }
  }

  /** Returns the width of the text field, or {@code 0} if auto-sizing. */
  public float getFieldWidth() {
    return fieldWidth;
  }

  /**
   * Sets the width of the text field. A positive value disables auto-sizing so
   * the hitbox and layout use this fixed width; a value {@code <= 0} re-enables
   * auto-sizing so dimensions are derived from the text content.
   *
   * @param fieldWidth The field width in pixels.
   */
  public void setFieldWidth(float fieldWidth) {
    if (this.fieldWidth != fieldWidth) {
      this.fieldWidth = fieldWidth;
      autoSize = fieldWidth <= 0;
      layoutDirty = true;
    }
  }

  /** Returns the height of the text field, or {@code 0} if auto-sizing. */
  public float getFieldHeight() {
    return fieldHeight;
  }

  /**
   * Sets the height of the text field. Only used when {@link #isAutoSize()} is {@code false}.
   *
   * @param fieldHeight The field height in pixels.
   */
  public void setFieldHeight(float fieldHeight) {
    if (this.fieldHeight != fieldHeight) {
      this.fieldHeight = fieldHeight;
      layoutDirty = true;
    }
  }

  /** Returns whether bold rendering is requested. */
  public boolean isBold() {
    return bold;
  }

  /** Returns whether bold rendering is requested. */
  public boolean getBold() {
    return bold;
  }

  /**
   * Requests bold rendering. Takes effect only when the selected font file provides a bold
   * variant; regular fonts render unchanged.
   *
   * @param bold Whether to request bold rendering.
   */
  public void setBold(boolean bold) {
    if (this.bold != bold) {
      this.bold = bold;
      fontDirty = true;
    }
  }

  /** Returns whether italic rendering is requested. */
  public boolean isItalic() {
    return italic;
  }

  /** Returns whether italic rendering is requested. */
  public boolean getItalic() {
    return italic;
  }

  /**
   * Requests italic rendering. Takes effect only when the selected font file provides an
   * italic variant; regular fonts render unchanged.
   *
   * @param italic Whether to request italic rendering.
   */
  public void setItalic(boolean italic) {
    if (this.italic != italic) {
      this.italic = italic;
      fontDirty = true;
    }
  }

  /** Returns the extra spacing between characters, in pixels. */
  public float getLetterSpacing() {
    return letterSpacing;
  }

  /**
   * Sets extra horizontal spacing between characters.
   *
   * @param letterSpacing Spacing in pixels; {@code 0} uses the font's natural spacing.
   */
  public void setLetterSpacing(float letterSpacing) {
    if (this.letterSpacing != letterSpacing) {
      this.letterSpacing = letterSpacing;
      layoutDirty = true;
    }
  }

  /**
   * Returns whether a scalable font is active (registry id or direct file), meaning glyphs
   * re-bake crisply when the display scale changes.
   */
  public boolean isEmbedded() {
    return fontRegistryId != null || fontFile != null || FlixelFontRegistry.getDefault() != null;
  }

  /** Returns whether a scalable font is active. */
  public boolean getEmbedded() {
    return isEmbedded();
  }

  /** Returns the current {@link FlixelFontRegistry} font id, or {@code null}. */
  @Nullable
  public String getFont() {
    return fontRegistryId;
  }

  /**
   * Selects a font registered in {@link FlixelFontRegistry}.
   *
   * @param id The registry id, or {@code null} to fall back to the default cascade.
   */
  public void setFont(@Nullable String id) {
    if (id != null && !FlixelFontRegistry.has(id)) {
      Flixel.warn("Fonts", "No font registered under id '" + id + "'; using the default font.");
    }
    fontRegistryId = id;
    fontFile = null;
    directFont = null;
    fontDirty = true;
  }

  /**
   * Selects a scalable font file directly, without registering it globally.
   *
   * @param fontFile The {@code .ttf} or {@code .otf} file to bake from.
   */
  public void setFont(@NotNull FlixelFile fontFile) {
    this.fontFile = fontFile;
    fontRegistryId = null;
    directFont = null;
    fontDirty = true;
  }

  /**
   * Sets a pre-built {@link FlixelFont} directly, bypassing the registry cascade. The caller
   * keeps ownership of the font.
   *
   * @param font The font to render with.
   */
  public void setFont(@NotNull FlixelFont font) {
    directFont = font;
    fontRegistryId = null;
    fontFile = null;
    fontDirty = true;
  }

  /** Returns the current border style. */
  public BorderStyle getBorderStyle() {
    return borderStyle;
  }

  /** Returns the border color. Treat as read-only. */
  public FlixelColor getBorderColor() {
    return borderColor;
  }

  /** Returns the border thickness in pixels. */
  public float getBorderSize() {
    return borderSize;
  }

  /** Returns the border quality factor. */
  public float getBorderQuality() {
    return borderQuality;
  }

  /**
   * Configures the text border.
   *
   * @param style The border style; {@link BorderStyle#NONE} disables borders.
   * @param color The border color; {@code null} keeps the current color.
   * @param size The border thickness in pixels.
   * @param quality Iterations per border pixel for {@link BorderStyle#OUTLINE} ({@code 0} to {@code 1}).
   */
  public void setBorderStyle(BorderStyle style, @Nullable FlixelColor color, float size, float quality) {
    this.borderStyle = (style != null) ? style : BorderStyle.NONE;
    if (color != null) {
      this.borderColor.setColor(color);
    }
    this.borderSize = size;
    this.borderQuality = Math.max(0, Math.min(1, quality));
  }

  /**
   * Configures the text border with default size and quality.
   *
   * @param style The border style.
   * @param color The border color.
   */
  public void setBorderStyle(BorderStyle style, @NotNull FlixelColor color) {
    setBorderStyle(style, color, 1, 1);
  }

  /**
   * Configures a black border with default size and quality.
   *
   * @param style The border style.
   */
  public void setBorderStyle(BorderStyle style) {
    setBorderStyle(style, FlixelColor.BLACK, 1, 1);
  }

  /**
   * Applies several format settings at once from a font file.
   *
   * @param fontFile The scalable font file, or {@code null} to keep the current font.
   * @param size The font size in pixels.
   * @param color The text color, or {@code null} to keep current.
   * @param alignment The alignment, or {@code null} to keep current.
   * @param borderStyle The border style, or {@code null} to keep current.
   * @param borderColor The border color, or {@code null} to keep current.
   */
  public void setFormat(@Nullable FlixelFile fontFile, int size, @Nullable FlixelColor color,
      @Nullable Alignment alignment, @Nullable BorderStyle borderStyle, @Nullable FlixelColor borderColor) {
    if (fontFile != null) {
      setFont(fontFile);
    }
    setTextSize(size);
    if (color != null) {
      setColor(color);
    }
    if (alignment != null) {
      setAlignment(alignment);
    }
    if (borderStyle != null) {
      setBorderStyle(borderStyle, (borderColor != null) ? borderColor : this.borderColor);
    } else if (borderColor != null) {
      this.borderColor.setColor(borderColor);
    }
  }

  /**
   * Applies font file, size, and color at once.
   *
   * @param fontFile The scalable font file, or {@code null} to keep the current font.
   * @param size The font size in pixels.
   * @param color The text color.
   */
  public void setFormat(@Nullable FlixelFile fontFile, int size, @NotNull FlixelColor color) {
    setFormat(fontFile, size, color, null, null, null);
  }

  /**
   * Applies several format settings at once from a registered font id.
   *
   * @param fontId The {@link FlixelFontRegistry} id, or {@code null} to keep the current font.
   * @param size The font size in pixels.
   * @param color The text color, or {@code null} to keep current.
   * @param alignment The alignment, or {@code null} to keep current.
   * @param borderStyle The border style, or {@code null} to keep current.
   * @param borderColor The border color, or {@code null} to keep current.
   */
  public void setFormat(@Nullable String fontId, int size, @Nullable FlixelColor color,
      @Nullable Alignment alignment, @Nullable BorderStyle borderStyle, @Nullable FlixelColor borderColor) {
    if (fontId != null) {
      setFont(fontId);
    }
    setTextSize(size);
    if (color != null) {
      setColor(color);
    }
    if (alignment != null) {
      setAlignment(alignment);
    }
    if (borderStyle != null) {
      setBorderStyle(borderStyle, (borderColor != null) ? borderColor : this.borderColor);
    } else if (borderColor != null) {
      this.borderColor.setColor(borderColor);
    }
  }

  /**
   * Applies registered font id, size, and color at once.
   *
   * @param fontId The {@link FlixelFontRegistry} id, or {@code null} to keep the current font.
   * @param size The font size in pixels.
   * @param color The text color.
   */
  public void setFormat(@Nullable String fontId, int size, @NotNull FlixelColor color) {
    setFormat(fontId, size, color, null, null, null);
  }

  /**
   * Applies size and color at once, keeping the current font.
   *
   * @param size The font size in pixels.
   * @param color The text color.
   */
  public void setFormat(int size, @NotNull FlixelColor color) {
    setFormat((String) null, size, color, null, null, null);
  }

  /** Returns the display width: the field width when fixed, otherwise the measured text width. */
  @Override
  public float getWidth() {
    rebuildIfDirty();
    return super.getWidth();
  }

  /** Returns the display height: the field height when fixed, otherwise the measured text height. */
  @Override
  public float getHeight() {
    rebuildIfDirty();
    return super.getHeight();
  }

  /** Returns the measured width of the laid-out text, ignoring any fixed field size. */
  public float getTextWidth() {
    rebuildIfDirty();
    return layout.getWidth();
  }

  /** Returns the measured height of the laid-out text, ignoring any fixed field size. */
  public float getTextHeight() {
    rebuildIfDirty();
    return layout.getHeight();
  }

  @Override
  public final void update(float elapsed) {
    // No-op: text does not animate.
  }

  @Override
  public void draw(@NotNull FlixelBatch batch) {
    if (!isOnDrawCamera()) {
      return;
    }
    if (textBuffer.isEmpty()) {
      return;
    }
    rebuildIfDirty();
    if (font == null) {
      return;
    }

    FlixelCamera cam = Flixel.getDrawCamera() != null ? Flixel.getDrawCamera() : Flixel.cameras.first();
    float wx = cam.worldToViewX(getX(), getScrollX());
    float wy = cam.worldToViewY(getY(), getScrollY());

    float scaleX = getScaleX();
    float scaleY = getScaleY();
    float rotation = getAngle();
    boolean needsTransform = rotation != 0 || scaleX != 1 || scaleY != 1;

    // The object's (x, y) is its top-left corner in the Y-down world, so the text block's top edge
    // is drawn directly at wy with no vertical offset.
    if (needsTransform) {
      savedTransform.set(batch.getTransform());

      float ox = getOriginX();
      float oy = getOriginY();
      textTransform.set(savedTransform);
      textTransform.translate(wx + ox, wy + oy, 0);
      textTransform.rotateZ(rotation);
      textTransform.scale(scaleX, scaleY, 1);
      textTransform.translate(-ox, -oy, 0);
      batch.setTransform(textTransform);

      drawTextContent(batch, 0, 0);

      batch.setTransform(savedTransform);
    } else {
      drawTextContent(batch, wx, wy);
    }
  }

  @Override
  public void setAntialiasing(boolean antialiasing) {
    super.setAntialiasing(antialiasing);
    fontDirty = true;
  }

  /** Always throws {@link UnsupportedOperationException}; text objects cannot load graphics. */
  @Override
  public final FlixelSprite loadGraphic(FlixelFile file) {
    throw new UnsupportedOperationException("FlixelText does not support loadGraphic(). Use setText() instead.");
  }

  /** Always throws {@link UnsupportedOperationException}; text objects cannot load graphics. */
  @Override
  public final FlixelSprite loadGraphic(FlixelFile file, int frameWidth) {
    throw new UnsupportedOperationException("FlixelText does not support loadGraphic(). Use setText() instead.");
  }

  /** Always throws {@link UnsupportedOperationException}; text objects cannot load graphics. */
  @Override
  public final FlixelSprite loadGraphic(FlixelFile file, int frameWidth, int frameHeight) {
    throw new UnsupportedOperationException("FlixelText does not support loadGraphic(). Use setText() instead.");
  }

  /** Always throws {@link UnsupportedOperationException}; text objects cannot load graphics. */
  @Override
  public final FlixelSprite loadGraphic(FlixelTexture texture, int frameWidth, int frameHeight) {
    throw new UnsupportedOperationException("FlixelText does not support loadGraphic(). Use setText() instead.");
  }

  /** Always throws {@link UnsupportedOperationException}; text objects cannot use Sparrow atlases. */
  @Override
  public final void applySparrowAtlas(@NotNull FlixelGraphic newGraphic,
      @NotNull FlixelArray<FlixelFrame> parsedFrames) {
    throw new UnsupportedOperationException("FlixelText does not support addSparrowAtlas().");
  }

  /** Always throws {@link UnsupportedOperationException}; text has no atlas regions. */
  @Override
  public final FlixelArray<FlixelFrame> getAtlasRegions() {
    throw new UnsupportedOperationException("FlixelText does not support atlas regions.");
  }

  /** Always throws {@link UnsupportedOperationException}; text has no animation frames. */
  @Override
  public final FlixelFrame getCurrentFrame() {
    throw new UnsupportedOperationException("FlixelText does not support animations.");
  }

  /** Always throws {@link UnsupportedOperationException}; text has no image frames. */
  @Override
  public final FlixelFrame[][] getFrames() {
    throw new UnsupportedOperationException("FlixelText does not support animations.");
  }

  @Override
  public void destroy() {
    super.destroy();
    font = null;
    directFont = null;
    textBuffer.clear();
    textBuffer.trimToSize();
    size = 8;
    alignment = Alignment.LEFT;
    wordWrap = true;
    autoSize = true;
    fieldWidth = 0;
    fieldHeight = 0;
    bold = false;
    italic = false;
    letterSpacing = 0;
    borderStyle = BorderStyle.NONE;
    borderColor.setColor(FlixelColor.CLEAR);
    borderSize = 1;
    borderQuality = 1;
    lastBakeScreenScale = 0f;
    fontFile = null;
    fontRegistryId = null;
    fontDirty = true;
    layoutDirty = true;
  }

  @Override
  public String toString() {
    return "FlixelText(textLen=" + textBuffer.length() + ", size=" + size
        + ", x=" + getX() + ", y=" + getY()
        + ", fieldWidth=" + fieldWidth + ", autoSize=" + autoSize + ")";
  }

  /**
   * Rebuilds the font and/or layout if their dirty flags are set.
   * Called lazily before drawing or when dimensions are queried.
   */
  private void rebuildIfDirty() {
    if (!fontDirty && directFont == null && isEmbedded()) {
      float s = currentScreenScale();
      if (Math.abs(s - lastBakeScreenScale) > 0.01f) {
        fontDirty = true;
      }
    }
    if (fontDirty) {
      resolveFont();
      fontDirty = false;
      layoutDirty = true;
    }
    if (layoutDirty) {
      layoutDirty = false;
      rebuildLayout();
    }
  }

  /**
   * Resolves the active {@link FlixelFont} following the cascade: direct font, registry id,
   * direct file, registry default, packaged bitmap font.
   *
   * <p>Scalable sources are baked at the current screen-pixel size (see
   * {@link #currentScreenScale()}) so glyphs stay crisp at any display resolution; the layout
   * then measures in game pixels through {@link #fontScale}.
   */
  private void resolveFont() {
    if (directFont != null) {
      font = directFont;
      fontScale = 1f;
      return;
    }

    float screenScale = currentScreenScale();
    lastBakeScreenScale = screenScale;
    int bakeSize = Math.max(1, Math.round(size * screenScale));
    boolean smooth = isAntialiasing();

    FlixelFont resolved = null;
    if (fontRegistryId != null) {
      resolved = FlixelFontRegistry.getFont(fontRegistryId, bakeSize, smooth);
    }
    if (resolved == null && fontFile != null) {
      resolved = FlixelFontRegistry.getFont(fontFile, bakeSize, smooth);
    }
    if (resolved != null) {
      font = resolved;
      fontScale = 1f / screenScale;
      return;
    }

    resolved = FlixelFontRegistry.getDefaultFont(bakeSize, smooth);
    font = resolved;
    if (resolved == null) {
      fontScale = 1f;
      return;
    }
    // The packaged bitmap font has a fixed baked size; scale its base height to the
    // requested size. A registry default bakes at the exact size, where base maps back
    // to the screen scale.
    if (FlixelFontRegistry.getDefault() != null) {
      fontScale = 1f / screenScale;
    } else {
      float base = Math.max(1f, resolved.getBase());
      fontScale = size / base;
    }
  }

  /**
   * Returns the ratio of screen pixels to world (game) pixels for the current camera.
   *
   * <p>Uses the active draw camera when inside a draw call, otherwise falls back to the
   * primary camera. Returns {@code 1f} when no camera or game context is available.
   */
  private float currentScreenScale() {
    FlixelCamera cam = Flixel.getDrawCamera();
    if (cam == null) {
      if (Flixel.game == null || Flixel.cameras.isEmpty()) {
        return 1f;
      }
      cam = Flixel.cameras.first();
    }
    float worldH = cam.getViewport().getWorldHeight();
    float screenH = cam.getViewport().getScreenHeight();
    if (worldH <= 0f || screenH <= 0f) {
      return 1f;
    }
    return screenH / worldH;
  }

  /** Recalculates the text layout and updates the sprite dimensions. */
  private void rebuildLayout() {
    if (font == null) {
      resolveFont();
    }
    if (font == null) {
      return;
    }

    boolean fixedWidth = fieldWidth > 0 && !autoSize;
    float layoutFieldWidth = fixedWidth ? fieldWidth : 0f;
    layout.set(font, textBuffer, fontScale, layoutFieldWidth, wordWrap && fixedWidth,
        alignment.align, letterSpacing);

    // Alignment without a fixed field aligns within the natural width, matching the old
    // behavior where centered auto-sized text still centers its shorter lines.
    if (!fixedWidth && alignment != Alignment.LEFT && layout.getWidth() > 0) {
      layout.set(font, textBuffer, fontScale, layout.getWidth(), false, alignment.align, letterSpacing);
    }

    float w = fixedWidth ? fieldWidth : layout.getWidth();
    float h = (fieldHeight > 0 && !autoSize) ? fieldHeight : layout.getHeight();
    setSize(w, h);
    setOriginCenter();
  }

  /**
   * Draws the full text content (border + main text) at the given coordinates.
   *
   * @param batch The sprite batch.
   * @param x The x coordinate of the text's left edge.
   * @param y The y coordinate of the text's <em>top</em> edge.
   */
  private void drawTextContent(FlixelBatch batch, float x, float y) {
    if (borderStyle != BorderStyle.NONE && borderColor.a > 0 && borderSize > 0) {
      drawBorder(batch, x, y);
    }
    batch.setColor(getColor());
    layout.draw(batch, x, y);
    batch.setColor(FlixelColor.WHITE);
  }

  /** Draws the text border/outline by rendering the layout at offset positions. */
  private void drawBorder(FlixelBatch batch, float x, float y) {
    batch.setColor(borderColor);

    switch (borderStyle) {
      case SHADOW -> layout.draw(batch, x + borderSize, y + borderSize);

      case OUTLINE_FAST -> {
        layout.draw(batch, x - borderSize, y);
        layout.draw(batch, x + borderSize, y);
        layout.draw(batch, x, y - borderSize);
        layout.draw(batch, x, y + borderSize);
      }

      case OUTLINE -> {
        int iterations = Math.max(1, (int) (borderSize * borderQuality));
        float step = borderSize / iterations;
        for (int i = 1; i <= iterations; i++) {
          float offset = step * i;
          layout.draw(batch, x - offset, y - offset);
          layout.draw(batch, x, y - offset);
          layout.draw(batch, x + offset, y - offset);
          layout.draw(batch, x - offset, y);
          layout.draw(batch, x + offset, y);
          layout.draw(batch, x - offset, y + offset);
          layout.draw(batch, x, y + offset);
          layout.draw(batch, x + offset, y + offset);
        }
      }

      default -> throw new IllegalArgumentException("Unexpected value: " + borderStyle);
    }
  }

  /** Horizontal alignment options for text within its field. */
  public enum Alignment {
    LEFT(FlixelAlign.LEFT),
    CENTER(FlixelAlign.CENTER),
    RIGHT(FlixelAlign.RIGHT);

    final int align;

    Alignment(int align) {
      this.align = align;
    }

    /**
     * Returns the alignment constant corresponding to the given integer value.
     *
     * @param value {@code 0} for LEFT, {@code 1} for CENTER, {@code 2} for RIGHT.
     * @return The matching alignment constant.
     * @throws IllegalArgumentException if {@code value} is not a valid alignment integer.
     */
    public static Alignment fromInt(int value) {
      return switch (value) {
        case 0 -> LEFT;
        case 1 -> CENTER;
        case 2 -> RIGHT;
        default -> throw new IllegalArgumentException("Invalid alignment value: " + value);
      };
    }

    /**
     * Returns the integer representation of this alignment constant.
     *
     * @return {@code 0} for LEFT, {@code 1} for CENTER, {@code 2} for RIGHT.
     */
    public int toInt() {
      return switch (this) {
        case LEFT -> 0;
        case CENTER -> 1;
        case RIGHT -> 2;
      };
    }
  }

  /** Border rendering styles for text. */
  public enum BorderStyle {
    /** No border. */
    NONE,
    /** A single offset copy drawn down-right, like a drop shadow. */
    SHADOW,
    /** A full outline built from multiple passes; smoothest, most draw calls. */
    OUTLINE,
    /** A four-direction outline; cheaper than {@link #OUTLINE} with slightly rougher corners. */
    OUTLINE_FAST
  }
}
