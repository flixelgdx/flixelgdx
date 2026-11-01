// FlixelGDX WGSL preamble.
//
// The framework puts this text in front of every shader before it is compiled, so shader files never
// declare these names themselves. It is the single source of truth for the shader contract: the renderer
// and the build plugins both read this exact file.
//
// Bind group 0 belongs to the framework and is bound for every draw:
//   binding 0: FlixelGlobals uniform (matrix and texture size)
//   binding 1: the texture being drawn
//   binding 2: the sampler (nearest or linear, following the texture's smooth setting)
//
// Bind group 1 belongs to the shader. Declare one uniform struct at @group(1) @binding(0) and set its
// members by name from Java with FlixelShaderProgram.setUniform(...).
//
// A shader file must define a fragment entry point named fs_main. It may also define a vertex entry point
// named vs_main; when it does not, the default flixel_vs below is used.

struct FlixelGlobals {
  // Maps world pixels to clip space (view-projection times the batch transform). Column-major.
  proj: mat4x4f,
  // Size in pixels of the texture bound at binding 1.
  texSize: vec2f,
  // One over texSize, so shaders can step by exactly one texel.
  texelSize: vec2f,
}

@group(0) @binding(0) var<uniform> flixel: FlixelGlobals;
@group(0) @binding(1) var flixelTexture: texture_2d<f32>;
@group(0) @binding(2) var flixelSampler: sampler;

// One vertex of a batched quad: world position in pixels, texture coordinate, and the tint color.
struct FlixelVertexInput {
  @location(0) position: vec2f,
  @location(1) uv: vec2f,
  @location(2) color: vec4f,
}

// What the vertex stage hands to the fragment stage. Custom vertex shaders must return this struct.
struct FlixelVertexOutput {
  @builtin(position) position: vec4f,
  @location(0) uv: vec2f,
  @location(1) color: vec4f,
}

@vertex
fn flixel_vs(in: FlixelVertexInput) -> FlixelVertexOutput {
  var out: FlixelVertexOutput;
  out.position = flixel.proj * vec4f(in.position, 0.0, 1.0);
  out.uv = in.uv;
  out.color = in.color;
  return out;
}
