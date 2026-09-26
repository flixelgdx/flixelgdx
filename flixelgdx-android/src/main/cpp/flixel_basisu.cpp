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
 * JNI bridge for the Basis Universal KTX2 transcoder.
 *
 * Each Java-side handle (jlong) is a raw pointer to a heap-allocated ktx2_transcoder. The caller
 * is responsible for calling close() when done; leaking a handle leaks native memory. All
 * functions return 0, false, or JNI_FALSE on failure rather than throwing or crashing the JVM.
 *
 * The Java class that declares the matching native methods is:
 *   org.flixelgdx.backend.android.graphics.FlixelBasisu
 */

#include <jni.h>
#include <android/log.h>
#include <cstdint>
#include <cstring>
#include <new>

// BASISD_SUPPORT_* macros are set in CMakeLists.txt before this unit is compiled.
#include "basisu_transcoder.h"

#define LOG_TAG "FlixelBasisu"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

/* Reinterpret a jlong handle as a pointer. Zero is treated as a null handle. */
static inline basist::ktx2_transcoder* to_transcoder(jlong handle) {
    return reinterpret_cast<basist::ktx2_transcoder*>(static_cast<uintptr_t>(handle));
}

static inline jlong from_transcoder(basist::ktx2_transcoder* t) {
    return static_cast<jlong>(reinterpret_cast<uintptr_t>(t));
}

/* Map the Java-side integer constants to the basisu enum values. */
static basist::transcoder_texture_format map_format(jint fmt) {
    switch (fmt) {
        case 0: return basist::transcoder_texture_format::cTFASTC_4x4_RGBA;
        case 1: return basist::transcoder_texture_format::cTFETC2_RGBA;
        case 2: return basist::transcoder_texture_format::cTFRGBA32;
        default:
            LOGE("Unknown target format %d; falling back to RGBA32", fmt);
            return basist::transcoder_texture_format::cTFRGBA32;
    }
}

