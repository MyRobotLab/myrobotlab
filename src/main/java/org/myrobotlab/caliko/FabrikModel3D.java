package org.myrobotlab.caliko;

import java.nio.FloatBuffer;

import au.edu.federation.caliko.FabrikBone3D;
import au.edu.federation.caliko.FabrikChain3D;
import au.edu.federation.caliko.FabrikStructure3D;
import au.edu.federation.caliko.visualisation.ShaderProgram;
import au.edu.federation.utils.Colour4f;
import au.edu.federation.utils.Mat3f;
import au.edu.federation.utils.Mat4f;
import au.edu.federation.utils.Utils;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * A class to represent a 3D model that can easily be attached to a FabrikBone3D object.
 *  
 * @author  Al Lansley
 * @version  0.3.1 - 20/07/2016
 */
public class FabrikModel3D
{	
	// Each vertex has three positional components - the x, y and z values. 
	private static final int VERTEX_COMPONENTS = 3;
	
	// A single static ShaderProgram is used to draw all axes
	private static ShaderProgram shaderProgram;
	
	// Vertex shader source
	private static final String VERTEX_SHADER_SOURCE =
			"#version 330"                                                                    + Utils.NEW_LINE +
			"in vec3 vertexLocation;   // Incoming vertex attribute"                          + Utils.NEW_LINE +
			"uniform mat4 mvpMatrix;   // Combined Model/View/Projection matrix  "            + Utils.NEW_LINE +
			"void main(void) {"                                                               + Utils.NEW_LINE +
			"	gl_Position = mvpMatrix * vec4(vertexLocation, 1.0); // Project our geometry" + Utils.NEW_LINE +
			"}";

	// Fragment shader source
	private static final String FRAGMENT_SHADER_SOURCE =
			"#version 330"              + Utils.NEW_LINE +
			"out vec4 outputColour;"    + Utils.NEW_LINE +
			"uniform vec4 colour;"      + Utils.NEW_LINE +
			"void main() {"             + Utils.NEW_LINE +
			"	outputColour = colour;" + Utils.NEW_LINE +
			"}";

    // Hold id values for the Vertex Array Object (VAO) and Vertex Buffer Object (VBO)
	private static int vaoId;
	private static int vboId;
	
	// Float buffers for the ModelViewProjection matrix and model colour
	private static FloatBuffer mvpMatrixFB;
	private static FloatBuffer colourFB;
	
	// We'll keep track of and restore the current OpenGL line width, which we'll store in this FloatBuffer.
	// Note: Although we only need a single float for this, LWJGL insists upon a minimum size of 16 floats.
	private static FloatBuffer currentLineWidthFB;	
	
	// ----- Non-Static Properties -----
	
	// The FloatBuffer which will contain our vertex data - as we may load multiple different models this cannot be static
	private FloatBuffer vertexFB;
		
	/** The actual Model associated with this FabrikModel3D. */
	private Model model;
	
	/** The float array storing the axis vertex (including colour) data. */
	private float[] modelData;	

	/**
	 * The line width with which to draw the model in pixels.
	 * 
	 * @default	1.0f
	 */
	private float mLineWidth = 1.0f;
	
	static {
		mvpMatrixFB        = Utils.createFloatBuffer(16);
		colourFB           = Utils.createFloatBuffer(16);
		currentLineWidthFB = Utils.createFloatBuffer(16);

		// ----- Grid shader program setup -----

		shaderProgram = new ShaderProgram();
		shaderProgram.initFromStrings(VERTEX_SHADER_SOURCE, FRAGMENT_SHADER_SOURCE);

		// ----- Grid shader attributes and uniforms -----

		// Add the shader attributes and uniforms
		shaderProgram.addAttribute("vertexLocation");			
		shaderProgram.addUniform("mvpMatrix");
		shaderProgram.addUniform("colour");

		// ----- Set up our Vertex Array Object (VAO) to hold the shader attributes -----

		// Create a VAO and bind to it
		vaoId = glGenVertexArrays();
		glBindVertexArray(vaoId);

		// ----- Vertex Buffer Object (VBO) -----

		// Create a VBO and bind to it
		vboId = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, vboId);

		// Note: We do NOT copy the data into the buffer at this time - we do that on draw!
		
