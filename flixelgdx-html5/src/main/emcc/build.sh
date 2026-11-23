#!/usr/bin/env bash
# Rebuilds the stb_truetype WebAssembly module that the web font rasterizer loads.
#
# Requires Emscripten (emcc) on the PATH. The checked-in files were built with emcc 3.1.6.
# Run this after changing stb_wrapper.c, then commit both output files.
set -euo pipefail

cd "$(dirname "$0")"
out="../resources/META-INF/wasm"

# Every function the Java bindings in StbWasm call must be listed here, plus malloc and free.
# Memory starts at 16 MB and grows as fonts are opened, because every font file is copied into
# it and large fonts (Japanese and Chinese ones are often 10 MB or more) would not fit otherwise.
emcc stb_wrapper.c -O3 -s WASM=1 \
  -s ALLOW_MEMORY_GROWTH=1 \
  -s EXPORTED_FUNCTIONS='["_stb_init","_stb_metrics","_stb_rasterize","_stb_advance","_stb_free_bitmap","_stb_free_ctx","_malloc","_free"]' \
  -s EXPORTED_RUNTIME_METHODS='["getValue","setValue"]' \
  -o "$out/stb_truetype_wasm.js"

echo "Wrote $out/stb_truetype_wasm.js and $out/stb_truetype_wasm.wasm"
