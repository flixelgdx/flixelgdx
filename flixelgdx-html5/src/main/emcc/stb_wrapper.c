#define STB_TRUETYPE_IMPLEMENTATION
#include "stb_truetype.h"
#include "stdlib.h"

typedef struct {
  stbtt_fontinfo info;
  float scale;
} FontCtx;

FontCtx* stb_init(unsigned char* data, float pixel_height) {
  FontCtx* ctx (FontCtx*) malloc(sizeof(FontCtx));
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
