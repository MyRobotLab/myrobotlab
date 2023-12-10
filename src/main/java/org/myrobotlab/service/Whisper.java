package org.myrobotlab.service;

import io.github.givimad.whisperjni.WhisperContext;
import io.github.givimad.whisperjni.WhisperFullParams;
import io.github.givimad.whisperjni.WhisperJNI;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.service.abstracts.AbstractSpeechRecognizer;
import org.myrobotlab.service.config.LlamaConfig;
import org.myrobotlab.service.config.WhisperConfig;
import org.myrobotlab.service.data.Locale;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.util.Map;

public class Whisper extends AbstractSpeechRecognizer<WhisperConfig> {
    private transient WhisperJNI whisper;

    private transient WhisperContext ctx;

    private transient WhisperFullParams params;

    private transient Thread listeningThread = new Thread();


    /**
     * Constructor of service, reservedkey typically is a services name and inId
     * will be its process id
     *
     * @param reservedKey the service name
     * @param inId        process id
     */
    public Whisper(String reservedKey, String inId) {
        super(reservedKey, inId);
    }

    public void loadModel(String modelPath) {
        try {
            whisper = new WhisperJNI();
            WhisperJNI.loadLibrary();
            ctx = whisper.init(Path.of(modelPath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        params = new WhisperFullParams();
        params.nThreads = Platform.getLocalInstance().getNumPhysicalProcessors();
        params.printRealtime = true;
        params.printProgress = true;

    }

    public String findModelPath(String modelName) {
        // First, we loop over all user-defined
        // model directories
        for (String dir : config.modelPaths) {
            File path = new File(dir + fs + modelName);
            if (path.exists()) {
                return path.getAbsolutePath();
            }
        }

        // Now, we check our data directory for any downloaded models
        File path = new File(getDataDir() + fs + modelName);
        if (path.exists()) {
            return path.getAbsolutePath();
        } else if (config.modelUrls.containsKey(modelName)) {
            // Model was not in data but we do have a URL for it
            try (FileOutputStream fileOutputStream = new FileOutputStream(path)) {
                ReadableByteChannel readableByteChannel = Channels.newChannel(new URL(config.modelUrls.get(modelName)).openStream());
                FileChannel fileChannel = fileOutputStream.getChannel();
                info("Downloading model %s to path %s from URL %s", modelName, path, config.modelUrls.get(modelName));
                fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return path.getAbsolutePath();
        }
        // Cannot find the model anywhere
        error("Could not locate model {}, add its URL to download it or add a directory where it is located", modelName);
        return null;
    }

    @Override
    public void startListening() {

        listeningThread = new Thread(() -> {
            AudioFormat format = new AudioFormat(16000.0f, 16, 1, true, false);
            TargetDataLine microphone = null;

            Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
            for (Mixer.Info info: mixerInfos){
                Mixer m = AudioSystem.getMixer(info);
                Line.Info[] lineInfos = m.getTargetLineInfo();
                for (Line.Info lineInfo:lineInfos){
                    System.out.println (info.getName()+"---"+lineInfo);
                    // Hard-code for my mic right now
                    if (info.getName().contains("U0x46d0x825")) {
                        try {
                            microphone = (TargetDataLine) m.getLine(lineInfo);
                            microphone.open(format);
                            System.out.println("Sample rate: " + format.getSampleRate());
                        } catch (LineUnavailableException e) {
                            throw new RuntimeException(e);
                        }
                    }

                }

            }

            int numBytesRead;

            microphone.start();
            while(config.listening) {
                int CHUNK_SIZE = (int)((format.getFrameSize() * format.getFrameRate())) * 5;
                ByteBuffer captureBuffer = ByteBuffer.allocate(CHUNK_SIZE);
                captureBuffer.order(ByteOrder.LITTLE_ENDIAN);
                numBytesRead = microphone.read(captureBuffer.array(), 0, CHUNK_SIZE);
                System.out.println("Num bytes read=" + numBytesRead);
                ShortBuffer shortBuffer = captureBuffer.asShortBuffer();
                // transform the samples to f32 samples
                float[] samples = new float[captureBuffer.capacity() / 2];
                int index = 0;
                shortBuffer.position(0);
                while (shortBuffer.hasRemaining()) {
                    samples[index++] = Float.max(-1f, Float.min(((float) shortBuffer.get()) / (float) Short.MAX_VALUE, 1f));
                }
                int result = whisper.full(ctx, params, samples, samples.length);
                if(result != 0) {
                    throw new RuntimeException("Transcription failed with code " + result);
                }
                int numSegments = whisper.fullNSegments(ctx);
                System.out.println("Inference done, numSegments=" + numSegments);
                for (int i = 0; i < numSegments; i++) {
                    System.out.println(whisper.fullGetSegmentText(ctx, i));
                    invoke("publishRecognized", whisper.fullGetSegmentText(ctx, i));
                }

            }
            microphone.close();
        });
        super.startListening();

        listeningThread.start();
    }

    @Override
    public WhisperConfig apply(WhisperConfig c) {
        super.apply(c);

        if (config.selectedModel != null && !config.selectedModel.isEmpty()) {
            String modelPath = findModelPath(config.selectedModel);
            if (modelPath != null) {
                loadModel(modelPath);
            } else {
                error("Could not find selected model {}", config.selectedModel);
            }
        }

        return config;
    }

    /**
     * locales this service supports - implementation can simply get
     * runtime.getLocales() if acceptable or create their own locales
     *
     * @return map of string to locale
     */
    @Override
    public Map<String, Locale> getLocales() {
        return null;
    }


}
