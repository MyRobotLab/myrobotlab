package org.myrobotlab.audio;

//import be.tarsos.dsp.AudioEvent;
//import be.tarsos.dsp.AudioProcessor;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.JavaSoundAudioDevice;

public class MRLSoundAudioDevice extends JavaSoundAudioDevice {

  // private List<AudioProcessor> audioProcessors = null;

  private float gain = 1.0F;

  @Override
  public void write(short[] paramArrayOfShort, int paramInt1, int paramInt2) throws JavaLayerException {

    if (gain == 1.0) {
      // default behavior
      super.write(paramArrayOfShort, paramInt1, paramInt2);
    } else {
      // so some digital signal processing!!! woot!
      // System.out.print(".");
      short[] volumeAdjusted = new short[paramArrayOfShort.length];
      for (int i = 0; i < paramArrayOfShort.length; i++) {
        // Multiplication is volume control! amplify the signal by the gain
        // EEK that's a lot of type casting!
        volumeAdjusted[i] = (short) (((float) paramArrayOfShort[i]) * gain);
      }
      // pass the volume adjusted array to the underlying audio device
      super.write(volumeAdjusted, paramInt1, paramInt2);

    }
  }

  public float getGain() {
    return gain;
  }

  /**
   * A value typically between 0.0 to 1.0. (Values larger than 1.0 may clip the
   * original signal)
   * 
   * @param gain - the gain to apply.  This is multiplied by the underlying audio signal.
   */
  public void setGain(float gain) {
    this.gain = gain;
  }

  // public void setLineGain(float gain)
  // {
  // this.getSourceLineInfo()
  // if (source != null)
  // {
  // FloatControl volControl = (FloatControl)
  // source.getControl(FloatControl.Type.MASTER_GAIN);
  // float newGain = Math.min(Math.max(gain, volControl.getMinimum()),
  // volControl.getMaximum());
  //
  // volControl.setValue(newGain);
  // }
  // }

}
