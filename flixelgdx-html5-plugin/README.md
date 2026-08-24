# FlixelGDX HTML5 Plugin

One Gradle task turns a game into a playable web app.

This plugin sits on top of the `org.teavm` plugin and fills in everything TeaVM leaves out: it copies
game assets, generates an `index.html` that boots the game, handles the WebAssembly GC loader, builds
an asset preload manifest, copies compiled web shader variants, and provides a `run` task with an
embedded HTTP server so the game opens in a browser with a single command.

## Why it exists

TeaVM compiles Java to a JavaScript or WebAssembly bundle, but that bundle is only one piece of a
deployable web app. A browser also needs an `index.html` to load the bundle, the game's assets served
from a predictable path, a preload manifest so the web backend knows which files to fetch before the
game starts, and a functioning HTTP server that sets the correct MIME types (browsers refuse to
execute WebAssembly served as `text/plain`). Setting all of that up by hand on every project is
repetitive and error-prone. The plugin wires the whole chain together so a normal Gradle build
produces a complete, deployable web app.

## Applying the plugin

Apply it alongside `org.teavm` in the web module:

```groovy
plugins {
  id 'org.teavm' version '0.13.0'
  id 'org.flixelgdx.html5' version '<flixel-version>'
}

teavm {
  all {
    mainClass = 'com.mygame.web.WebLauncher'
  }
  js {
    addedToWebApp = true
    obfuscated = true
  }
  wasmGC {
    addedToWebApp = true
    obfuscated = true
  }
}
```

`org.teavm` must come before `org.flixelgdx.html5` so the plugin can find the TeaVM extension.

## Running the game

```
./gradlew :html5:run
```

This builds the web app, starts the dev server at `http://localhost:8080`, and opens that URL in
the default browser automatically. Use `debug` instead of `run` to open in debug mode:

```
./gradlew :html5:debug
```

The debug task starts the same server but appends `?flixel.mode=debug` to the URL, telling the
runtime to enable debug logging and diagnostics.

Press `Ctrl+C` in the terminal to stop the server.

## Packaging for deployment

```
./gradlew :html5:package
```

Produces a `<name>-html5.zip` archive in `dist/` at the project root, ready to upload to any
static web host or CDN. The archive is self-contained: unzip it and point a web server at the
resulting directory.

## Configuring the extension

```groovy
flixelgdx {
  // Title shown in the browser tab (default: "My FlixelGDX Game").
  title = 'My Game'

  // ID of the <canvas> element the launcher binds to (default: "flixelgdx-canvas").
  canvasId = 'game-canvas'

  // Runtime mode baked into the page as the default (default: none, which means release mode).
  // The ?flixel.mode= URL parameter overrides this at load time.
  mode = FlixelHtml5Mode.DEBUG

  // Port the run/debug dev server listens on (default: 8080).
  devServerPort = 9000

  // Source directory for game assets (default: rootProject/assets/).
  assetsDir = file('../assets')

  // Source directory for user-provided web resources (default: src/main/webapp/).
  // Files here are copied verbatim into the web output. An index.html here suppresses
  // auto-generation.
  webappDir = file('src/main/webapp')

  // Set to false to skip index.html auto-generation entirely (default: true).
  generateDefaultIndexHtml = true

  // Provide a hand-crafted index.html instead of the generated default.
  customIndexHtml = file('src/main/webapp/index.html')

  // Provide a favicon that is copied to the output and linked in the generated index.html.
  customFavicon = file('src/main/webapp/favicon.ico')
}
```

## JavaScript and WebAssembly

The generated `index.html` prefers the WebAssembly GC build for speed and falls back to the
JavaScript build automatically when the browser does not support WebAssembly GC. Enable whichever
targets you want in the `teavm` block; the plugin detects which are present and generates the
loader accordingly:

```groovy
teavm {
  all { mainClass = 'com.mygame.web.WebLauncher' }
  // JavaScript fallback
  js { 
    addedToWebApp = true 
  }
  // WebAssembly GC preferred target
  wasmGC { 
    addedToWebApp = true 
  }
}
```

Omitting a target simply removes it from the loader. A JS-only build works without any changes;
so does a WASM-only build (the fallback path just never triggers).

## Custom index.html

If the generated page does not meet the game's needs, supply a custom one in two ways:

**Via `webappDir` (recommended).** Place an `index.html` in `src/main/webapp/`. The plugin copies
it verbatim into the web output and skips auto-generation. Other files in that directory (scripts,
stylesheets, extra images) are copied alongside it.

**Via `customIndexHtml`.** Point directly at a file anywhere on disk. The file is copied as
`index.html` regardless of where `webappDir` is. No placeholder substitution is applied; the full
HTML is the developer's responsibility, including loading the TeaVM bundle.

## Asset manifest

The plugin writes `assets/assets.txt` into the web output, listing every file under the assets
directory one per line. The web backend reads this at startup and preloads all of them before the
game's `create()` method is called, so assets are available synchronously (matching the behavior of
the desktop and Android backends).

## Shaders

If the shader plugin is on the classpath, the plugin also runs `copyShaders`, which copies the ESSL
shader variants out of the JAR resources and into the web assets tree so the browser can fetch them
as regular files. This happens automatically; no extra configuration is needed.

## Tasks registered

| Task                    | Group       | Description                                               |
|-------------------------|-------------|-----------------------------------------------------------|
| `copyAssets`            | flixelgdx   | Copies game assets into the web output.                   |
| `copyWebApp`            | flixelgdx   | Copies user-provided web resources into the web output.   |
| `copyShaders`           | flixelgdx   | Copies compiled ESSL shader variants into the web assets. |
| `generateAssetManifest` | flixelgdx   | Writes `assets/assets.txt` for the web preloader.         |
| `generateIndexHtml`     | flixelgdx   | Generates `index.html` from the built-in template.        |
| `run`                   | application | Builds the web app and starts the dev server.             |
| `debug`                 | application | Same as `run`, but opens in debug mode.                   |
| `package`               | application | Zips the web output into `dist/<name>-html5.zip`.         |
