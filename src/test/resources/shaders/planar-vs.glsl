#version 460 core

layout(location=0) in vec3 inPosition;
layout(location=1) in vec4 inColor;

layout(location=0) out vec4 outColor;

layout(push_constant) uniform PushConstants
{
    mat4 projection_mtrx;
    mat4 modelView_mtrx;
} push_constants;

void main(void)
{
    gl_Position = push_constants.projection_mtrx * push_constants.modelView_mtrx * vec4(inPosition + vec3(0.0, 1 * gl_InstanceIndex, 0.0), 1.0);
    outColor = inColor;
}
