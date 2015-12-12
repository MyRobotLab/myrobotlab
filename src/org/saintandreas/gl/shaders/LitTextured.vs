#version 330

uniform mat4 Projection = mat4(1);
uniform mat4 ModelView = mat4(1);

layout(location = 0) in vec4 Position;
layout(location = 1) in vec2 TexCoord;
layout(location = 2) in vec3 Normal;

out vec3 vViewNormal;
out vec4 vViewPosition;
out vec2 vTexCoord;

void main() {
    gl_Position = Projection * ModelView * Position;

    // The normal in view space
    vViewNormal = vec4(ModelView * vec4(Normal.xyz, 0)).xyz;

    // The position in view space
    vViewPosition = ModelView * Position;

    // The vertex color
    vTexCoord = TexCoord;
}