		// Vertex attribute configuration
		glVertexAttribPointer(shaderProgram.attribute("vertexLocation"), // Vertex location attribute index
				                                      VERTEX_COMPONENTS, // Number of components per vertex
				                                               GL_FLOAT, // Data type
				                                                  false, // Normalised?
                                            VERTEX_COMPONENTS * Float.BYTES, // Stride
				                                                    0); // Offset
		
		// Unbind VBO
		glBindBuffer(GL_ARRAY_BUFFER, 0);

		// Enable the vertex attributes
		glEnableVertexAttribArray(shaderProgram.attribute("vertexLocation"));

		// Unbind VAO - all the buffer and attribute settings above will now be associated with our VAO
		glBindVertexArray(0);			
	}
	
	/**
	 * Default constructor.
	 * 
	 * @param	modelFilename	The filename of the model to load.
	 * @param	lineWidth		The width of the lines used to draw the model in pixels.
	 */
	// Note: width is along +/- x-axis, depth is along +/- z-axis, height is the location on
	// the y-axis, numDivisions is how many lines to draw across each axis
	public FabrikModel3D(String modelFilename, float lineWidth)
	{	
		// Load the model, get the vertex data and put it into our vertex FloatBuffer
		model = new Model(modelFilename);
		modelData = model.getVertexFloatArray();
		vertexFB = Utils.createFloatBuffer(model.getNumVertices() * VERTEX_COMPONENTS);
		
		mLineWidth = lineWidth;
		
	} // End of constructor

	/** Private method to actually draw the model. */
	private void drawModel(float lineWidth, Colour4f colour, Mat4f mvpMatrix)
	{
		// Enable our shader program and bind to our VAO
		shaderProgram.use();
		glBindVertexArray(vaoId);

		// Bind to our VBO so we can update the axis data for this particular axis object
		glBindBuffer(GL_ARRAY_BUFFER, vboId);

		// Copy the data for this particular model into the vertex float buffer
		// Note: The model is scaled to each individual bone length, hence the GL_DYNAMIC_DRAW performance hint.
		vertexFB.put(modelData);
		vertexFB.flip();
		glBufferData(GL_ARRAY_BUFFER, vertexFB, GL_DYNAMIC_DRAW);

		// Provide the mvp matrix uniform data
		mvpMatrixFB.put( mvpMatrix.toArray() );
		mvpMatrixFB.flip();
		glUniformMatrix4fv(shaderProgram.uniform("mvpMatrix"), false, mvpMatrixFB);
		
		// Provide the model vertex colour data
		colourFB.put( colour.toArray() );
		colourFB.flip();
		glUniform4fv(shaderProgram.uniform("colour"), colourFB);

		// Store the current GL_LINE_WIDTH
		// IMPORTANT: We MUST allocate a minimum of 16 floats in our FloatBuffer in LWJGL, we CANNOT just get a FloatBuffer with 1 float!
		// ALSO: glPushAttrib(GL_LINE_BIT); /* do stuff */ glPopAttrib(); should work instead of this in theory - but LWJGL fails with 'function not supported'.
		glGetFloatv(GL_LINE_WIDTH, currentLineWidthFB);

		/// Set the GL_LINE_WIDTH to be the width requested, as passed to the constructor
		glLineWidth(lineWidth);

		// 	Draw the model as lines
		glDrawArrays( GL_LINES, 0, model.getNumVertices() );

		// Reset the line width to the previous value
		glLineWidth( currentLineWidthFB.get(0) );

		// Unbind from our VBO
		glBindBuffer(GL_ARRAY_BUFFER, 0);

		// Unbind from our VAO
		glBindVertexArray(0);

		// Disable our shader program
		shaderProgram.disable();
	}
	
	/**
	 * Draw a bone using the model loaded on this FabrikModel3D.
	 * 
	 * @param	bone				The bone to draw.
	 * @param	viewMatrix			The view matrix, typically retrieved from the camera.
	 * @param	projectionMatrix	The projection matrix of our scene.
	 * @param	colour				The colour of the lines used to draw the bone.
	 */
	public void drawBone(FabrikBone3D bone, Mat4f viewMatrix, Mat4f projectionMatrix, Colour4f colour)	
	{	
		// Clone the model and scale the clone to be twice as wide and deep, and scaled along the z-axis to match the bone length
		Model modelCopy = Model.clone(model);
		modelCopy.scale( 2.0f, 2.0f, bone.length() );
		
		// Get our scaled model data
		modelData = modelCopy.getVertexFloatArray();
		
		// Construct a model matrix for this bone
		Mat4f modelMatrix = new Mat4f( Mat3f.createRotationMatrix( bone.getDirectionUV().normalised() ), bone.getStartLocation() );
		
		// Construct a ModelViewProjection and draw the model for this bone
		Mat4f mvpMatrix = projectionMatrix.times(viewMatrix).times(modelMatrix);				
		this.drawModel(mLineWidth, colour, mvpMatrix);
	}
	
	/**
	 * Draw a bone using the model loaded on this FabrikModel3D using a default colour of white at full opacity.
	 * 
	 * @param	bone				The bone to draw.
	 * @param	viewMatrix			The view matrix, typically retrieved from the camera.
	 * @param	projectionMatrix	The projection matrix of our scene.
	 */
	public void drawBone(FabrikBone3D bone, Mat4f viewMatrix, Mat4f projectionMatrix)	
	{	
		this.drawBone(bone, viewMatrix, projectionMatrix, Utils.WHITE);
	}
	
	/**
	 * Draw a chain using the model loaded on this FabrikModel3D.
	 * 
	 * @param	chain				The FabrikChain3D to draw the model as bones on.
	 * @param	viewMatrix			The view matrix, typically retrieved from the camera.
	 * @param	projectionMatrix	The projection matrix of our scene.
	 * @param	colour				The colour of the lines used to draw the model.
	 */
	public void drawChain(FabrikChain3D chain, Mat4f viewMatrix, Mat4f projectionMatrix, Colour4f colour)	
	{	
		int numBones = chain.getNumBones();
		for (int loop = 0; loop < numBones; ++loop)
		{	
			this.drawBone( chain.getBone(loop), viewMatrix, projectionMatrix, colour );
		}	
	}
	
	/**
	 * Draw a chain using the model loaded on this FabrikModel3D using a default colour of white at full opacity.
	 * 
	 * @param	chain				The FabrikChain3D to draw the model as bones on.
	 * @param	viewMatrix			The view matrix, typically retrieved from the camera.
	 * @param	projectionMatrix	The projection matrix of our scene.
	 */
	public void drawChain(FabrikChain3D chain, Mat4f viewMatrix, Mat4f projectionMatrix)	
	{	
		int numBones = chain.getNumBones();
		for (int loop = 0; loop < numBones; ++loop)
		{	
			this.drawBone( chain.getBone(loop), viewMatrix, projectionMatrix, Utils.WHITE);
		}	
	}
	
	/**
	 * Draw a structure using the model loaded on this FabrikModel3D.
	 * 
	 * @param	structure			The FabrikStructure3D to draw the model as bones on.
	 * @param	viewMatrix			The view matrix, typically retrieved from the camera.
	 * @param	projectionMatrix	The projection matrix of our scene.
	 * @param	colour				The colour of the lines used to draw the model.
	 */
	public void drawStructure(FabrikStructure3D structure, Mat4f viewMatrix, Mat4f projectionMatrix, Colour4f colour)	
	{	
		int numChains = structure.getNumChains();
		for (int loop = 0; loop < numChains; ++loop)
		{	
			this.drawChain( structure.getChain(loop), viewMatrix, projectionMatrix, colour );
		}	
	}
	
	/**
	 * Draw a structure using the model loaded on this FabrikModel3D using a default colour of white at full opacity.
	 * 
	 * @param	structure	The FabrikStructure3D to draw the model as bones on.
	 * @param	viewMatrix			The view matrix, typically retrieved from the camera.
	 * @param	projectionMatrix	The projection matrix of our scene.
	 */
	public void drawStructure(FabrikStructure3D structure, Mat4f viewMatrix, Mat4f projectionMatrix)	
	{	
		int numChains = structure.getNumChains();
		for (int loop = 0; loop < numChains; ++loop)
		{	
			this.drawChain( structure.getChain(loop), viewMatrix, projectionMatrix, Utils.WHITE);
		}	
	}
	
	/**
	 * Line width property setter.
	 * <p>
	 * Valid line widths are between 1.0f an 32.0f - values outside of this range will result
	 * in an IllegalArgumentException being thrown.
	 * 
	 * @param	lineWidth	The width of the line used to draw this FabrikModel3D in pixels.
	 */
	void setLineWidth(float lineWidth)
	{
		Utils.validateLineWidth(lineWidth);
		mLineWidth = lineWidth;
	}
	
} // End of FabrikModel3D class
