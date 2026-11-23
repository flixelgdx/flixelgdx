# Localization: Translating Your Game's Text

`Flixel.locale` shows your game's text in the player's language. You write one JSON file per language, load them,
pick a language, and ask for text by key instead of hard-coding it. This guide covers string files, loading, switching
languages, the fallback chain, filling in values with `{0}` placeholders, and the mistakes to avoid.

## Table of contents

1. [The phrasebook analogy](#the-phrasebook-analogy)
2. [Writing string files](#writing-string-files)
3. [Loading and choosing a language](#loading-and-choosing-a-language)
4. [Showing text](#showing-text)
5. [Placeholders: filling in values](#placeholders-filling-in-values)
6. [The fallback chain](#the-fallback-chain)
7. [Switching language while the game runs](#switching-language-while-the-game-runs)
8. [Missing keys](#missing-keys)
9. [Things to keep in mind](#things-to-keep-in-mind)
10. [Check your understanding](#check-your-understanding)

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
- **No plural rules yet.** For "1 coin" versus "2 coins", use separate keys (`hud.coin` and `hud.coins`) and pick one
  in code.

## Check your understanding

1. The language is `es-MX`, you loaded `es` and `en`, and `menu.play` exists only in `es`. Which text appears?
2. Why does `format(...)` take a `FlixelString` to write into instead of returning a new `String`?
3. A translator writes `"Hola {name}"`. What does the player see?

(Answers: the `es` text, because `es-MX` is not loaded so the chain moves on to `es`; so the call can reuse one buffer
every frame without creating garbage; `Hola {name}` unchanged, because only numbered placeholders are filled.)

**Next:** [Collections and Pooling](collections-and-pooling.md)
