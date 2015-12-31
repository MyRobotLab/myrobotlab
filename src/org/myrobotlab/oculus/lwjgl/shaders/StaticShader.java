package org.myrobotlab.oculus.lwjgl.shaders;

import java.io.IOException;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

/** 
 * A shader to be used to render objects, this shader uses the hard coded 
 * vertex and fragment shader code in resources/oculus ...
 * 
 * @author kwatters
 *
 */
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
		// we currently only care about positions and texture coordinates
		// in the future we might care about other things
		super.bindAttribute(0,  "position");
		super.bindAttribute(1,  "textureCoords");
	}

}
