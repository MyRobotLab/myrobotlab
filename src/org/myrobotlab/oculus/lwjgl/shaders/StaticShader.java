package org.myrobotlab.oculus.lwjgl.shaders;

import java.io.IOException;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

public class StaticShader extends ShaderProgram {

	
	private static final String VERTEX_SHADER;
	private static final String FRAGMENT_SHADER;
	static {
		try {
			VERTEX_SHADER = Resources.toString(Resources.getResource("resource/oculus/vertexShader.txt"), Charsets.UTF_8);
			FRAGMENT_SHADER = Resources.toString(Resources.getResource("resource/oculus/fragmentShader.txt"), Charsets.UTF_8);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
	
	public StaticShader() {
		// TODO: figure out how to load these from the jar or something...	
		super(VERTEX_SHADER, FRAGMENT_SHADER);
		
	}

	@Override
	protected void bindAttributes() {
		super.bindAttribute(0,  "position");
		super.bindAttribute(1,  "textureCoords");
	}

}
