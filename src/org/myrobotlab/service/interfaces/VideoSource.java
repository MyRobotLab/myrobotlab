package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.Service;
import org.myrobotlab.image.SerializableImage;

// FIXME !!!! THIS NEEDS TO BE AN INTERFACE NOT AND ABSTACT CLASS !!! IPCAMERA AND OPENCV ARE VIDEO SOURCES

public abstract class VideoSource extends Service {

	private static final long serialVersionUID = 1L;

	public VideoSource(String n) {
		super(n);
	}

	public boolean attach(VideoSink vs) {
		vs.subscribe("publishDisplay", getName(), "publishDisplay", SerializableImage.class);
		return true;
	}

	public boolean detach(VideoSink vs) {
		vs.unsubscribe("publishDisplay", getName(), "publishDisplay", SerializableImage.class);
		return true;
	}

	public abstract SerializableImage publishDisplay(SerializableImage img);

}
