package org.myrobotlab.service.interfaces;

import org.myrobotlab.audio.AudioData;

public interface AudioListener {
	
	public void onAudioStart(AudioData data);
	
	public void onAudioEnd(AudioData data);

}
