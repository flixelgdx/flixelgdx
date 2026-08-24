# FlixelGDX Basis Universal Plugin

Compress textures once at build time. Load them faster everywhere at runtime.

This Gradle plugin encodes a game's PNG assets into KTX2/Basis Universal supercompressed textures
automatically, so the images stay compressed on the GPU instead of being decoded into raw RGBA8888
data on upload. The plugin bundles a `basisu` encoder binary for every supported platform, so no
external installation is needed: compression is a single Gradle property away.

## Why it exists

A PNG decoded into an `RGBA8888` texture consumes roughly four bytes per pixel on the GPU. A 1024x1024
sprite sheet that looks like 1 MB on disk becomes 4 MB of video memory, and that cost is paid every
time the texture is used. Basis Universal keeps the texture compressed on the GPU and transcodes it to
the best format each device supports (ETC1S on mobile, BCn on desktop, ASTC on newer hardware).

Without this plugin, encoding requires installing the `basisu` command line tool, finding a version
that matches what the framework expects, and running it by hand for every asset update. The plugin
removes all of that: it wires a `compressBasisuTextures` task into the build, picks the right bundled
binary for the host OS and architecture, and makes the compressed output available to Android's asset
merge and the desktop `jar` task automatically.

The runtime side is transparent. `FlixelGame.create()` enables the matching loader on every backend,
so game code keeps requesting `.png` paths and gets the compressed texture back without any changes.

## Applying the plugin

In the platform module's `build.gradle` alongside `com.android.application`, `com.android.library`,
or the desktop `application` plugin:

```groovy
plugins {
  id 'com.android.application'
  id 'org.flixelgdx.basisu' version '<flixel-version>'
}
```

Compression is off by default. Enable it by passing a project property:

```
./gradlew assembleRelease -PenableBasisuCompression=true
```

Or enable it permanently in `gradle.properties`:

```properties
enableBasisuCompression=true
```

## Configuring the extension

```groovy
flixelgdxBasisu {
  // Explicit override. When absent, reads the enableBasisuCompression property (default: false).
  enabled = true

  // Directory scanned recursively for .png files (default: rootProject/assets/).
  assetsDir = file('../assets')

  // Where .ktx2 files are written, mirroring the source layout (default: build/generated/basisuAssets).
  outputDir = file('build/compressedAssets')

  // Generate a full mipmap chain for each texture (default: true).
  generateMipmaps = true

  // Use higher-quality UASTC instead of the default smaller ETC1S mode (default: false).
  useUastc = false

  // ETC1S quality level, 1 (smallest, worst) to 255 (largest, best). Ignored in UASTC mode.
  // Default: 128.
  etc1sQuality = 128

  // UASTC encoding level, 0 (fastest, worst) to 4 (slowest, best). Ignored in ETC1S mode.
  // Default: 2.
  uastcLevel = 2

  // Ant-style glob patterns, relative to assetsDir, for assets that must stay as plain PNGs.
  // A trailing /** excludes an entire folder. Default: empty.
  excludes = [
    'ui/icons/app-icon.png',
    'fonts/**'
  ]
}
```

## How it wires into the build

**Android.** The compressed output directory is added as an extra assets source set so `mergeXxxAssets`
picks up the `.ktx2` files alongside the source assets. Once the merge finishes, any plain PNG that has
a compressed sibling in the merged output is deleted so both formats do not ship.

**Desktop (JVM).** The `jar` task gains a dependency on `compressBasisuTextures` and is configured to
include the compressed output. Any plain PNG that has a compressed sibling in the output directory is
excluded from the JAR, so only the `.ktx2` variant is packaged.

**Development runs.** Running the game via `./gradlew :lwjgl3:run` skips compression: the task reads
PNGs straight from the source assets directory so the development loop stays fast.

## Compression modes

| Mode  | Flag              | Size     | Quality   | Best for                           |
|-------|-------------------|----------|-----------|------------------------------------|
| ETC1S | (default)         | Smallest | Good      | Most 2D sprites and backgrounds    |
| UASTC | `useUastc = true` | Larger   | Excellent | Highly detailed textures or UI art |

ETC1S is adequate for most 2D sprite work. UASTC produces noticeably larger output but preserves
fine detail that ETC1S's block compression can lose.

## Excluding assets from compression

Some PNGs must stay uncompressed, for example bitmap font pages where the runtime reads exact pixel
values to compute glyph metrics. Use the `excludes` list for those:

```groovy
flixelgdxBasisu {
  excludes = [
    'fonts/**',          // all bitmap font textures
    'ui/cursor.png'      // a single file
  ]
}
```

Excluded files are left as-is in the source assets directory and pass through the build unchanged.

## Supported platforms

The plugin bundles encoder binaries for:

| OS      | Architectures    |
|---------|------------------|
| Linux   | x64 (SSE), arm64 |
| macOS   | x64 (SSE), arm64 |
| Windows | x64 (SSE)        |

Binaries are cached in `~/.gradle/caches/flixelgdx-basisu/` after first extraction. The cache
directory includes a hash of the binary bytes, so updating the plugin version automatically
re-extracts without any manual cleanup.
