/*
 * MIT License
 *
 * Copyright (c) 2026 stringdotjar
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

/*
 * Simple wrapper for the stb_truetype library. The code in here was compiled to
 * JavaScript and WebAssembly via Emscripten.
 */

#define STB_TRUETYPE_IMPLEMENTATION
#include "stb_truetype.h"
#include "stdlib.h"

typedef struct {
  stbtt_fontinfo info;
  float scale;
} FontCtx;

FontCtx* stb_init(unsigned char* data, float pixel_height) {
  FontCtx* ctx = (FontCtx*) malloc(sizeof(FontCtx));
  if (!stbtt_InitFont(&ctx->info, data, 0)) {
    free(ctx);
    return 0;
  }
  ctx->scale = stbtt_ScaleForPixelHeight(&ctx->info, pixel_height);
  return ctx;
}

void stb_metrics(FontCtx* ctx, float* metrics_out) {
  int a, d, l;
  stbtt_GetFontVMetrics(&ctx->info, &a, &d, &l);
  metrics_out[0] = a * ctx->scale;
  metrics_out[1] = d * ctx->scale;
  metrics_out[2] = l * ctx->scale;
}

unsigned char* stb_rasterize(FontCtx* ctx, int codepoint, int* metrics_out) {
  return stbtt_GetCodepointBitmap(&ctx->info, ctx->scale, ctx->scale, codepoint, &metrics_out[0],
      &metrics_out[1], &metrics_out[2], &metrics_out[3]);
}

void stb_free_bitmap(unsigned char* bitmap) {
  stbtt_FreeBitmap(bitmap, 0);
}

void stb_free_ctx(FontCtx* ctx) {
  free(ctx);
}
