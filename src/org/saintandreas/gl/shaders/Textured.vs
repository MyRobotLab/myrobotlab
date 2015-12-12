#version 330

uniform mat4 Projection = mat4(1);
uniform mat4 ModelView = mat4(1);

layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 TexCoord0;

out vec2 vTexCoord;

void main() {
  gl_Position = Projection * ModelView * vec4(Position, 1);
  vTexCoord = TexCoord0;
}
