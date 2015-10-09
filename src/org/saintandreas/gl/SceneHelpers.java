package org.saintandreas.gl;

import static java.lang.Math.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.saintandreas.ExampleResource.*;
import static org.saintandreas.gl.OpenGL.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.oculus.lwjgl.models.RawModel;
import org.myrobotlab.oculus.lwjgl.models.TexturedModel;
import org.myrobotlab.oculus.lwjgl.renderengine.Loader;
import org.myrobotlab.oculus.lwjgl.renderengine.Renderer;
import org.myrobotlab.oculus.lwjgl.shaders.StaticShader;
import org.myrobotlab.oculus.lwjgl.textures.ModelTexture;
import org.saintandreas.ExampleResource;
import org.saintandreas.gl.buffers.VertexArray;
import org.saintandreas.gl.shaders.Attribute;
import org.saintandreas.gl.shaders.Program;
import org.saintandreas.gl.textures.Texture;
import org.saintandreas.math.Matrix4f;
import org.saintandreas.math.Quaternion;
import org.saintandreas.math.Vector3f;
import org.saintandreas.math.Vector4f;
import org.saintandreas.resources.Resource;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

public class SceneHelpers {

	private static IndexedGeometry cubeGeometry;
	private static IndexedGeometry cubeGeometryWithNormals;
	private static Program cubeProgram;

	private static IndexedGeometry floorGeometry;
	private static Program floorProgram;
	private static Texture floorTexture;

	private static Program skyboxProgram;
	private static Texture skyboxTexture;

	private static Program unitQuadProgram;
	private static VertexArray unitQuadVao;


	private static TexturedModel texturedModel = null;
	private static Loader loader = new Loader();
	private static Renderer renderer = new Renderer();
	private static StaticShader shader = new StaticShader();

