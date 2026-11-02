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
 * Each Java handle (jlong) is a pointer to a heap-allocated ktx2_transcoder, which keeps reading the caller's direct
 * buffer until it is closed. Every function reports failure through its return value (0, false) and a logcat line
 * instead of throwing, so a broken file never takes the process down.
 *
 * The Java class that declares the matching native methods is
 *   org.flixelgdx.backend.android.graphics.FlixelAndroidBasisTranscoder
 * and the target numbers below must match its TARGET_ constants.
 */

#include <jni.h>
#include <android/log.h>
#include <cstdint>
#include <new>

// The BASISD_SUPPORT_* macros come from CMakeLists.txt.
#include "basisu_transcoder.h"

#define LOG_TAG "FlixelBasisu"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static inline basist::ktx2_transcoder* to_transcoder(jlong handle) {
    return reinterpret_cast<basist::ktx2_transcoder*>(static_cast<uintptr_t>(handle));
}

/* Maps a Java target number to the transcoder's format. Returns false for an unknown number. */
static bool map_target(jint target, basist::transcoder_texture_format* out) {
    switch (target) {
        case 0: *out = basist::transcoder_texture_format::cTFASTC_4x4_RGBA; return true;
        case 1: *out = basist::transcoder_texture_format::cTFETC2_RGBA; return true;
        // ETC1 data is valid ETC2 RGB8 data, since ETC2 decoders read ETC1 blocks unchanged.
        case 2: *out = basist::transcoder_texture_format::cTFETC1_RGB; return true;
        case 3: *out = basist::transcoder_texture_format::cTFBC7_RGBA; return true;
        case 4: *out = basist::transcoder_texture_format::cTFRGBA32; return true;
        default: return false;
    }
}

extern "C" {

/* Builds the transcoder's global lookup tables. Safe to call more than once. */
JNIEXPORT void JNICALL
Java_org_flixelgdx_backend_android_graphics_FlixelAndroidBasisTranscoder_nInit(JNIEnv*, jclass) {
    basist::basisu_transcoder_init();
}

/* Opens the first `length` bytes of a direct buffer as a KTX2 file. Returns 0 when the data is not valid. */
JNIEXPORT jlong JNICALL
Java_org_flixelgdx_backend_android_graphics_FlixelAndroidBasisTranscoder_nOpen(
        JNIEnv* env, jclass, jobject buffer, jint length) {
    void* data = env->GetDirectBufferAddress(buffer);
    jlong capacity = env->GetDirectBufferCapacity(buffer);
    if (data == nullptr || length <= 0 || length > capacity) {
        LOGE("open: the buffer is not direct, or the length %d does not fit it", length);
        return 0;
    }
    auto* transcoder = new (std::nothrow) basist::ktx2_transcoder();
    if (transcoder == nullptr) {
        LOGE("open: out of memory");
        return 0;
    }
    if (!transcoder->init(data, static_cast<uint32_t>(length)) || !transcoder->start_transcoding()) {
        LOGE("open: the data is not a valid Basis Universal KTX2 file");
        delete transcoder;
        return 0;
    }
    return static_cast<jlong>(reinterpret_cast<uintptr_t>(transcoder));
}

JNIEXPORT void JNICALL
Java_org_flixelgdx_backend_android_graphics_FlixelAndroidBasisTranscoder_nClose(JNIEnv*, jclass, jlong handle) {
    delete to_transcoder(handle);
}

JNIEXPORT jint JNICALL
Java_org_flixelgdx_backend_android_graphics_FlixelAndroidBasisTranscoder_nGetLevels(JNIEnv*, jclass, jlong handle) {
    return static_cast<jint>(to_transcoder(handle)->get_levels());
}

JNIEXPORT jint JNICALL
Java_org_flixelgdx_backend_android_graphics_FlixelAndroidBasisTranscoder_nGetWidth(JNIEnv*, jclass, jlong handle) {
    return static_cast<jint>(to_transcoder(handle)->get_width());
}

JNIEXPORT jint JNICALL
Java_org_flixelgdx_backend_android_graphics_FlixelAndroidBasisTranscoder_nGetHeight(JNIEnv*, jclass, jlong handle) {
    return static_cast<jint>(to_transcoder(handle)->get_height());
}

JNIEXPORT jboolean JNICALL
Java_org_flixelgdx_backend_android_graphics_FlixelAndroidBasisTranscoder_nHasAlpha(JNIEnv*, jclass, jlong handle) {
    return to_transcoder(handle)->get_has_alpha() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_org_flixelgdx_backend_android_graphics_FlixelAndroidBasisTranscoder_nIsUastc(JNIEnv*, jclass, jlong handle) {
    return to_transcoder(handle)->is_uastc() ? JNI_TRUE : JNI_FALSE;
}

/*
 * Transcodes one mip level into `size` bytes of a direct buffer, starting `offset` bytes in. Fails when the space is
 * smaller than the level needs.
 */
JNIEXPORT jboolean JNICALL
Java_org_flixelgdx_backend_android_graphics_FlixelAndroidBasisTranscoder_nTranscode(
        JNIEnv* env, jclass, jlong handle, jint level, jint target, jobject buffer, jint offset, jint size) {
    basist::ktx2_transcoder* transcoder = to_transcoder(handle);
    basist::transcoder_texture_format format;
    if (!map_target(target, &format)) {
        LOGE("transcode: unknown target %d", target);
        return JNI_FALSE;
    }
    auto* base = static_cast<uint8_t*>(env->GetDirectBufferAddress(buffer));
    jlong capacity = env->GetDirectBufferCapacity(buffer);
    if (base == nullptr || offset < 0 || size < 0 || static_cast<jlong>(offset) + size > capacity) {
        LOGE("transcode: the buffer is not direct, or the range does not fit it");
        return JNI_FALSE;
    }
    if (level < 0 || static_cast<uint32_t>(level) >= transcoder->get_levels()) {
        LOGE("transcode: level %d is out of range", level);
        return JNI_FALSE;
    }
    basist::ktx2_image_level_info info;
    if (!transcoder->get_image_level_info(info, static_cast<uint32_t>(level), 0, 0)) {
        LOGE("transcode: no information for level %d", level);
        return JNI_FALSE;
    }
    // Uncompressed output is counted in pixels, block formats in blocks.
    bool uncompressed = basist::basis_transcoder_format_is_uncompressed(format);
    uint32_t units = uncompressed ? info.m_orig_width * info.m_orig_height : info.m_num_blocks_x * info.m_num_blocks_y;
    uint64_t needed = static_cast<uint64_t>(units) * basist::basis_get_bytes_per_block_or_pixel(format);
    if (needed > static_cast<uint64_t>(size)) {
        LOGE("transcode: level %d needs %llu bytes but only %d are available", level,
             static_cast<unsigned long long>(needed), size);
        return JNI_FALSE;
    }
    if (!transcoder->transcode_image_level(static_cast<uint32_t>(level), 0, 0, base + offset, units, format)) {
        LOGE("transcode: level %d could not be transcoded to target %d", level, target);
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

} /* extern "C" */
