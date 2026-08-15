# Bundled `shaderc` binaries

This directory holds the bgfx `shaderc` compiler that the FlixelGDX shader plugin runs at build
time. Each operating system has its own subfolder, named by classifier:

```
tools/
  linux-x86_64/shaderc
  macos-x86_64/shaderc
  macos-aarch64/shaderc
  windows-x86_64/shaderc.exe
  windows-shim/d3d4linux.exe
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

The Direct3D (`dx11`) variants are DXBC, which needs Microsoft's FXC compiler. FXC is native to
Windows, so the Windows `shaderc` emits those variants directly. On Linux and macOS, `shaderc`
produces them through the `d3d4linux` Wine shim, which lives in `windows-shim/` and is extracted
next to the binary at build time (Wine must be installed on the host). When no FXC compiler can run,
the plugin skips just that variant with a warning, the same way the framework's
`scripts/build_shaders.sh` does.

## The `windows-shim/` directory

Holds the bgfx `d3d4linux` shim: `d3d4linux.exe` and `d3dcompiler_47.dll` (the real Windows FXC),
both taken from bgfx's `tools/bin/windows`. They are Windows binaries run under Wine, so one copy
serves both Linux and macOS. The plugin extracts them to `<workDir>/windows/`, where `shaderc`
looks for them relative to its own location.
