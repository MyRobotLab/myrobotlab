package org.myrobotlab.oculus.lwjgl.shaders;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

public abstract class ShaderProgram {

	private int programID;
	private int vertexShaderID;
	private int fragmentShaderID;

	public ShaderProgram(String vertexFile, String fragmentFile) {
		
		
		vertexShaderID = loadShader(vertexFile,  GL20.GL_VERTEX_SHADER, false);
		fragmentShaderID = loadShader(fragmentFile, GL20.GL_FRAGMENT_SHADER, false);
		programID = GL20.glCreateProgram();
		GL20.glAttachShader(programID, vertexShaderID);
		GL20.glAttachShader(programID,  fragmentShaderID);
		bindAttributes();
		GL20.glLinkProgram(programID);
		GL20.glValidateProgram(programID);
	}

	public void start() {
		GL20.glUseProgram(programID);
	}

	public void stop() {
		GL20.glUseProgram(0);
	}

	public void cleanUp() {
		stop();
		GL20.glDetachShader(programID, vertexShaderID);
		GL20.glDetachShader(programID, fragmentShaderID);
		GL20.glDeleteShader(vertexShaderID);
		GL20.glDeleteShader(fragmentShaderID);
		GL20.glDeleteProgram(programID);
	};

	protected abstract void bindAttributes();

	protected void bindAttribute(int attribute, String variableName) {
		GL20.glBindAttribLocation(programID,  attribute,  variableName);
	}

	// if isFile = true , load the file name. o/w first arg is the actual source for the shader.
	private static int loadShader(String file, int type, boolean isFile) {

		// load the shader from the file
		String shaderSource = null;
		if (isFile) {
			StringBuilder shaderSourceBuilder = new StringBuilder();
			BufferedReader reader;
			try {
				reader = new BufferedReader(new FileReader(file));
				String line;
				while ((line = reader.readLine()) != null) {
					shaderSourceBuilder.append(line).append("\n");
				};
				reader.close();
				shaderSource = shaderSourceBuilder.toString();
			} catch (IOException e) {
				System.err.println("Could not read file!");
				e.printStackTrace();
				System.exit(-1);
			}
		} else {
			shaderSource = file;
		}

		int shaderID = GL20.glCreateShader(type);
		GL20.glShaderSource(shaderID, shaderSource);
		// compile the shader
		GL20.glCompileShader(shaderID);
		// report an error during compilation.
		if (GL20.glGetShaderi(shaderID,  GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
			System.out.println(GL20.glGetShaderInfoLog(shaderID, 500));
			System.err.println("Could not compile Shader.");
			System.exit(-1);
		}
		return shaderID;

	}


}

