#version 330

const float SIZE = 1.0;
const vec4 UNIT_QUAD[4] = vec4[4](
    vec4(-SIZE, -SIZE, 0.0, 1.0),
    vec4(+SIZE, -SIZE, 0.0, 1.0),
    vec4(-SIZE, +SIZE, 0.0, 1.0),
    vec4(+SIZE, +SIZE, 0.0, 1.0)
);

out vec2 vTexCoord0;

void main() {
  gl_Position = UNIT_QUAD[gl_VertexID];
  vTexCoord0 = (gl_Position.xy / 2.0) + 0.5;
  vTexCoord0.y = 1.0 - vTexCoord0.y; 
}
