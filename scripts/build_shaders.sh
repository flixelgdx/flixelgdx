#!/usr/bin/env bash
#
# Compiles the desktop backend's bgfx sprite shaders into per-renderer bytecode and drops the
# results into the module's bundled resources. Run from the repository root:
#
#   ./scripts/build_shaders.sh
#
# Requirements:
#   - bgfx's shader compiler, "shaderc", on your PATH (or set SHADERC to its full path).
#   - bgfx's shader include directory (the folder that contains bgfx_shader.sh), set via
#     BGFX_SHADER_INCLUDE. When bgfx is checked out next to this repo it is usually
#     "../bgfx/src".
#   - The Direct3D (dx11) profiles additionally need Microsoft's fxc, so they build only on
#     Windows (or under Wine). They are skipped elsewhere with a notice.
#
# The compiled .bin files are committed so packaged games render with no extra setup. Only rebuild
# when the .sc sources change. FlixelBgfxGraphics picks the matching folder at runtime from the
# active bgfx renderer (glsl, spirv, metal, or dx11).
set -euo pipefail

SHADER_SRC="flixelgdx-desktop/src/main/shaders"
OUT_ROOT="flixelgdx-desktop/src/main/resources/org/flixelgdx/shaders"
VARYING="${SHADER_SRC}/varying.def.sc"

SHADERC="${SHADERC:-shaderc}"
if ! command -v "${SHADERC}" >/dev/null 2>&1; then
  echo "Could not find shaderc. Install bgfx's shaderc and put it on PATH, or set SHADERC." >&2
  exit 1
fi
if [ -z "${BGFX_SHADER_INCLUDE:-}" ] || [ ! -f "${BGFX_SHADER_INCLUDE}/bgfx_shader.sh" ]; then
  echo "Set BGFX_SHADER_INCLUDE to bgfx's shader include dir (contains bgfx_shader.sh)." >&2
  exit 1
fi

# host platform for shaderc's --platform flag on the non-Windows profiles.
case "$(uname -s)" in
  Linux*)  HOST_PLATFORM="linux" ;;
  Darwin*) HOST_PLATFORM="osx" ;;
  *)       HOST_PLATFORM="linux" ;;
esac

# compile <dir> <platform> <vs_profile> <fs_profile>
compile() {
  local dir="$1" platform="$2" vsp="$3" fsp="$4"
  local out="${OUT_ROOT}/${dir}"
  mkdir -p "${out}"
  echo "Building ${dir} shaders..."
  "${SHADERC}" -f "${SHADER_SRC}/vs_sprite.sc" -o "${out}/vs_sprite.bin" \
    --type vertex --platform "${platform}" -p "${vsp}" \
    --varyingdef "${VARYING}" -i "${BGFX_SHADER_INCLUDE}"
  "${SHADERC}" -f "${SHADER_SRC}/fs_sprite.sc" -o "${out}/fs_sprite.bin" \
    --type fragment --platform "${platform}" -p "${fsp}" \
    --varyingdef "${VARYING}" -i "${BGFX_SHADER_INCLUDE}"
}

# OpenGL / GLSL (desktop GL fallback).
compile "glsl"  "${HOST_PLATFORM}" "120" "120"

# Vulkan (SPIR-V).
compile "spirv" "${HOST_PLATFORM}" "spirv" "spirv"

# Metal (macOS).
compile "metal" "osx" "metal" "metal"

# Direct3D 11/12 (Windows only; needs fxc).
if [ "$(uname -s)" = "Linux" ] || [ "$(uname -s)" = "Darwin" ]; then
  echo "Skipping dx11 shaders: fxc is only available on Windows (or Wine)."
else
  compile "dx11" "windows" "vs_5_0" "ps_5_0"
fi

echo "Done. Compiled shaders are under ${OUT_ROOT}."
