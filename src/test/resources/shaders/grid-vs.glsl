#version 460 core

layout(location=0) in vec2 inPosition;

layout(location=0) out vec2 outGridPosition;

layout(push_constant) uniform PushConstants
{
    mat4 projection_mtrx;
    mat4 modelView_mtrx;
} push_constants;

const float grid_size = 20.0;

void main(void)
{
    outGridPosition = inPosition * grid_size;
    gl_Position = push_constants.projection_mtrx * push_constants.modelView_mtrx * vec4(vec3(inPosition.x, 0.0, inPosition.y) * grid_size, 1.0);
}
