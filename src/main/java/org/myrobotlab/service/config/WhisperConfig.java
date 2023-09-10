package org.myrobotlab.service.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WhisperConfig extends SpeechRecognizerConfig {
    public String selectedModel = "ggml-tiny.en.bin";

    public List<String> modelPaths = new ArrayList<>(List.of(

    ));

    public Map<String, String> modelUrls = new HashMap<>(Map.of(
            "ggml-tiny.bin", "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin",
            "ggml-small.bin", "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small.bin",
            "ggml-tiny.en.bin", "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.en.bin",
            "ggml-small.en.bin", "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small.en.bin",
            "ggml-medium-q5_0.bin", "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-medium-q5_0.bin",
            "ggml-medium.en-q5_0.bin", "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-medium.en-q5_0.bin"
    ));
}
