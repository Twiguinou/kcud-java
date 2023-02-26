#version 460 core

layout(location=0) in vec2 inGridPosition;

layout(location=0) out vec4 outColor;

layout(push_constant) uniform PushConstants
{
    layout(offset = 128) vec3 grid_color;
} push_constants;

/*layout(std140,binding=0) uniform UBO
{
    vec3 grid_color;
} ubo;*/

void main(void)
{
    vec2 grid = abs(fract(inGridPosition - 0.5) - 0.5) / fwidth(inGridPosition);
    float line = min(grid.x, grid.y);
    //outColor = vec4(vec3(1.0 - min(line, 1.0)) * ubo.grid_color, 1.0 - line);
    outColor = vec4(vec3(1.0 - min(line, 1.0)) * vec3(1.0, 1.0, 0.0), 1.0 - line);
}
