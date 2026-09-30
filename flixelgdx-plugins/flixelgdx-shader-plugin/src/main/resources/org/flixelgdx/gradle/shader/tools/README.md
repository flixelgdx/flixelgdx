# Bundled shader tools

This directory holds the command-line compilers the FlixelGDX shader plugin runs at build time. Each
operating system has its own subfolder, named by classifier:

```
tools/
  linux-aarch64/{glslang, spirv-cross, shaderc}
  linux-x86_64/{glslang, spirv-cross, shaderc}
  macos-aarch64/{glslang, spirv-cross, shaderc}
  windows-aarch64/{glslang.exe, spirv-cross.exe, shaderc.exe}
  windows-x86_64/{glslang.exe, spirv-cross.exe, shaderc.exe}
  windows-shim/{d3d4linux.exe, d3dcompiler_47.dll}
```

The plugin extracts the binaries for the current host, then invokes them: `glslang` compiles each
shader to SPIR-V, `spirv-cross` translates that into ESSL for the web and Android backends, and
`shaderc` produces the per-renderer bytecode the desktop backend loads. Because this is build-time
tooling, its size does not affect a shipped game.

## `glslang` and `spirv-cross`

Both come from the Khronos Group and are built from source:

- [glslang](https://github.com/KhronosGroup/glslang) tag `16.6.0`, target `glslang-standalone`,
  configured with `-DENABLE_OPT=OFF -DENABLE_HLSL=OFF` (the plugin needs neither the optimizer nor
  the HLSL front end, and leaving them out keeps the binary small).
- [SPIRV-Cross](https://github.com/KhronosGroup/SPIRV-Cross) tag `vulkan-sdk-1.4.363.0`, target
  `spirv-cross`.

Every binary links the C and C++ runtimes statically (`-static-libstdc++ -static-libgcc` on Linux,
the `MultiThreaded` runtime on Windows), so it runs on a clean machine with nothing else installed.
The Linux builds are made on Ubuntu 22.04 so they only need glibc 2.34 or newer. To update them,
build each classifier with those settings (a GitHub Actions matrix over `ubuntu-22.04`,
`ubuntu-22.04-arm`, `macos-14`, `windows-2022`, and `windows-11-arm` covers all five), strip the
non-Windows binaries, and replace the files here.

## Why `shaderc` is vendored

`shaderc` is bgfx's own shader compiler. It produces the exact binary container format
`bgfx_create_shader` expects (uniform reflection table plus vertex/fragment signature hashes), and
it is the same compiler that builds the framework's own sprite shader. Driving it directly is far
safer than trying to synthesize that format by hand.

## Building `shaderc`

The `shaderc` binaries are built from a checkout of [bgfx](https://github.com/bkaradzic/bgfx).

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
