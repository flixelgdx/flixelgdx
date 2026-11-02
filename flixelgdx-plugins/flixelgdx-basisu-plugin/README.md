# FlixelGDX Basis Universal Plugin

Compress textures once at build time. Load them faster and keep them small on Android at runtime.

This Gradle plugin encodes a game's PNG assets into KTX2/Basis Universal supercompressed textures
automatically, so the images stay compressed on the GPU instead of being decoded into raw RGBA8888
data on upload. The plugin bundles a `basisu` encoder binary for every supported platform, so no
external installation is needed: compression is a single Gradle property away.

## Why it exists

A PNG decoded into an `RGBA8888` texture consumes roughly four bytes per pixel on the GPU. A 1024x1024
sprite sheet that looks like 1 MB on disk becomes 4 MB of video memory, and that cost is paid every
time the texture is used. Basis Universal stores the texture in a "supercompressed" form that is even
smaller than the GPU formats, and the game transcodes it at load time to the best compressed format the
device's GPU supports (ASTC or ETC2 on Android), so it stays compressed in video memory too.

Without this plugin, encoding requires installing the `basisu` command line tool, finding a version
that matches what the framework expects, and running it by hand for every asset update. The plugin
removes all of that: it wires a `compressBasisuTextures` task into the build, picks the right bundled
binary for the host OS and architecture, and makes the compressed output available to Android's asset
merge automatically.

The runtime side is transparent. The Android backend registers a `.ktx2` loader at launch and
`FlixelGame.create()` turns compressed texture lookup on, so game code keeps requesting `.png` paths
and gets the compressed texture back without any changes. Desktop and web builds are not affected and
keep loading the plain PNGs, since only the Android backend ships the Basis Universal transcoder.

## Applying the plugin

In the Android module's `build.gradle` alongside `com.android.application` or `com.android.library`:

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
basisu {
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

**Desktop and web.** Nothing changes: those builds package and load the plain PNGs.

## What happens at runtime

When a game asks for `images/player.png` and `images/player.ktx2` exists, the Android backend loads
the `.ktx2` file instead. On an asset loading thread, it transcodes the file to the best format the
GPU supports, then uploads it on the render thread:

| Source mode | First choice                     | Then           | Last resort |
|-------------|----------------------------------|----------------|-------------|
| ETC1S       | ETC2 (8 bpp, or 4 bpp if opaque) | ASTC 4x4, BC7  | RGBA8       |
| UASTC       | ASTC 4x4 (8 bpp)                 | BC7, ETC2      | RGBA8       |

Each first choice is lossless for its source mode. RGBA8 (32 bpp) is only used on a GPU without any
compressed format; the file is still smaller in the APK.

A few details are worth knowing:

- **Sizes that are not a multiple of 4.** Compressed formats store 4x4 pixel blocks, so a 50x37 image
  becomes a 52x40 texture on the GPU. The texture still reports 50x37 and texture coordinates are scaled
  to match, so frames, animations and atlases keep working unchanged.
- **Mipmaps.** The mipmaps from `generateMipmaps` are uploaded too, which keeps scaled-down sprites
  smooth. When the size is not a multiple of 4, only the levels that line up with the rounded-up size are
  kept (often the first two or three).
- **No pixel updates.** A compressed texture cannot be changed with `FlixelTexture.update(...)`. Exclude
  any image the game draws into at runtime.

## Compression modes

| Mode  | Flag              | Size on disk | Quality   | Best for                           |
|-------|-------------------|--------------|-----------|------------------------------------|
| ETC1S | (default)         | Smallest     | Good      | Most 2D sprites and backgrounds    |
| UASTC | `useUastc = true` | Larger       | Excellent | Highly detailed textures or UI art |

ETC1S is adequate for most 2D sprite work. UASTC produces noticeably larger output but preserves
fine detail that ETC1S's block compression can lose.

## Excluding assets from compression

Some PNGs must stay uncompressed, for example bitmap font pages where the runtime reads exact pixel
values to compute glyph metrics. Use the `excludes` list for those:

```groovy
basisu {
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
