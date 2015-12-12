#version 330

uniform samplerCube cubemap;

in vec3 texCoord;
out vec4 fragColor;

void main (void) {
  fragColor = texture(cubemap, texCoord);
  //fragColor = vec4(texCoord, 1);
}
