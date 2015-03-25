package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.Service;
import org.myrobotlab.image.SerializableImage;

public abstract class VideoSink extends Service {

	private static final long serialVersionUID = 1L;

	public VideoSink(String n) {
		super(n);
	}

	public boolean attach(VideoSource vs) {
		subscribe("publishDisplay", vs.getName(), "publishDisplay", SerializableImage.class);
		return true;
	}

	public boolean attachVideoSource(String videoSource) {
		ServiceInterface si = org.myrobotlab.service.Runtime.getService(videoSource);
		if (si instanceof VideoSource) {
			return attach((VideoSource) si);
		}

		error("%s is not a VideoSource", videoSource);
		return false;
	}

	public boolean detach(VideoSource vs) {
		unsubscribe("publishDisplay", vs.getName(), "publishDisplay", SerializableImage.class);
		return true;
	}

	public boolean detachVideoSource(String videoSource) {
		ServiceInterface si = org.myrobotlab.service.Runtime.getService(videoSource);
		if (si instanceof VideoSource) {
			return detach((VideoSource) si);
		}

		error("%s is not a VideoSource", videoSource);
		return false;
	}

	public abstract void publishDisplay(SerializableImage img);
}
