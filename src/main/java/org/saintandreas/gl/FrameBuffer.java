package org.saintandreas.gl;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL30.*;

import org.saintandreas.gl.textures.Texture;

public class FrameBuffer {
    int frameBuffer;
    Texture texture;
    int depthBuffer;
    int width, height;

    public FrameBuffer(int width, int height) {
        this.width = width; 
        this.height = height;

        frameBuffer = glGenFramebuffers();
        glBindFramebuffer( GL_FRAMEBUFFER, frameBuffer );

        
        texture = new Texture();
        texture.bind();
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE );
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE );

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0,GL_RGBA, GL_INT, (java.nio.ByteBuffer) null);
        glFramebufferTexture2D(GL_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D, texture.id, 0);

        depthBuffer = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, depthBuffer);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, width, height);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER,GL_DEPTH_ATTACHMENT,GL_RENDERBUFFER, depthBuffer);

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public void activate() {
        glViewport (0, 0, width, height);
        glBindTexture(GL_TEXTURE_2D, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, frameBuffer);
    }

    public void deactivate() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public Texture getTexture() {
        return texture;
    }
}
