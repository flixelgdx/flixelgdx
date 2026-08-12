/**
 * FlixelGDX's own reflection-free JSON parser and writer.
 *
 * <p>{@link org.flixelgdx.json.FlixelJson FlixelJson} parses text into a
 * {@link org.flixelgdx.json.FlixelJsonValue FlixelJsonValue} tree with typed,
 * default-friendly getters. Because no reflection is involved, the same code runs unchanged on
 * desktop, Android, web, and native images without any configuration files or keep rules.
 *
 * <h2>Reading JSON</h2>
 * <pre>{@code
 * String text = Flixel.files.internal("data/config.json").readString();
 * FlixelJsonValue root = FlixelJson.parse(text);
 *
 * int gravity  = root.getInt("gravity", 600);   // 600 if key is missing
 * float speed  = root.getFloat("speed",  200f);
 * String title = root.getString("title", "Game");
 *
 * // Iterate an array:
 * FlixelJsonValue levels = root.get("levels");
 * for (int i = 0; i < levels.size(); i++) {
 *   FlixelJsonValue level = levels.get(i);
 *   Flixel.info("Level: " + level.getString("name", "?"));
 * }
 * }</pre>
 *
 * <h2>Writing JSON</h2>
 * <p>{@link org.flixelgdx.json.FlixelJsonWriter FlixelJsonWriter} builds JSON text incrementally.
 * Types that implement {@link org.flixelgdx.json.JsonSerializable JsonSerializable} define their
 * own layout, keeping serialization logic close to the data it describes.
 *
 * <h2>Relaxed input format</h2>
 * <p>The parser accepts standard JSON plus trailing commas and single-quoted strings, both of
 * which appear in common level editor and tool exports.
 *
 * @see org.flixelgdx.json.FlixelJson
 * @see org.flixelgdx.json.FlixelJsonValue
 * @see org.flixelgdx.json.FlixelJsonWriter
 */
package org.flixelgdx.json;
