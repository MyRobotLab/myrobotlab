#version 330

uniform sampler2D sampler;

in vec2 vTexCoord0;
out vec4 vFragColor;

void main() {
    //vFragColor = vec4(vTexCoord0, 1, 1);
    vFragColor = texture(sampler, vTexCoord0); 
    //vFragColor = vec4(fract(vTexCoord), log(vTexCoord.x), 1.0);
}
