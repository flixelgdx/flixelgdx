package me.stringdotjar.flixelgdx.text;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.XmlReader;

import me.stringdotjar.flixelgdx.FlixelSprite;

import com.badlogic.gdx.utils.ObjectMap;

import org.jetbrains.annotations.NotNull;

/**
 * A display object for rendering text on screen.
 *
 * <p>Extends {@link FlixelSprite} so that text objects can be added to sprite groups and
 * states, with full support for tinting, fading, rotation, and scaling. Uses libGDX's
 * {@link BitmapFont} for rendering and optionally {@link FreeTypeFontGenerator} for
 * dynamic font generation from {@code .ttf}/{@code .otf} files.
 *
 * <h3>Auto-sizing</h3>
 * <p>By default, {@code FlixelText} auto-sizes to fit its text content. To use a fixed
 * width, pass a positive {@code fieldWidth} to the constructor or call
 * {@link #setFieldWidth(float)}. A fixed height can be set via {@link #setFieldHeight(float)}.
 *
 * <h3>Fonts</h3>
 * <p>The default font is libGDX's built-in bitmap font (Arial 15px), scaled to the
 * requested size. For best quality at any size, supply a {@code .ttf} or {@code .otf}
 * file via {@link #setFont(FileHandle)}, which uses FreeType to generate a crisp
 * bitmap font at the exact pixel size requested.
 *
 * <h3>Border Styles</h3>
 * <p>Text can be rendered with borders via {@link #setBorderStyle(BorderStyle, Color, float, float)}.
 * Supported styles are {@link BorderStyle#SHADOW}, {@link BorderStyle#OUTLINE}, and
 * {@link BorderStyle#OUTLINE_FAST}.
 *
 * <h3>Sprite Methods</h3>
 * <p>Graphic-loading and animation methods inherited from {@link FlixelSprite} are not
 * applicable to text and will throw {@link UnsupportedOperationException} if called.
 */
public class FlixelText extends FlixelSprite {

  /** The text being displayed. */
  private String text = "";

  /** Font size in pixels. */
  private int size;

  /** Horizontal alignment within the field. */
  private Alignment alignment = Alignment.LEFT;

  /** Whether text wraps at {@link #fieldWidth}. Defaults to {@code true}. */
  private boolean wordWrap = true;

  /**
   * Whether the field dimensions are determined automatically from the text
   * content. Requires {@link #wordWrap} to be {@code false} to take full effect.
   */
  private boolean autoSize = true;

  /** The width of the text field. {@code 0} means auto-width. */
  private float fieldWidth;

  /** The height of the text field. {@code 0} means auto-height. */
  private float fieldHeight;

  /** Whether to render bold text. Only effective with FreeType fonts. */
  private boolean bold = false;

  /** Whether to render italic text. Only effective with FreeType fonts. */
  private boolean italic = false;

  /** Whether the text is underlined. Stored for API compatibility. */
  private boolean underline = false;

  /** Extra horizontal spacing between characters, in pixels. */
  private float letterSpacing = 0;

  /** The current border style. */
  private BorderStyle borderStyle = BorderStyle.NONE;

  /** The color of the border in RGBA. */
  private final Color borderColor = new Color(Color.CLEAR);

  /** The size of the border in pixels. */
  private float borderSize = 1;

  /**
   * Quality of the border rendering. {@code 0}: single iteration,
   * {@code 1}: one iteration for every pixel in {@link #borderSize}.
   */
  private float borderQuality = 1;

  /** The font used for text rendering. */
  private BitmapFont bitmapFont;

  /** Cached text layout used for measurement and drawing. */
  private final GlyphLayout glyphLayout = new GlyphLayout();

  /** The TrueType font file used for FreeType generation, or {@code null} for the default font. */
  private FileHandle fontFile;

  /**
   * The {@link FlixelFontRegistry} ID for the font, or {@code null} if a direct
   * {@link FileHandle} or the default font is used instead.
   */
  private String fontRegistryId;

  /**
   * The FreeType generator used by this instance. May be owned by
   * {@link FlixelFontRegistry} (shared) or by this instance (private).
   */
  private FreeTypeFontGenerator generator;

  /** The path used to create a privately-owned {@link #generator}, for change detection. */
  private String currentGeneratorPath;

