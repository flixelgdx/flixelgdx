/**
 * Localization: showing the game's text in the player's language.
 *
 * <p>Each language gets a {@link org.flixelgdx.locale.FlixelLocalizationTable FlixelLocalizationTable}, a phrasebook
 * of keys (such as {@code "menu.play"}) mapped to translated text. The global
 * {@link org.flixelgdx.locale.FlixelLocalization FlixelLocalization}, reachable as
 * {@link org.flixelgdx.Flixel#locale Flixel.locale}, loads those tables from JSON, tracks the current
 * language, and searches a fallback chain when a string is missing.
 *
 * <h2>Core workflow</h2>
 *
 * <ol>
 *   <li><b>Write</b> one JSON file per language, such as {@code lang/en.json} and {@code lang/es.json}.
 *     Nested objects become dotted keys.</li>
 *   <li><b>Load</b> each file with
 *     {@link org.flixelgdx.locale.FlixelLocalization#load(String, org.flixelgdx.file.FlixelFile) load(...)}
 *     during boot or a loading screen.</li>
 *   <li><b>Pick</b> the language with
 *     {@link org.flixelgdx.locale.FlixelLocalization#setLocale(String) setLocale(...)}, for example from a
 *     settings menu or a saved preference.</li>
 *   <li><b>Show</b> text with {@link org.flixelgdx.locale.FlixelLocalization#get(String) get(...)}, or with
 *     {@link org.flixelgdx.locale.FlixelLocalization#format(org.flixelgdx.util.FlixelString, String, CharSequence) format(...)}
 *     when it has {@code {0}}-style placeholders, or with
 *     {@link org.flixelgdx.locale.FlixelLocalization#plural(org.flixelgdx.util.FlixelString, String, long) plural(...)}
 *     when its wording depends on a count.</li>
 * </ol>
 *
 * <h2>Plurals</h2>
 *
 * <p>A JSON object whose keys are all {@link org.flixelgdx.locale.FlixelPluralCategory plural category}
 * names (and includes {@code other}) becomes a plural entry. You register a
 * {@link org.flixelgdx.locale.FlixelPluralRule FlixelPluralRule} for each language tag; the framework
 * never guesses one from the tag, so you stay in control of which rule each language uses.
 *
 * <pre>{@code
 * // "hud": { "coins": { "one": "{0} coin", "other": "{0} coins" } }
 * Flixel.locale.setPluralRule("en", FlixelPluralRule.ONE_OTHER);
 * coinsText.setText(Flixel.locale.plural(coinsLine, "hud.coins", coins));
 * }</pre>
 *
 * <pre>{@code
 * // lang/en.json: { "menu": { "play": "Play" }, "hud": { "coins": "Coins: {0}" } }
 * Flixel.locale.load("en", Flixel.files.internal("lang/en.json"));
 * Flixel.locale.setLocale(Flixel.save.getString("language", "en"));
 *
 * playButton.setText(Flixel.locale.get("menu.play"));
 * coinsText.setText(Flixel.locale.format(coinsLine, "hud.coins", coinsValue.set(coins)));
 * }</pre>
 *
 * <h2>Things to keep in mind</h2>
 * <ul>
 *   <li>Lookups and formatting are allocation-free. Loading and switching languages allocate, so keep
 *     them out of per-frame code.</li>
 *   <li>A missing key returns the key itself and logs a warning once, so untranslated text is easy to
 *     spot while testing.</li>
 *   <li>Your font must contain the glyphs the language needs. Translating to Japanese with a
 *     Latin-only font shows empty boxes.</li>
 *   <li>Use from the main game thread only.</li>
 * </ul>
 *
 * @see org.flixelgdx.locale.FlixelLocalization
 * @see org.flixelgdx.locale.FlixelLocalizationTable
 * @see org.flixelgdx.locale.FlixelPluralRule
 */
package org.flixelgdx.locale;