extern "C" {

/*
 * FlixelBasisu.init() - Initialize global transcoder lookup tables.
 *
 * Must be called once before any other function. Calling it multiple times is safe (the
 * underlying implementation guards against re-initialization). The Java side calls this
 * from a static initializer block so normal usage is automatic.
 */
JNIEXPORT void JNICALL
Java_org_flixelgdx_backend_android_graphics_FlixelBasisu_init(JNIEnv* /*env*/, jclass /*clazz*/) {
    basist::basisu_transcoder_init();
}

/*
 * FlixelBasisu.open(ByteBuffer) - Parse a KTX2 file held in a direct ByteBuffer.
 *
 * Returns a non-zero handle on success, or 0 if the buffer is null, not direct, or the data
 * cannot be parsed. The caller must eventually pass the handle to close().
 */
JNIEXPORT jlong JNICALL
Java_org_flixelgdx_backend_android_graphics_FlixelBasisu_open(
        JNIEnv* env, jclass /*clazz*/, jobject buf) {
    if (buf == nullptr) {
        LOGE("open: buffer is null");
        return 0;
    }
    void* data = env->GetDirectBufferAddress(buf);
    jlong cap  = env->GetDirectBufferCapacity(buf);
    if (data == nullptr || cap <= 0) {
        LOGE("open: buffer is not direct or has zero capacity");
        return 0;
    }

    auto* tc = new (std::nothrow) basist::ktx2_transcoder();
    if (tc == nullptr) {
        LOGE("open: allocation failed");
        return 0;
    }

    if (!tc->init(data, static_cast<uint32_t>(cap))) {
        LOGE("open: ktx2_transcoder::init failed");
        delete tc;
        return 0;
    }
    if (!tc->start_transcoding()) {
        LOGE("open: start_transcoding failed");
        delete tc;
        return 0;
    }
    return from_transcoder(tc);
}

/*
 * FlixelBasisu.close(long) - Release native memory for a transcoder handle.
 *
 * Safe to call with a 0 handle (no-op). After this call the handle must not be used again.
 */
JNIEXPORT void JNICALL
Java_org_flixelgdx_backend_android_graphics_FlixelBasisu_close(
        JNIEnv* /*env*/, jclass /*clazz*/, jlong handle) {
    if (handle == 0) return;
    basist::ktx2_transcoder* tc = to_transcoder(handle);
    delete tc;
}

/*
 * FlixelBasisu.getLevels(long) - Return the mip-map level count.
 */
JNIEXPORT jint JNICALL
Java_org_flixelgdx_backend_android_graphics_FlixelBasisu_getLevels(
        JNIEnv* /*env*/, jclass /*clazz*/, jlong handle) {
    if (handle == 0) return 0;
    return static_cast<jint>(to_transcoder(handle)->get_levels());
}

/*
 * FlixelBasisu.getWidth(long) - Return the base-level width in texels.
 */
JNIEXPORT jint JNICALL
Java_org_flixelgdx_backend_android_graphics_FlixelBasisu_getWidth(
        JNIEnv* /*env*/, jclass /*clazz*/, jlong handle) {
    if (handle == 0) return 0;
    return static_cast<jint>(to_transcoder(handle)->get_width());
}

/*
 * FlixelBasisu.getHeight(long) - Return the base-level height in texels.
 */
JNIEXPORT jint JNICALL
Java_org_flixelgdx_backend_android_graphics_FlixelBasisu_getHeight(
        JNIEnv* /*env*/, jclass /*clazz*/, jlong handle) {
    if (handle == 0) return 0;
    return static_cast<jint>(to_transcoder(handle)->get_height());
}

/*
 * FlixelBasisu.hasAlpha(long) - Return whether the texture has an alpha channel.
 */
JNIEXPORT jboolean JNICALL
Java_org_flixelgdx_backend_android_graphics_FlixelBasisu_hasAlpha(
        JNIEnv* /*env*/, jclass /*clazz*/, jlong handle) {
    if (handle == 0) return JNI_FALSE;
    return to_transcoder(handle)->get_has_alpha() ? JNI_TRUE : JNI_FALSE;
}

/*
 * FlixelBasisu.isUastc(long) - Return true if the source encoding is UASTC (vs. ETC1S).
 */
JNIEXPORT jboolean JNICALL
Java_org_flixelgdx_backend_android_graphics_FlixelBasisu_isUastc(
        JNIEnv* /*env*/, jclass /*clazz*/, jlong handle) {
    if (handle == 0) return JNI_FALSE;
    return to_transcoder(handle)->is_uastc() ? JNI_TRUE : JNI_FALSE;
}

/*
 * FlixelBasisu.getTranscodedSize(long, int, int) - Return the byte count needed to hold
 * the transcoded data for mip level levelIndex in format fmt.
 *
 * Returns 0 on any error (invalid handle, out-of-range level, or unsupported format).
 */
JNIEXPORT jint JNICALL
Java_org_flixelgdx_backend_android_graphics_FlixelBasisu_getTranscodedSize(
        JNIEnv* /*env*/, jclass /*clazz*/, jlong handle, jint levelIndex, jint fmt) {
    if (handle == 0) return 0;
    basist::ktx2_transcoder* tc = to_transcoder(handle);
    if (levelIndex < 0 || static_cast<uint32_t>(levelIndex) >= tc->get_levels()) {
        LOGE("getTranscodedSize: level %d out of range [0, %u)", levelIndex, tc->get_levels());
        return 0;
    }

    basist::ktx2_image_level_info info;
    if (!tc->get_image_level_info(info, static_cast<uint32_t>(levelIndex), 0, 0)) {
        LOGE("getTranscodedSize: get_image_level_info failed for level %d", levelIndex);
        return 0;
    }

    basist::transcoder_texture_format tf = map_format(fmt);
    uint32_t bytes_per_pixel = basist::basis_get_uncompressed_bytes_per_pixel(tf);
    if (bytes_per_pixel > 0) {
        /* Uncompressed: width * height * bytes-per-pixel. */
        return static_cast<jint>(info.m_orig_width * info.m_orig_height * bytes_per_pixel);
    }
    /* Block-compressed: total_blocks * block_size_in_bytes. */
    uint32_t block_w = basist::basis_get_block_width(tf);
    uint32_t block_h = basist::basis_get_block_height(tf);
    /* basis_universal block sizes: ETC2_RGBA = 16 bytes, ASTC_4x4 = 16 bytes. */
    (void)block_w; (void)block_h;
    /* The transcoder packs two 8-byte ETC1S planes into 16 bytes for ETC2_RGBA. */
    const uint32_t block_bytes = 16u;
    return static_cast<jint>(info.m_num_blocks_x * info.m_num_blocks_y * block_bytes);
}

/*
 * FlixelBasisu.transcode(long, int, int, ByteBuffer) - Transcode a mip level into an
 * output direct ByteBuffer.
 *
 * The buffer must be at least getTranscodedSize(handle, levelIndex, fmt) bytes and must be
 * a direct ByteBuffer. Returns true on success, false on any error.
 */
JNIEXPORT jboolean JNICALL
Java_org_flixelgdx_backend_android_graphics_FlixelBasisu_transcode(
        JNIEnv* env, jclass /*clazz*/, jlong handle, jint levelIndex, jint fmt, jobject outBuf) {
    if (handle == 0) {
        LOGE("transcode: null handle");
        return JNI_FALSE;
    }
    if (outBuf == nullptr) {
        LOGE("transcode: output buffer is null");
        return JNI_FALSE;
    }
    void* dst = env->GetDirectBufferAddress(outBuf);
    jlong dstCap = env->GetDirectBufferCapacity(outBuf);
    if (dst == nullptr || dstCap <= 0) {
        LOGE("transcode: output buffer is not direct or has zero capacity");
        return JNI_FALSE;
    }

    basist::ktx2_transcoder* tc = to_transcoder(handle);
    if (levelIndex < 0 || static_cast<uint32_t>(levelIndex) >= tc->get_levels()) {
        LOGE("transcode: level %d out of range [0, %u)", levelIndex, tc->get_levels());
        return JNI_FALSE;
    }

    basist::ktx2_image_level_info info;
    if (!tc->get_image_level_info(info, static_cast<uint32_t>(levelIndex), 0, 0)) {
        LOGE("transcode: get_image_level_info failed for level %d", levelIndex);
        return JNI_FALSE;
    }

    basist::transcoder_texture_format tf = map_format(fmt);
    uint32_t blocks_or_pixels = basist::basis_transcoder_format_is_uncompressed(tf)
        ? (info.m_orig_width * info.m_orig_height)
        : info.m_total_blocks;

    bool ok = tc->transcode_image_level(
        static_cast<uint32_t>(levelIndex), /*layer_index=*/0, /*face_index=*/0,
        dst, blocks_or_pixels,
        tf,
        /*decode_flags=*/0,
        /*output_row_pitch=*/0,
        /*output_rows=*/0
    );
    if (!ok) {
        LOGE("transcode: transcode_image_level failed for level %d fmt %d", levelIndex, fmt);
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

} /* extern "C" */
