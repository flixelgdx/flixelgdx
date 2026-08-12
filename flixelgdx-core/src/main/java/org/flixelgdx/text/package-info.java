/**
 * Text rendering and font management for FlixelGDX.
 *
 * <p>The central display object is {@link org.flixelgdx.text.FlixelText FlixelText}, which
 * extends {@link org.flixelgdx.FlixelSprite FlixelSprite} so that text can be placed in states
 * and groups like any other sprite - with full tinting, rotation, scaling, and
 * {@link org.flixelgdx.tween.FlixelTween FlixelTween} support.
 *
 * <h2>Quick start</h2>
 * <p>Creating a text object with the built-in bitmap font requires no setup at all:
 *
 * <pre>{@code
 * // Constructor: x, y, fieldWidth (0 = auto-size), text, size
 * FlixelText label = new FlixelText(10, 20, 0, "Score: 0", 16);
 * add(label);
 * }</pre>
 *
 * <h2>Registering scalable fonts</h2>
 * <p>For crisp text at any pixel size, register a {@code .ttf} or {@code .otf} file with
 * {@link org.flixelgdx.text.FlixelFontRegistry FlixelFontRegistry} once at startup, then
 * reference it by ID from any text object. Baked atlases are cached per ID and size, so
 * repeated lookups are instant:
 *
 * <pre>{@code
 * // In create():
 * FlixelFontRegistry.register("ui", Flixel.files.internal("fonts/Nunito.ttf"));
 * FlixelFontRegistry.setDefault("ui");
 *
 * // Anywhere:
 * FlixelText title = new FlixelText(0, 0, 400, "Level Complete!", 32);
 * title.setFont("ui");
 * title.screenCenter();
 * add(title);
 * }</pre>
 *
 * <h2>Fixed vs. auto-size fields</h2>
 * <p>Pass a positive {@code fieldWidth} to the constructor to give the text a fixed column
 * width; content that exceeds the width wraps automatically. Leave {@code fieldWidth} at zero
 * for a single-line label that resizes itself to fit. Use
 * {@link org.flixelgdx.text.FlixelText#setFieldWidth(float) FlixelText.setFieldWidth(...)} or
 * {@link org.flixelgdx.text.FlixelText#setFieldHeight(float) FlixelText.setFieldHeight(...)} to
 * adjust these after construction.
 *
 * <h2>Border styles</h2>
 * <p>A drop shadow or outline can be added via
 * {@link org.flixelgdx.text.FlixelText#setBorderStyle(org.flixelgdx.text.FlixelText.BorderStyle, org.flixelgdx.util.FlixelColor, float, float) FlixelText.setBorderStyle(...)}:
 *
 * <pre>{@code
 * // Black drop shadow, 2 px offset, full opacity.
 * label.setBorderStyle(FlixelText.BorderStyle.SHADOW, FlixelColor.BLACK, 2f, 1f);
 *
 * // White outline, 1 px thick.
 * label.setBorderStyle(FlixelText.BorderStyle.OUTLINE, FlixelColor.WHITE, 1f, 1f);
 * }</pre>
 *
 * <h2>How fonts work internally</h2>
 * <p>{@link org.flixelgdx.text.FlixelFont FlixelFont} is the low-level object: a set of glyph
 * metrics baked into one or more atlas textures. Scalable files are turned into atlases through
 * the platform's {@link org.flixelgdx.text.FlixelFontRasterizer FlixelFontRasterizer} (installed
 * by the backend when the launcher runs). Bitmap fonts in AngelCode {@code .fnt} format are
 * parsed directly. The packaged default bitmap font is used as a fallback whenever no custom
 * font is selected or the rasterizer is unavailable. Line-breaking and glyph placement are
 * handled by {@link org.flixelgdx.text.FlixelTextLayout FlixelTextLayout}; you rarely need to
 * touch it directly.
 *
 * @see org.flixelgdx.text.FlixelText
 * @see org.flixelgdx.text.FlixelFontRegistry
 * @see org.flixelgdx.text.FlixelFont
 */
package org.flixelgdx.text;
