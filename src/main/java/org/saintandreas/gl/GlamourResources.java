package org.saintandreas.gl;

import org.saintandreas.resources.Resource;

public enum GlamourResources implements Resource {

  SHADERS_COLORED_FS("org/saintandreas/gl/shaders/Colored.fs"), SHADERS_COLORED_VS("org/saintandreas/gl/shaders/Colored.vs"), SHADERS_CUBEMAP_FS(
      "org/saintandreas/gl/shaders/CubeMap.fs"), SHADERS_CUBEMAP_VS("org/saintandreas/gl/shaders/CubeMap.vs"), SHADERS_INDEXED_VS(
          "org/saintandreas/gl/shaders/Indexed.vs"), SHADERS_LIT_VS("org/saintandreas/gl/shaders/Lit.vs"), SHADERS_LITCOLORED_FS(
              "org/saintandreas/gl/shaders/LitColored.fs"), SHADERS_LITCOLORED_VS("org/saintandreas/gl/shaders/LitColored.vs"), SHADERS_SIMPLE_VS(
                  "org/saintandreas/gl/shaders/Simple.vs"), SHADERS_TEXT_FS("org/saintandreas/gl/shaders/Text.fs"), SHADERS_TEXT_VS(
                      "org/saintandreas/gl/shaders/Text.vs"), SHADERS_TEXTURED_FS("org/saintandreas/gl/shaders/Textured.fs"), SHADERS_TEXTURED_VS(
                          "org/saintandreas/gl/shaders/Textured.vs"), SHADERS_NOISE_CELLULAR2_GLSL(
                              "org/saintandreas/gl/shaders/noise/cellular2.glsl"), SHADERS_NOISE_CELLULAR2X2_GLSL(
                                  "org/saintandreas/gl/shaders/noise/cellular2x2.glsl"), SHADERS_NOISE_CELLULAR2X2X2_GLSL(
                                      "org/saintandreas/gl/shaders/noise/cellular2x2x2.glsl"), SHADERS_NOISE_CELLULAR3_GLSL(
                                          "org/saintandreas/gl/shaders/noise/cellular3.glsl"), SHADERS_NOISE_CNOISE2_GLSL(
                                              "org/saintandreas/gl/shaders/noise/cnoise2.glsl"), SHADERS_NOISE_CNOISE3_GLSL(
                                                  "org/saintandreas/gl/shaders/noise/cnoise3.glsl"), SHADERS_NOISE_CNOISE4_GLSL(
                                                      "org/saintandreas/gl/shaders/noise/cnoise4.glsl"), SHADERS_NOISE_SNOISE2_GLSL(
                                                          "org/saintandreas/gl/shaders/noise/snoise2.glsl"), SHADERS_NOISE_SNOISE3_GLSL(
                                                              "org/saintandreas/gl/shaders/noise/snoise3.glsl"), SHADERS_NOISE_SNOISE4_GLSL(
                                                                  "org/saintandreas/gl/shaders/noise/snoise4.glsl"), SHADERS_NOISE_SRDNOISE2_GLSL(
                                                                      "org/saintandreas/gl/shaders/noise/srdnoise2.glsl"), NO_RESOURCE("");

  public final String path;

  GlamourResources(String path) {
    this.path = path;
  }

  @Override
  public String getPath() {
    return path;
  }
}
