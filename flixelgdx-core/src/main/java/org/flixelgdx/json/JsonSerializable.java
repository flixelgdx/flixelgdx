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
package org.flixelgdx.json;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class for automatic, reflection-free JSON mapping.
 *
 * <p>At compile time an annotation processor generates a companion class named
 * {@code <ClassName>JsonSerializer} in the same package, with two static methods:
 * <ul>
 *   <li>{@code String toJson(<ClassName> value)} - turns an instance into a JSON string.</li>
 *   <li>{@code <ClassName> fromJson(FlixelJsonValue json)} - fills a fresh instance from parsed JSON.</li>
 * </ul>
 *
 * <p>Because the mapping code is generated ahead of time, there is no runtime reflection: this both
 * avoids per-call allocation churn and works on platforms (such as the web target) where reflection
 * metadata is expensive or unavailable.
 *
 * <p>Requirements for an annotated class:
 * <ul>
 *   <li>It must have an accessible no-argument constructor.</li>
 *   <li>Mapped fields must be non-{@code private} (the generated class lives in the same package and
 *       reads and writes fields directly). {@code static}, {@code transient}, and {@code final}
 *       fields are skipped.</li>
 *   <li>Supported field types: the primitives, {@link String}, enums (mapped by
 *       {@link Enum#name()}), other {@code @JsonSerializable} types (which map through their own
 *       generated serializer), and arrays (including multidimensional) of those.</li>
 * </ul>
 *
 * <p><b>Multidimensional arrays are jagged, not rectangular.</b> In Java, {@code int[][]} is an
 * array of {@code int[]} references. Each row is a separate heap object and can have a different
 * length. JSON has the same model: every element of an array can itself be an array of any size.
 * The generated serializer round-trips this faithfully, so a grid where every row has the same
 * width will still deserialize correctly. However, nothing in the type or the serializer enforces
 * that constraint. If the JSON is edited by hand so that rows have different lengths, the game
 * will not detect the inconsistency at load time. For structured grids where uniform row width is
 * a correctness requirement (for example, tilemaps), prefer a flat {@code int[]} plus explicit
 * {@code width} and {@code height} fields; you can then validate {@code tiles.length == width *
 * height} right after deserialization and fail early with a clear error.</p>
 *
 * <p>Example:
 * <pre>{@code
 * @JsonSerializable
 * public class SaveData {
 *   public int highScore;
 *   public String playerName;
 *   public boolean tutorialDone;
 * }
 *
 * // Elsewhere, no reflection involved:
 * String text = SaveDataJsonSerializer.toJson(save);
 * SaveData loaded = SaveDataJsonSerializer.fromJson(FlixelJson.parse(text));
 * }</pre>
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface JsonSerializable {
}
