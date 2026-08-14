# Bundled `shaderc` binaries

This directory holds the bgfx `shaderc` compiler that the FlixelGDX shader plugin runs at build
time. Each operating system has its own subfolder, named by classifier:

```
tools/
  linux-x86_64/shaderc
  macos-x86_64/shaderc
  macos-aarch64/shaderc
  windows-x86_64/shaderc.exe
```

The plugin extracts the binary for the current host, then invokes it to cross-compile each GLSL
shader into the per-renderer bytecode the runtime loads. Because this is build-time tooling, its
size does not affect a shipped game.

## Why the compiler is vendored

`shaderc` is bgfx's own shader compiler. It produces the exact binary container format
`bgfx_create_shader` expects (uniform reflection table plus vertex/fragment signature hashes), and
it is the same compiler that builds the framework's own sprite shader. Driving it directly is far
safer than trying to synthesize that format by hand.

## Adding or updating a binary

The binaries are built from a checkout of [bgfx](https://github.com/bkaradzic/bgfx):

```
# From a bgfx checkout, after fetching bx and bimg alongside it:
make tools           # or the platform-specific shaderc target
# The result is under .build/<config>/bin/shaderc[Release]
```

Copy the result into the matching classifier folder and, on Unix, keep the executable bit set.

The Windows and macOS binaries are produced by the temporary
`.github/workflows/build-shaderc.yml` workflow (run it manually from the Actions tab, download the
artifacts, and drop them here). The Linux binary is already vendored.

Only the Windows build can emit the Direct3D (`dx11`) variants, because compiling HLSL to DXBC
needs a Direct3D compiler. On other hosts the plugin skips that variant with a warning, the same
way the framework's `scripts/build_shaders.sh` does.
