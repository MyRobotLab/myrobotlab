#version 330

uniform sampler2D Font;
uniform vec4 Color;

in vec2 vTexCoord;
out vec4 FragColor;

const float gamma = 2.6;
//const float smoothing = 100.0;

void main()
{
   // retrieve signed distance
   float sdf = texture( Font, vTexCoord ).r;

   // perform adaptive anti-aliasing of the edges
   // float s = smoothing * length(fwidth(vTexCoord));
   float w = 0.2;  // clamp( s, 0.0, 0.5);
   float a = smoothstep(0.5-w, 0.5+w, sdf);

   // gamma correction for linear attenuation
   a = pow(a, 1.0/gamma);

   if (a < 0.01) {
       discard;
   }

   // final color
   FragColor = vec4(Color.rgb, a);
}
