package org.myrobotlab.service.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LlamaConfig extends ServiceConfig {

    public String systemPrompt = "";

    public String systemMessage = "";

    /**
     * The prompt that is prefixed to every user request.
     * No whitespace is stripped, so ensure that
     * the prompt is formatted so that a whitespace-stripped
     * user request does not cause tokenizer errors.
     */
    public String userPrompt = "### User:\n";

    /**
     * The prompt that the AI should use, should not
     * have a trailing space. Any trailing space
     * (but not newlines) are stripped to prevent
     * tokenizer errors.
     */
    public String assistantPrompt = "### Assistant:\n";

    public String selectedModel = "llama-2-7b-guanaco-qlora.Q4_K_M.gguf";

    public List<String> modelPaths = new ArrayList<>(List.of(

    ));

    public Map<String, String> modelUrls = new HashMap<>(Map.of(
            "stablebeluga-7b.Q4_K_M.gguf", "https://huggingface.co/TheBloke/StableBeluga-7B-GGUF/resolve/main/stablebeluga-7b.Q4_K_M.gguf",
            "llama-2-7b-guanaco-qlora.Q4_K_M.gguf", "https://huggingface.co/TheBloke/llama-2-7B-Guanaco-QLoRA-GGUF/resolve/main/llama-2-7b-guanaco-qlora.Q4_K_M.gguf"
    ));

}