  /** Whether this instance created (and therefore owns) the current {@link #generator}. */
  private boolean ownsGenerator;

  /** Whether the font needs to be regenerated (size, bold, italic, or font file changed). */
  private boolean fontDirty = true;

  /** Whether the text layout needs to be recalculated. */
  private boolean layoutDirty = true;

  /** Reusable matrix to save the batch's transform before applying text transforms. */
  private final Matrix4 savedTransform = new Matrix4();

  /** Reusable matrix for applying rotation/scale transforms during drawing. */
  private final Matrix4 textTransform = new Matrix4();

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
  public String getText() {
    return text;
  }

  /**
   * Sets the text to display.
   *
   * @param text The new text string.
   * @return This instance for chaining.
   */
  public FlixelText setText(String text) {
    String newText = (text != null) ? text : "null";
    if (!this.text.equals(newText)) {
      this.text = newText;
      layoutDirty = true;
    }
    return this;
  }

  /** Returns the font size in pixels. */
  public int getTextSize() {
    return size;
  }

  /**
   * Sets the font size in pixels. When using a FreeType font, this triggers font
   * regeneration. When using the default font, the built-in bitmap font is scaled.
   *
   * @param size The new font size (minimum 1).
   * @return This instance for chaining.
   */
  public FlixelText setTextSize(int size) {
    int newSize = Math.max(1, size);
    if (this.size != newSize) {
      this.size = newSize;
      fontDirty = true;
      layoutDirty = true;
    }
    return this;
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
   * @return This instance for chaining.
   */
  public FlixelText setAlignment(@NotNull Alignment alignment) {
    if (this.alignment != alignment) {
      this.alignment = alignment;
      layoutDirty = true;
    }
    return this;
  }

  /** Returns whether word wrapping is enabled. */
  public boolean isWordWrap() {
    return wordWrap;
  }

  /**
   * Enables or disables word wrapping. Defaults to {@code true}.
   *
   * @param wordWrap Whether to wrap text at the field width.
   * @return This instance for chaining.
   */
  public FlixelText setWordWrap(boolean wordWrap) {
    if (this.wordWrap != wordWrap) {
      this.wordWrap = wordWrap;
      layoutDirty = true;
    }
    return this;
  }

  /** Returns whether the text field auto-sizes to fit its content. */
  public boolean isAutoSize() {
    return autoSize;
  }

  /**
   * Sets whether the text field auto-sizes to fit content. When {@code true},
   * {@link #getFieldWidth()} and {@link #getFieldHeight()} are determined
   * automatically. Requires {@link #isWordWrap()} to be {@code false} to
   * take full effect.
   *
   * @param autoSize Whether to auto-size.
   * @return This instance for chaining.
   */
  public FlixelText setAutoSize(boolean autoSize) {
    if (this.autoSize != autoSize) {
      this.autoSize = autoSize;
      layoutDirty = true;
    }
    return this;
  }

  /** Returns the width of the text field, or {@code 0} if auto-sizing. */
  public float getFieldWidth() {
    return fieldWidth;
  }

  /**
   * Sets the width of the text field. Enables auto-sizing if {@code <= 0}.
   *
   * @param fieldWidth The field width in pixels.
   * @return This instance for chaining.
   */
  public FlixelText setFieldWidth(float fieldWidth) {
    float newWidth = Math.max(0, fieldWidth);
    if (this.fieldWidth != newWidth) {
      this.fieldWidth = newWidth;
      if (newWidth <= 0) {
        autoSize = true;
      }
      layoutDirty = true;
    }
    return this;
  }

  /** Returns the height of the text field, or {@code 0} if auto-height. */
  public float getFieldHeight() {
    return fieldHeight;
  }

  /**
   * Sets the height of the text field. When {@code <= 0}, height is determined
   * automatically from the text content. Has no effect when {@link #isAutoSize()}
   * is {@code true}.
   *
   * @param fieldHeight The field height in pixels.
   * @return This instance for chaining.
   */
  public FlixelText setFieldHeight(float fieldHeight) {
    float newHeight = Math.max(0, fieldHeight);
    if (this.fieldHeight != newHeight) {
      this.fieldHeight = newHeight;
      layoutDirty = true;
    }
    return this;
  }

  /** Returns whether bold text is enabled. */
  public boolean isBold() {
    return bold;
  }

  /**
   * Sets whether to use bold text. Only takes visual effect when a {@code .ttf}
   * font has been set via {@link #setFont(FileHandle)}, since the default bitmap
   * font does not support runtime weight changes.
   *
   * @param bold Whether to use bold.
   * @return This instance for chaining.
   */
  public FlixelText setBold(boolean bold) {
    if (this.bold != bold) {
      this.bold = bold;
      if (fontFile != null || fontRegistryId != null) {
        fontDirty = true;
      }
      layoutDirty = true;
    }
    return this;
  }

  /** Returns whether italic text is enabled. */
  public boolean isItalic() {
    return italic;
  }

  /**
   * Sets whether to use italic text. Only takes visual effect when a {@code .ttf}
   * font has been set via {@link #setFont(FileHandle)}, since the default bitmap
   * font does not support runtime style changes.
   *
   * @param italic Whether to use italic.
   * @return This instance for chaining.
   */
  public FlixelText setItalic(boolean italic) {
    if (this.italic != italic) {
      this.italic = italic;
      if (fontFile != null || fontRegistryId != null) {
        fontDirty = true;
      }
      layoutDirty = true;
    }
    return this;
  }

  /** Returns whether underline is enabled. */
  public boolean isUnderline() {
    return underline;
  }

  /**
   * Sets whether to underline the text. This property is stored for API
   * compatibility but visual rendering of underlines is limited.
   *
   * @param underline Whether to underline.
   * @return This instance for chaining.
   */
  public FlixelText setUnderline(boolean underline) {
    this.underline = underline;
    return this;
  }

  /** Returns the letter spacing in pixels. */
  public float getLetterSpacing() {
    return letterSpacing;
  }

  /**
   * Sets the spacing between characters in pixels. Only takes visual effect
   * when a {@code .ttf} font has been set via {@link #setFont(FileHandle)},
   * as FreeType uses this value during glyph generation.
   *
   * @param letterSpacing The spacing in pixels.
   * @return This instance for chaining.
   */
  public FlixelText setLetterSpacing(float letterSpacing) {
    if (this.letterSpacing != letterSpacing) {
      this.letterSpacing = letterSpacing;
      if (fontFile != null || fontRegistryId != null) {
        fontDirty = true;
      }
      layoutDirty = true;
    }
    return this;
  }

  /**
   * Returns whether this text uses a custom embedded font ({@code true}) or the
   * default libGDX bitmap font ({@code false}). A font is considered embedded when
   * set via {@link #setFont(FileHandle)}, {@link #setFont(String)}, or when a
   * {@linkplain FlixelFontRegistry#setDefault(String) registry default} is active.
   */
  public boolean isEmbedded() {
    return fontFile != null || fontRegistryId != null
      || FlixelFontRegistry.getDefault() != null;
  }

  /**
   * Returns the {@link FlixelFontRegistry} ID currently set on this text, or
   * {@code null} if a direct {@link FileHandle} or the default font is used.
   */
  public String getFontRegistryId() {
    return fontRegistryId;
  }

  /**
   * Sets the font by its {@link FlixelFontRegistry} identifier. The font must
   * have been previously registered via
   * {@link FlixelFontRegistry#register(String, FileHandle)}. Pass {@code null}
   * to clear the registry reference and fall back to the default resolution order
   * (direct file &rarr; registry default &rarr; built-in font).
   *
   * @param id The registered font ID, or {@code null} to clear.
   * @return This instance for chaining.
   * @throws IllegalArgumentException if {@code id} is non-null but not registered.
   */
  public FlixelText setFont(String id) {
    if (id != null && !FlixelFontRegistry.has(id)) {
      throw new IllegalArgumentException("No font registered with id \"" + id + "\".");
    }
    disposeOwnedGenerator();
    this.fontRegistryId = id;
    this.fontFile = null;
    fontDirty = true;
    layoutDirty = true;
    return this;
  }

  /**
   * Sets a custom TrueType font file for this text. Uses FreeType to generate a
   * bitmap font at the current size. Pass {@code null} to revert to the default
   * font. This clears any previously set {@linkplain #setFont(String) registry ID}.
   *
   * @param fontFile The {@code .ttf} or {@code .otf} file handle, or {@code null}.
   * @return This instance for chaining.
   */
  public FlixelText setFont(FileHandle fontFile) {
    disposeOwnedGenerator();
    this.fontFile = fontFile;
    this.fontRegistryId = null;
    fontDirty = true;
    layoutDirty = true;
    return this;
  }

  /**
   * Sets a pre-built {@link BitmapFont} directly, bypassing FreeType generation.
   * This gives full control over font settings. The caller is responsible for the
   * font's lifecycle if this text is destroyed or the font is replaced.
   * Clears any previously set font file or registry ID.
   *
   * @param font The bitmap font to use. Must not be {@code null}.
   * @return This instance for chaining.
   * @throws IllegalArgumentException if {@code font} is {@code null}.
   */
  public FlixelText setBitmapFont(BitmapFont font) {
    if (font == null) {
      throw new IllegalArgumentException("BitmapFont cannot be null.");
    }
    disposeFont();
    this.bitmapFont = font;
    this.fontFile = null;
    this.fontRegistryId = null;
    fontDirty = false;
    layoutDirty = true;
    return this;
  }

  /** Returns the current border style. */
  public BorderStyle getBorderStyle() {
    return borderStyle;
  }

  /** Returns the border color. */
  public Color getBorderColor() {
    return borderColor;
  }

  /** Returns the border size in pixels. */
  public float getBorderSize() {
    return borderSize;
  }

  /** Returns the border rendering quality. */
  public float getBorderQuality() {
    return borderQuality;
  }

  /**
   * Sets the border style, color, size, and quality in one call.
   *
   * @param style The border style.
   * @param color The border color in RGBA. Pass {@code null} for transparent.
   * @param size The border size in pixels.
   * @param quality Rendering quality. {@code 0}: single iteration, {@code 1}: one
   * iteration per pixel in {@code size}.
   * @return This instance for chaining.
   */
  public FlixelText setBorderStyle(BorderStyle style, Color color, float size, float quality) {
    this.borderStyle = (style != null) ? style : BorderStyle.NONE;
    this.borderColor.set((color != null) ? color : Color.CLEAR);
    this.borderSize = Math.max(0, size);
    this.borderQuality = Math.max(0, quality);
    return this;
  }

  /**
   * Sets the border style and color with default size (1) and quality (1).
   *
   * @param style The border style.
   * @param color The border color.
   * @return This instance for chaining.
   */
  public FlixelText setBorderStyle(BorderStyle style, Color color) {
    return setBorderStyle(style, color, 1, 1);
  }

  /**
   * Sets the border style with a default black color, size of 1, and quality of 1.
   *
   * @param style The border style.
   * @return This instance for chaining.
   */
  public FlixelText setBorderStyle(BorderStyle style) {
    return setBorderStyle(style, Color.BLACK, 1, 1);
  }

  /**
   * Convenience method to set many text properties at once. Pass {@code null} or
   * {@code 0} for any parameter to keep its current value.
   *
   * @param fontFile The {@code .ttf}/{@code .otf} font file, or {@code null} to keep current.
   * @param size The font size, or {@code 0} to keep current.
   * @param color The text color, or {@code null} to keep current.
   * @param alignment The text alignment, or {@code null} to keep current.
   * @param borderStyle The border style, or {@code null} to keep current.
   * @param borderColor The border color, or {@code null} to keep current.
   * @return This instance for chaining.
   */
  public FlixelText setFormat(FileHandle fontFile, int size, Color color,
                              Alignment alignment, BorderStyle borderStyle,
                              Color borderColor) {
    if (fontFile != null) {
      setFont(fontFile);
    }
    if (size > 0) {
      setTextSize(size);
    }
    if (color != null) {
      setColor(color);
    }
    if (alignment != null) {
      setAlignment(alignment);
    }
    if (borderStyle != null) {
      setBorderStyle(borderStyle, (borderColor != null) ? borderColor : this.borderColor);
    } else if (borderColor != null) {
      this.borderColor.set(borderColor);
    }
    return this;
  }

  /**
   * Simplified format setter with font file, size, and color.
   *
   * @param fontFile The font file, or {@code null} to keep current.
   * @param size The font size, or {@code 0} to keep current.
   * @param color The text color, or {@code null} to keep current.
   * @return This instance for chaining.
   */
  public FlixelText setFormat(FileHandle fontFile, int size, Color color) {
    return setFormat(fontFile, size, color, null, null, null);
  }

  /**
   * Convenience format setter using a {@link FlixelFontRegistry} font ID.
   * Pass {@code null} for any parameter to keep its current value.
   *
   * @param fontId The registered font ID, or {@code null} to keep current.
   * @param size The font size, or {@code 0} to keep current.
   * @param color The text color, or {@code null} to keep current.
   * @param alignment The text alignment, or {@code null} to keep current.
   * @param borderStyle The border style, or {@code null} to keep current.
   * @param borderColor The border color, or {@code null} to keep current.
   * @return This instance for chaining.
   */
  public FlixelText setFormat(String fontId, int size, Color color,
                              Alignment alignment, BorderStyle borderStyle,
                              Color borderColor) {
    if (fontId != null) {
      setFont(fontId);
    }
    if (size > 0) {
      setTextSize(size);
    }
    if (color != null) {
      setColor(color);
    }
    if (alignment != null) {
      setAlignment(alignment);
    }
    if (borderStyle != null) {
      setBorderStyle(borderStyle, (borderColor != null) ? borderColor : this.borderColor);
    } else if (borderColor != null) {
      this.borderColor.set(borderColor);
    }
    return this;
  }

  /**
   * Simplified format setter with a registry font ID, size, and color.
   *
   * @param fontId The registered font ID, or {@code null} to keep current.
   * @param size The font size, or {@code 0} to keep current.
   * @param color The text color, or {@code null} to keep current.
   * @return This instance for chaining.
   */
  public FlixelText setFormat(String fontId, int size, Color color) {
    return setFormat(fontId, size, color, null, null, null);
  }

  /**
   * Simplified format setter with size and color only.
   *
   * @param size The font size, or {@code 0} to keep current.
   * @param color The text color, or {@code null} to keep current.
   * @return This instance for chaining.
   */
  public FlixelText setFormat(int size, Color color) {
    return setFormat((FileHandle) null, size, color, null, null, null);
  }

  /**
   * Returns the actual rendered width of the text content (which may differ from
   * {@link #getFieldWidth()} when a fixed field width is set). Triggers a layout
   * rebuild if necessary.
   */
  public float getTextWidth() {
    rebuildIfDirty();
    return glyphLayout.width;
  }

  /**
   * Returns the actual rendered height of the text content (which may differ from
   * {@link #getFieldHeight()} when a fixed field height is set). Triggers a layout
   * rebuild if necessary.
   */
  public float getTextHeight() {
    rebuildIfDirty();
    return glyphLayout.height;
  }

  /**
   * Text objects do not use frame-based animation. This override prevents the
   * animation state machine in {@link FlixelSprite} from running.
   */
  @Override
  public void update(float delta) {
    // No-op: text does not animate.
  }

  @Override
  public void draw(Batch batch) {
    if (text.isEmpty()) {
      return;
    }
    rebuildIfDirty();

    float scaleX = getScaleX();
    float scaleY = getScaleY();
    float rotation = getAngle();
    boolean needsTransform = rotation != 0 || scaleX != 1 || scaleY != 1;

    float textTop = getHeight();

    if (needsTransform) {
      savedTransform.set(batch.getTransformMatrix());

      float ox = getOriginX();
      float oy = getOriginY();
      textTransform.set(savedTransform);
      textTransform.translate(getX() + ox, getY() + oy, 0);
      textTransform.rotate(0, 0, 1, rotation);
      textTransform.scale(scaleX, scaleY, 1);
      textTransform.translate(-ox, -oy, 0);
      batch.setTransformMatrix(textTransform);

      drawTextContent(batch, 0, textTop);

      batch.setTransformMatrix(savedTransform);
    } else {
      drawTextContent(batch, getX(), getY() + textTop);
    }
  }

  /** @throws UnsupportedOperationException always; text objects cannot load graphics. */
  @Override
  public final FlixelSprite loadGraphic(FileHandle path) {
    throw new UnsupportedOperationException("FlixelText does not support loadGraphic(). Use setText() instead.");
  }

  /** @throws UnsupportedOperationException always; text objects cannot load graphics. */
  @Override
  public final FlixelSprite loadGraphic(FileHandle path, int frameWidth) {
    throw new UnsupportedOperationException("FlixelText does not support loadGraphic(). Use setText() instead.");
  }

  /** @throws UnsupportedOperationException always; text objects cannot load graphics. */
  @Override
  public final FlixelSprite loadGraphic(FileHandle path, int frameWidth, int frameHeight) {
    throw new UnsupportedOperationException("FlixelText does not support loadGraphic(). Use setText() instead.");
  }

  /** @throws UnsupportedOperationException always; text objects cannot load graphics. */
  @Override
  public final FlixelSprite loadGraphic(Texture texture, int frameWidth, int frameHeight) {
    throw new UnsupportedOperationException("FlixelText does not support loadGraphic(). Use setText() instead.");
  }

  /** @throws UnsupportedOperationException always; text objects cannot load sparrow frames. */
  @Override
  public final FlixelSprite loadSparrowFrames(FileHandle texture, FileHandle xmlFile) {
    throw new UnsupportedOperationException("FlixelText does not support loadSparrowFrames().");
  }

  /** @throws UnsupportedOperationException always; text objects cannot load sparrow frames. */
  @Override
  public final FlixelSprite loadSparrowFrames(Texture texture, XmlReader.Element xmlFile) {
    throw new UnsupportedOperationException("FlixelText does not support loadSparrowFrames().");
  }

  /** @throws UnsupportedOperationException always; text objects do not have frame animations. */
  @Override
  public final void addAnimationByPrefix(String name, String prefix, int frameRate, boolean loop) {
    throw new UnsupportedOperationException("FlixelText does not support animations.");
  }

  /** @throws UnsupportedOperationException always; text objects do not have frame animations. */
  @Override
  public final void addAnimation(String name, int[] frameIndices, float frameDuration) {
    throw new UnsupportedOperationException("FlixelText does not support animations.");
  }

  /** @throws UnsupportedOperationException always; text objects do not have frame animations. */
  @Override
  public final void playAnimation(String name) {
    throw new UnsupportedOperationException("FlixelText does not support animations.");
  }

  /** @throws UnsupportedOperationException always; text objects do not have frame animations. */
  @Override
  public final void playAnimation(String name, boolean loop) {
    throw new UnsupportedOperationException("FlixelText does not support animations.");
  }

  /** @throws UnsupportedOperationException always; text objects do not have frame animations. */
  @Override
  public final void playAnimation(String name, boolean loop, boolean forceRestart) {
    throw new UnsupportedOperationException("FlixelText does not support animations.");
  }

  /** @return {@code true} always, since text has no animations to finish. */
  @Override
  public final boolean isAnimationFinished() {
    return true;
  }

  private static final ObjectMap<String, Animation<TextureRegion>> EMPTY_ANIMATIONS = new ObjectMap<>(0);

  /** @return An empty map; text has no animations. */
  @Override
  public final ObjectMap<String, Animation<TextureRegion>> getAnimations() {
    return EMPTY_ANIMATIONS;
  }

  /** @return {@code null} always; text has no atlas regions. */
  @Override
  public final Array<TextureAtlas.AtlasRegion> getAtlasRegions() {
    return null;
  }

  /** @return {@code null} always; text has no animation frames. */
  @Override
  public final TextureAtlas.AtlasRegion getCurrentFrame() {
    return null;
  }

  /** @return {@code null} always; text has no image frames. */
  @Override
  public final TextureRegion[][] getFrames() {
    return null;
  }

  @Override
  public void destroy() {
    reset();
  }

  @Override
  public void reset() {
    disposeFont();
    text = "";
    size = 8;
    alignment = Alignment.LEFT;
    wordWrap = true;
    autoSize = true;
    fieldWidth = 0;
    fieldHeight = 0;
    bold = false;
    italic = false;
    underline = false;
    letterSpacing = 0;
    borderStyle = BorderStyle.NONE;
    borderColor.set(Color.CLEAR);
    borderSize = 1;
    borderQuality = 1;
    fontFile = null;
    fontRegistryId = null;
    currentGeneratorPath = null;
    ownsGenerator = false;
    fontDirty = true;
    layoutDirty = true;
    setPosition(0, 0);
    setColor(Color.WHITE);
  }

  @Override
  public String toString() {
    return "FlixelText(text=\"" + text + "\", size=" + size
      + ", x=" + getX() + ", y=" + getY()
      + ", fieldWidth=" + fieldWidth + ", autoSize=" + autoSize + ")";
  }

  /**
   * Rebuilds the font and/or layout if their dirty flags are set.
   * Called lazily before drawing or when dimensions are queried.
   */
  private void rebuildIfDirty() {
    if (fontDirty) {
      rebuildFont();
      fontDirty = false;
      layoutDirty = true;
    }
    if (layoutDirty) {
      rebuildLayout();
      layoutDirty = false;
    }
  }

  /**
   * Regenerates the {@link BitmapFont} based on current settings. The font source
   * is resolved in this order:
   * <ol>
   *   <li>{@link #fontRegistryId} &mdash; shared generator from {@link FlixelFontRegistry}</li>
   *   <li>{@link #fontFile} &mdash; privately-owned generator for a direct file</li>
   *   <li>{@link FlixelFontRegistry#getDefault()} &mdash; global registry default</li>
   *   <li>libGDX built-in bitmap font (Arial 15px, scaled)</li>
   * </ol>
   */
  private void rebuildFont() {
    BitmapFont oldFont = bitmapFont;

    FreeTypeFontGenerator gen = resolveGenerator();
    if (gen != null) {
      FreeTypeFontParameter param = new FreeTypeFontParameter();
      param.size = size;
      param.spaceX = (int) letterSpacing;
      param.genMipMaps = true;
      param.minFilter = Texture.TextureFilter.Linear;
      param.magFilter = Texture.TextureFilter.Linear;

      bitmapFont = gen.generateFont(param);
    } else {
      bitmapFont = new BitmapFont();
      float defaultHeight = bitmapFont.getLineHeight();
      if (defaultHeight > 0) {
        bitmapFont.getData().setScale(size / defaultHeight);
      }
      bitmapFont.getRegion()
        .getTexture()
        .setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    }

    if (oldFont != null) {
      oldFont.dispose();
    }
  }

  /**
   * Resolves the {@link FreeTypeFontGenerator} to use, following the cascade:
   * registry ID &rarr; direct file &rarr; registry default &rarr; {@code null}.
   */
  private FreeTypeFontGenerator resolveGenerator() {
    if (fontRegistryId != null) {
      generator = FlixelFontRegistry.getGenerator(fontRegistryId);
      ownsGenerator = false;
      return generator;
    }

    if (fontFile != null) {
      String path = fontFile.path();
      if (!ownsGenerator || generator == null || !path.equals(currentGeneratorPath)) {
        disposeOwnedGenerator();
        generator = new FreeTypeFontGenerator(fontFile);
        currentGeneratorPath = path;
        ownsGenerator = true;
      }
      return generator;
    }

    FreeTypeFontGenerator defaultGen = FlixelFontRegistry.getDefaultGenerator();
    if (defaultGen != null) {
      generator = defaultGen;
      ownsGenerator = false;
      return generator;
    }

    return null;
  }

  /** Recalculates the text layout and updates the sprite dimensions. */
  private void rebuildLayout() {
    if (bitmapFont == null) {
      return;
    }

    boolean fixedWidth = fieldWidth > 0 && !autoSize;

    if (fixedWidth) {
      glyphLayout.setText(bitmapFont, text, Color.WHITE, fieldWidth,
        alignment.gdxAlign, wordWrap);
    } else if (alignment != Alignment.LEFT) {
      glyphLayout.setText(bitmapFont, text);
      float naturalWidth = glyphLayout.width;
      if (naturalWidth > 0) {
        glyphLayout.setText(bitmapFont, text, Color.WHITE, naturalWidth, alignment.gdxAlign, false);
      }
    } else {
      glyphLayout.setText(bitmapFont, text);
    }

    float w = fixedWidth ? fieldWidth : glyphLayout.width;
    float h = (fieldHeight > 0 && !autoSize) ? fieldHeight : glyphLayout.height;
    setSize(w, h);
    setOriginCenter();
  }

  /**
   * Draws the full text content (border + main text) at the given coordinates.
   *
   * @param batch The sprite batch.
   * @param x The x coordinate of the text's left edge.
   * @param y The y coordinate of the text's <em>top</em> edge (BitmapFont convention).
   */
  private void drawTextContent(Batch batch, float x, float y) {
    Color spriteColor = getColor();

    if (borderStyle != BorderStyle.NONE && borderColor.a > 0 && borderSize > 0) {
      drawBorder(batch, x, y);
    }

    updateLayoutColors(spriteColor);
    bitmapFont.draw(batch, glyphLayout, x, y);
  }

  /** Draws the text border/outline by rendering the layout at offset positions. */
  private void drawBorder(Batch batch, float x, float y) {
    updateLayoutColors(borderColor);

    switch (borderStyle) {
      case SHADOW:
        bitmapFont.draw(batch, glyphLayout, x + borderSize, y - borderSize);
        break;

      case OUTLINE_FAST:
        bitmapFont.draw(batch, glyphLayout, x - borderSize, y);
        bitmapFont.draw(batch, glyphLayout, x + borderSize, y);
        bitmapFont.draw(batch, glyphLayout, x, y - borderSize);
        bitmapFont.draw(batch, glyphLayout, x, y + borderSize);
        break;

      case OUTLINE:
        int iterations = Math.max(1, (int) (borderSize * borderQuality));
        float step = borderSize / iterations;
        for (int i = 1; i <= iterations; i++) {
          float offset = step * i;
          bitmapFont.draw(batch, glyphLayout, x - offset, y - offset);
          bitmapFont.draw(batch, glyphLayout, x, y - offset);
          bitmapFont.draw(batch, glyphLayout, x + offset, y - offset);
          bitmapFont.draw(batch, glyphLayout, x - offset, y);
          bitmapFont.draw(batch, glyphLayout, x + offset, y);
          bitmapFont.draw(batch, glyphLayout, x - offset, y + offset);
          bitmapFont.draw(batch, glyphLayout, x, y + offset);
          bitmapFont.draw(batch, glyphLayout, x + offset, y + offset);
        }
        break;

      default:
        break;
    }
  }

  /**
   * Updates the color of all glyphs in the cached layout. The layout stores colors
   * as (glyphIndex, ABGR8888) pairs in {@link GlyphLayout#colors}.
   */
  private void updateLayoutColors(Color color) {
    int colorBits = color.toIntBits();
    for (int i = 1; i < glyphLayout.colors.size; i += 2) {
      glyphLayout.colors.set(i, colorBits);
    }
  }

  /** Disposes the BitmapFont and any privately-owned generator. */
  private void disposeFont() {
    if (bitmapFont != null) {
      bitmapFont.dispose();
      bitmapFont = null;
    }
    disposeOwnedGenerator();
  }

  /** Disposes the generator only if this instance owns it (not borrowed from the registry). */
  private void disposeOwnedGenerator() {
    if (generator != null && ownsGenerator) {
      generator.dispose();
    }
    generator = null;
    currentGeneratorPath = null;
    ownsGenerator = false;
  }

  /** Horizontal alignment options for text within its field. */
  public enum Alignment {
    LEFT(Align.left),
    CENTER(Align.center),
    RIGHT(Align.right);

    final int gdxAlign;

    Alignment(int gdxAlign) {
      this.gdxAlign = gdxAlign;
    }

    public static Alignment fromInt(int value) {
      return switch (value) {
        case 0 -> LEFT;
        case 1 -> CENTER;
        case 2 -> RIGHT;
        default -> throw new IllegalArgumentException("Invalid alignment value: " + value);
      };
    }

    public int toInt() {
      return switch (this) {
        case LEFT -> 0;
        case CENTER -> 1;
        case RIGHT -> 2;
      };
    }

    public int toGdxAlign() {
      return switch (this) {
        case LEFT -> Align.left;
        case CENTER -> Align.center;
        case RIGHT -> Align.right;
      };
    }
  }

  /** Border/outline styles for text rendering. */
  public enum BorderStyle {
    /** No border. */
    NONE,
    /** A simple drop-shadow offset below and to the right of the text. */
    SHADOW,
    /** A full outline drawn in all 8 directions around each glyph. */
    OUTLINE,
    /** A faster outline using only the 4 cardinal directions. */
    OUTLINE_FAST
  }
}