	private static final String UNIT_QUAD_VS;
	private static final String UNIT_QUAD_FS;
	static {
		try {
			UNIT_QUAD_VS = Resources.toString(Resources.getResource("resource/oculus/unitQuad.vs"), Charsets.UTF_8);
			UNIT_QUAD_FS = Resources.toString(Resources.getResource("resource/oculus/unitQuad.fs"), Charsets.UTF_8);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public static void renderTexturedQuad(int texture) {
		if (null == unitQuadProgram) {
			unitQuadProgram = new Program(UNIT_QUAD_VS, UNIT_QUAD_FS);
			unitQuadProgram.link();
		}
		if (null == unitQuadVao) {
			unitQuadVao = new VertexArray();
		}
		unitQuadProgram.use();
		glDisable(GL_DEPTH_TEST);
		glDisable(GL_CULL_FACE);
		unitQuadVao.bind();
		glBindTexture(GL_TEXTURE_2D, texture);
		glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
		Texture.unbind(GL_TEXTURE_2D);
		Program.clear();
		VertexArray.unbind();
	}

	/**
	 * helper function to render an image on the current bound texture. 
	 */
	public static void renderScreen(SerializableImage img) {

		float size = 1.0f;

		if (texturedModel == null) {

			float[] verticies = {
					// left bottom triangle
					-size, size, 0f, // V0
					-size, -size, 0f, // V1
					size, -size, 0f, // V2
					size,size,0f, // V3
			};
			int[] indicies = { 
					0,1,3,
					3,1,2
			};

			// TODO: calculate the texture scaling (probably depends on the power of 2 size thing for a texture.)
			// What's the power of 2 here?
			float xMax = 1.0f;
			float yMax = 0.63f;
			// something like this?
//			float xMax = img.getWidth()/1024.0f;
//			float yMax = img.getHeight()/512.0f;
			float[] textureCoords = {
					0,0,  //V0
					0,yMax,  //V1
					xMax,yMax,  //V2
					xMax,0   //V3
			};
			// TODO: maybe I shouldn't do this each time ? 
			RawModel model = loader.loadToVAO(verticies, textureCoords, indicies);
			ModelTexture texture = new ModelTexture(loader.loadTexture("agent"));
			texturedModel = new TexturedModel(model, texture);

		} 
		
		if (img != null ) {
			System.out.println("WIDTH AND HEIGHT " + img.getWidth() + " " + img.getHeight());
			// clean up the texture as we're about to replace it?
			GL11.glDeleteTextures(texturedModel.getTexture().getID());
			ModelTexture texture = new ModelTexture(loader.loadTexture(img.getImage()));
			//ModelTexture texture = new ModelTexture(loader.loadTexture("agent"));
			texturedModel.setTexture(texture);
		}
		

		// grab the latest opencv image
			//  TODO: get this on a separate thread/via callback from mrl.
//		if (opencv.videoProcessor.getData() != null) {
//			// TODO: you'll run out of textures! need to free up the old texture before 
//			// creating a new one i guess.
//		
//			// maybe this cleans up the old texture?
//			//GL11.glDeleteTextures(texturedModel.getTexture().getID());
//			BufferedImage bi = opencv.videoProcessor.getData().getBufferedImage();
//			ModelTexture texture = new ModelTexture(loader.loadTexture(bi));
//			texturedModel.setTexture(texture);
//		}
		shader.start();
		renderer.render(texturedModel);
		shader.stop();
	}

	public static void renderFloor() {
		if (null == floorGeometry) {
			float size = 100.0f;
			List<Vector4f> vertices = new ArrayList<>();
			vertices.add(new Vector4f(size, 0, size, 1));
			vertices.add(new Vector4f(size, size, 0, 0));
			vertices.add(new Vector4f(size, 0, -size, 1));
			vertices.add(new Vector4f(size, -size, 0, 0));
			vertices.add(new Vector4f(-size, 0, size, 1));
			vertices.add(new Vector4f(-size, size, 0, 0));
			vertices.add(new Vector4f(-size, 0, -size, 1));
			vertices.add(new Vector4f(-size, -size, 0, 0));
			List<Short> indices = new ArrayList<>();
			indices.add((short) 0); // LL
			indices.add((short) 1); // LR
			indices.add((short) 2); // UL
			indices.add((short) 3); // UR
			IndexedGeometry.Builder builder = new IndexedGeometry.Builder(indices, vertices);
			builder.withDrawType(GL_TRIANGLE_STRIP).withAttribute(Attribute.POSITION).withAttribute(Attribute.TEX);
			floorGeometry = builder.build();
		}

		if (null == floorTexture) {
			try {
				floorTexture = Texture.loadImage(IMAGES_FLOOR_PNG);
			} catch (IOException ex) {
				throw new IllegalStateException("Unable to load floor texture", ex);
			}
			floorTexture.bind();
			floorTexture.parameter(GL_TEXTURE_MAG_FILTER, GL_NEAREST);
			floorTexture.parameter(GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_NEAREST);
			glGenerateMipmap(GL_TEXTURE_2D);
			floorTexture.unbind();
		}
		if (null == floorProgram) {
			floorProgram = new Program(SHADERS_TEXTURED_VS, SHADERS_TEXTURED_FS);
			floorProgram.link();
		}

		floorProgram.use();
		OpenGL.bindAll(floorProgram);
		floorTexture.bind();
		floorGeometry.bindVertexArray();
		floorGeometry.draw();
		Texture.unbind(GL_TEXTURE_2D);
		Program.clear();
		VertexArray.unbind();
	}

	private static Vector4f toNormal(Matrix4f m) {
		Vector4f v = m.mult(new Vector4f(0, 0, 0, 1));
		v = new Vector4f(new Vector3f(v.x, v.y, v.z).normalize(), 1);
		return v;
	}

	private static List<Vector4f> makeNormalCubeVertices() {
		List<Vector4f> result = new ArrayList<>(6 * 4 * 2);
		Matrix4f m;
		List<Vector4f> q = makeQuad(1.0f);
		Vector3f side = new Vector3f(0, 0, 0.5f);

		// Front
		m = new Matrix4f().translate(new Vector3f(0, 0, 0.5f));
		result.addAll(interleaveConstants(transformed(q, m), toNormal(m)));

		// Back
		m = new Matrix4f().rotate((float) PI, Vector3f.UNIT_X).translate(side);
		result.addAll(interleaveConstants(transformed(q, m), toNormal(m)));

		// Top
		m = new Matrix4f().rotate((float) PI / -2f, Vector3f.UNIT_X).translate(side);
		result.addAll(interleaveConstants(transformed(q, m), toNormal(m)));

		// Bottom
		m = new Matrix4f().rotate((float) PI / 2f, Vector3f.UNIT_X).translate(side);
		result.addAll(interleaveConstants(transformed(q, m), toNormal(m)));

		// Left
		m = new Matrix4f().rotate((float) PI / -2f, Vector3f.UNIT_Y).translate(side);
		result.addAll(interleaveConstants(transformed(q, m), toNormal(m)));

		// Right
		m = new Matrix4f().rotate((float) PI / 2f, Vector3f.UNIT_Y).translate(side);
		result.addAll(interleaveConstants(transformed(q, m), toNormal(m)));

		return result;
	}

	public static void renderColorCube() {
		if (null == cubeGeometryWithNormals) {
			List<Vector4f> vertices = makeNormalCubeVertices();
			List<Short> indices = OpenGL.makeColorCubeIndices();
			Geometry.Builder builder = new IndexedGeometry.Builder(indices, vertices)
			.withDrawType(GL_TRIANGLE_STRIP)
			.withAttribute(Attribute.POSITION)
			.withAttribute(Attribute.NORMAL);
			cubeGeometryWithNormals = (IndexedGeometry) builder.build();
		}
		if (null == cubeProgram) {
			cubeProgram = new Program(ExampleResource.SHADERS_COLORCUBE_VS, ExampleResource.SHADERS_COLORCUBE_FS);
			cubeProgram.link();
		}
		glPrimitiveRestartIndex(Short.MAX_VALUE);
		glEnable(GL_PRIMITIVE_RESTART);

		cubeProgram.use();
		OpenGL.bindAll(cubeProgram);
		cubeGeometryWithNormals.bindVertexArray();
		cubeGeometryWithNormals.draw();
		Program.clear();
		VertexArray.unbind();
	}

	// @formatter:off
	private static final Resource SKYBOX[] = {
		IMAGES_SKY_CITY_XPOS_PNG,
		IMAGES_SKY_CITY_XNEG_PNG,
		IMAGES_SKY_CITY_YPOS_PNG,
		IMAGES_SKY_CITY_YNEG_PNG,
		IMAGES_SKY_CITY_ZPOS_PNG,
		IMAGES_SKY_CITY_ZNEG_PNG,
	};
	// @formatter:on

	public static void renderSkybox() {
		if (null == cubeGeometry) {
			cubeGeometry = OpenGL.makeColorCube();
		}
		if (null == skyboxProgram) {
			skyboxProgram = new Program(SHADERS_CUBEMAP_VS, SHADERS_CUBEMAP_FS);
			skyboxProgram.link();
		}

		if (null == skyboxTexture) {
			skyboxTexture = OpenGL.getCubemapTextures(SKYBOX);
		}

		MatrixStack mv = MatrixStack.MODELVIEW;
		cubeGeometry.bindVertexArray();
		mv.push();
		{
			Quaternion q = mv.getRotation();
			mv.identity().rotate(q);
			skyboxProgram.use();
			OpenGL.bindAll(skyboxProgram);
			glCullFace(GL_FRONT);
			skyboxTexture.bind();
			glDisable(GL_DEPTH_TEST);
			cubeGeometry.draw();
			glEnable(GL_DEPTH_TEST);
			skyboxTexture.unbind();
			glCullFace(GL_BACK);
		}
		mv.pop();
	}

}
