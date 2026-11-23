# Localization: Translating Your Game's Text

`Flixel.locale` shows your game's text in the player's language. You write one JSON file per language, load them,
pick a language, and ask for text by key instead of hard-coding it. This guide covers string files, loading, switching
languages, the fallback chain, filling in values with `{0}` placeholders, plurals, and the mistakes to avoid.

## Table of contents

1. [The phrasebook analogy](#the-phrasebook-analogy)
2. [Writing string files](#writing-string-files)
3. [Loading and choosing a language](#loading-and-choosing-a-language)
4. [Showing text](#showing-text)
5. [Placeholders: filling in values](#placeholders-filling-in-values)
6. [Plurals: "1 coin" versus "2 coins"](#plurals-1-coin-versus-2-coins)
7. [The fallback chain](#the-fallback-chain)
8. [Switching language while the game runs](#switching-language-while-the-game-runs)
9. [Missing keys](#missing-keys)
10. [Things to keep in mind](#things-to-keep-in-mind)
11. [Check your understanding](#check-your-understanding)

## The phrasebook analogy

Imagine a traveler with a stack of phrasebooks. They never memorize "Where is the station?" in every language; they
remember the *idea* ("ask for the station") and look it up in the book for wherever they are.

Your game works the same way:

- A **key** such as `menu.play` is the idea.
- A **localization table** (`FlixelLocalizationTable`) is one phrasebook: every key translated into one language.
- `Flixel.locale` (a `FlixelLocalization`) is the traveler: it knows which language you are in and which book to open.

## Writing string files

Put one JSON file per language in your assets, for example `assets/lang/en.json`:

```json
{
  "menu": {
    "play": "Play",
    "quit": "Quit"
  },
  "hud": {
    "score": "Score: {0}",
    "defeated": "{0} defeated {1}!"
  },
  "intro": [
    "Long ago, the stars went out.",
    "One hero set out to light them again."
  ]
}
```

and `assets/lang/es.json`:

```json
{
  "menu": { "play": "Jugar", "quit": "Salir" },
  "hud": { "score": "Puntos: {0}", "defeated": "¡{0} derrotó a {1}!" }
}
```

Nested objects become **dotted keys**: the file above has `menu.play`, `menu.quit`, `hud.score`, and `hud.defeated`.
Arrays use the index as the last part, so the intro lines are `intro.0` and `intro.1`. Numbers and booleans are stored
as text, and `null` values are skipped.

Save the files as UTF-8 so accented letters and other scripts survive.

## Loading and choosing a language

Load the files during boot or on a loading screen, then pick a language:

```java
Flixel.locale.load("en", Flixel.files.internal("lang/en.json"));
Flixel.locale.load("es", Flixel.files.internal("lang/es.json"));

// Remember the player's choice between sessions.
Flixel.locale.setLocale(Flixel.save.getString("language", "en"));
```

The first argument is a **language tag**: `en`, `es`, `pt-BR`, `zh-Hant-TW`, and so on. Underscores are treated as
hyphens (`pt_BR` is the same as `pt-BR`), but letter case is kept, so pick one style and use it everywhere.

Loading the same tag twice **merges** the files, with later files overriding matching keys. That lets you split a
large game into `lang/en/menus.json`, `lang/en/dialogue.json`, and so on.

If you already have JSON text in memory, use `Flixel.locale.loadJson("en", text)`. To build a table in code, create a
`FlixelLocalizationTable`, `put(...)` strings into it, and pass it to `Flixel.locale.add(table)`.

## Showing text

For text that is set once, such as a menu button, use `get(...)`:

```java
playButton.setText(Flixel.locale.get("menu.play"));
```

`get(key, defaultValue)` returns your default instead of the key when the string is missing, and `has(key)` tells you
whether any loaded language has it.

## Placeholders: filling in values

Many strings contain values: a score, a name, a count. Write `{0}`, `{1}`, and so on where the values go, then fill
them in with `format(...)`. It writes into a reusable `FlixelString`, so it creates no garbage and is safe to call every
frame:

```java
private final FlixelString scoreValue = new FlixelString(16);
private final FlixelString scoreLine = new FlixelString(32);

@Override
public void update(float elapsed) {
  super.update(elapsed);
  scoreValue.set(score); // Turn the number into text without creating a String.
  scoreText.setText(Flixel.locale.format(scoreLine, "hud.score", scoreValue));
}
```

Placeholders are numbered rather than positional, so a translation can **reorder** them. English says
`"{0} defeated {1}!"`, while another language is free to write `"{1} was defeated by {0}!"`, and your code does not
change:

```java
Flixel.locale.format(line, "hud.defeated", playerName, bossName);
```

The rules:

- `{{` writes a single `{`, and `}}` writes a single `}`.
- A placeholder with no matching value (such as `{3}` when you passed two values, or `{name}`) is left as-is, so a
  mistake stays visible.
- A `null` value writes `null`.

The same formatting is available on any `FlixelString`, even without localization:

```java
label.setFormat("{0} / {1}", current, max);       // Replaces the contents.
label.concatFormat(" ({0})", status);             // Appends.
```

The one, two, and three value versions are allocation-free. Passing four or more values uses a varargs array, which
allocates, so avoid that in per-frame code.

## Plurals: "1 coin" versus "2 coins"

Words change with the count, and every language does it differently. English has two forms ("1 coin", "2 coins"),
Japanese has one, Polish has three or more, and Arabic has six. So the choice of form cannot live in your code; it
lives in the string file and in a **plural rule** for each language.

Write the forms as an object whose keys are the standard plural category names: `zero`, `one`, `two`, `few`, `many`,
and `other`. Every plural entry needs `other`, which is used whenever a form is missing:

```json
{
  "hud": {
    "coins": { "one": "{0} coin", "other": "{0} coins" }
  }
}
```

```json
{
  "hud": {
    "coins": { "one": "{0} moneta", "few": "{0} monety", "many": "{0} monet", "other": "{0} monety" }
  }
}
```

Then register a rule for each language tag and call `plural(...)`. The count picks the form and is written into
`{0}` for you, so you do not need a second buffer for the number:

```java
Flixel.locale.setPluralRule("en", FlixelPluralRule.ONE_OTHER);
Flixel.locale.setPluralRule("pl", FlixelPluralRule.POLISH);

coinText.setText(Flixel.locale.plural(coinLine, "hud.coins", coins)); // "1 coin", "5 coins", "3 monety"...
```

Extra values come after the count: `plural(out, "hud.found", keys, areaName)` fills `{0}` with the count and `{1}`
with the area name.

A few things to know:

- **You choose the rules.** The framework never guesses a rule from a language tag, so your tags always pair with the
  rules you picked. The Unicode CLDR project publishes the plural rules for every language if you need to look one up.
  `FlixelPluralRule` ships ready-made rules for most languages, each listing the tags it fits in its Javadoc:
  `ONE_OTHER` (English, German, and many more), `OTHER_ONLY` (Japanese, Chinese, Korean), `HINDI`, `FRENCH`,
  `SPANISH`, `EAST_SLAVIC` (Russian, Ukrainian), `SOUTH_SLAVIC` (Croatian, Serbian), `POLISH`, `CZECH`, `SLOVENIAN`,
  `ICELANDIC`, `LITHUANIAN`, `LATVIAN`, `ROMANIAN`, `HEBREW`, `ARABIC`, `MALTESE`, `IRISH`, `SCOTTISH_GAELIC`,
  `WELSH`, and `FILIPINO`. Write your own lambda for anything else.
- **Tags without a rule** use the rule of their shorter form (a rule for `pt` also covers `pt-BR`), and then the
  default rule, which is `ONE_OTHER` unless you change it with `setDefaultPluralRule(...)`.
- **Fallback text uses its own language's rule.** If a Polish player gets English fallback text, the English rule picks
  the form, so it never asks English for a `few` form it does not have.
- **It is fast.** Plural entries are stored as a small array per key when the file loads, and each language's rule is
  looked up ahead of time, so `plural(...)` creates no garbage. Rules you write must not allocate either.
- An object only counts as a plural entry when **every** key is a category name and `other` is present. Anything else
  flattens into dotted keys as usual.

## The fallback chain

Translations are rarely complete. When a key is missing, `Flixel.locale` looks further down a **fallback chain**
instead of failing. With the language set to `pt-BR` and the default fallback of `en`, it checks:

1. `pt-BR` (Brazilian Portuguese),
2. `pt` (Portuguese in general), by dropping the last part of the tag,
3. `en`, the fallback language.

The first language that has the key wins, and languages you never loaded are skipped. So you can ship a full `pt.json`
plus a tiny `pt-BR.json` with only the words that differ, and anything a translator has not finished yet still shows up
in English.

Change the fallback with `Flixel.locale.setFallback("ja")` if your game was written in another language first.

## Switching language while the game runs

`setLocale(...)` takes effect immediately for every later `get(...)` and `format(...)`. Text that is rebuilt every
frame updates on its own. For text you set once, listen to `onLocaleChanged`:

```java
Flixel.locale.onLocaleChanged.add(tag -> {
  playButton.setText(Flixel.locale.get("menu.play"));
  quitButton.setText(Flixel.locale.get("menu.quit"));
});
```

Often the simplest approach is to switch states (or restart the current one) after the player picks a language, so
every label is created again in the new language.

Remove the listener when its state is destroyed, as with any global signal (see [Signals](signals.md)).

## Missing keys

When no language in the chain has a key, `get(...)` returns the **key itself**, so the screen shows `menu.plya` instead
of crashing or showing nothing. It also logs a warning the first time each missing key is asked for. Turn the warning
off with `Flixel.locale.setWarnMissing(false)`.

## Things to keep in mind

- **Load early, look up anywhere.** Loading and `setLocale(...)` allocate; `get(...)` and `format(...)` do not.
- **Fonts need the right glyphs.** A Latin-only font shows empty boxes for Japanese, Korean, Chinese, Arabic, and
  other scripts. Register a font that covers the languages you ship (see [Text, HUDs, and Bars](text-and-ui.md)).
- **Leave room.** Translations are often longer than English. German and French text can easily be a third longer.
- **Main thread only.** `FlixelLocalization` is not thread-safe.

## Check your understanding

1. The language is `es-MX`, you loaded `es` and `en`, and `menu.play` exists only in `es`. Which text appears?
2. Why does `format(...)` take a `FlixelString` to write into instead of returning a new `String`?
3. A translator writes `"Hola {name}"`. What does the player see?
4. You set the language to `pl` but forgot `setPluralRule("pl", ...)`. What happens to `plural(out, "hud.coins", 5)`?

(Answers: the `es` text, because `es-MX` is not loaded so the chain moves on to `es`; so the call can reuse one buffer
every frame without creating garbage; `Hola {name}` unchanged, because only numbered placeholders are filled; the
default `ONE_OTHER` rule picks `other`, so the player sees `5 monety` instead of the correct `5 monet`.)

**Next:** [Collections and Pooling](collections-and-pooling.md)
