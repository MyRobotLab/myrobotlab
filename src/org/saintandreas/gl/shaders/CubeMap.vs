#version 330

uniform mat4 Projection = mat4(1);
uniform mat4 ModelView = mat4(1);

layout(location = 0) in vec3 Position;

out vec3 texCoord;

void main() {
    gl_Position = Projection * mat4(mat3(ModelView)) * vec4(Position, 1.0);
    texCoord = -Position;
}
