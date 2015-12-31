package org.myrobotlab.oculus.lwjgl.renderengine;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.OpenGLException;
import org.myrobotlab.oculus.lwjgl.models.RawModel;
import org.myrobotlab.oculus.lwjgl.models.TexturedModel;
import org.newdawn.slick.util.Log;

/**
 * An OpenGL renderer class to render textured models in the viewport.
 * @author kwatters
 *
 */
public class Renderer {

	// prepare the frame for rendering.
	public void prepare() {
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
		GL11.glClearColor(1, 0, 0, 1);		
	};
	
	// render the model 
	public void render(TexturedModel texturedModel) {
		RawModel model = texturedModel.getRawModel();
		try {
			GL30.glBindVertexArray(model.getVaoID());
			GL20.glEnableVertexAttribArray(0);
			GL20.glEnableVertexAttribArray(1);
			GL13.glActiveTexture(GL13.GL_TEXTURE0);
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, texturedModel.getTexture().getID());
			GL11.glDrawElements(GL11.GL_TRIANGLES, model.getVertexCount(), GL11.GL_UNSIGNED_INT, 0);
			GL20.glDisableVertexAttribArray(0);
			GL20.glDisableVertexAttribArray(1);
			GL30.glBindVertexArray(0);
		} catch (OpenGLException e) {
			// TODO: figure out what the heck is going on sometimes.. we miss the texture.
			// it seems to become null or isn't bound?!
			//log.error("OPEN GL EXCEPTION");
			e.printStackTrace();
		}
	}	
}
