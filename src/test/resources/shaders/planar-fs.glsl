#version 460 core

layout(location=0) in vec4 inColor;
layout(location=1) in vec2 inTexUV;

layout(location=0) out vec4 outColor;

void main(void)
{
    outColor = inColor;
}
