package org.myrobotlab.service;

import de.kherud.llama.LlamaModel;
import de.kherud.llama.Parameters;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.programab.Response;
import org.myrobotlab.service.config.LlamaConfig;
import org.myrobotlab.service.data.Utterance;
import org.myrobotlab.service.interfaces.ResponsePublisher;
import org.myrobotlab.service.interfaces.UtterancePublisher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.stream.StreamSupport;

public class Llama extends Service<LlamaConfig> implements UtterancePublisher, ResponsePublisher {
    private transient LlamaModel model;

    /**
     * Constructor of service, reservedkey typically is a services name and inId
     * will be its process id
     *
     * @param reservedKey the service name
     * @param inId        process id
     */
    public Llama(String reservedKey, String inId) {
        super(reservedKey, inId);
    }

    public void loadModel(String modelPath) {
        Parameters params = new Parameters.Builder()
                .setNGpuLayers(0)
                .setTemperature(0.7f)
                .setPenalizeNl(true)
                .setMirostat(Parameters.MiroStat.V2)
                .setAntiPrompt(new String[]{config.userPrompt})
                .build();
        model = new LlamaModel(modelPath, params);
    }

    public Response getResponse(String text) {
        if (model == null) {
            error("Model is not loaded.");
            return null;
        }

        String prompt = config.systemPrompt + config.systemMessage + "\n" + text + "\n";
        String response = StreamSupport.stream(model.generate(prompt).spliterator(), false)
                .map(LlamaModel.Output::toString)
                .reduce("", (a, b) -> a + b);

        Utterance utterance = new Utterance();
        utterance.username = getName();
        utterance.text = response;
        utterance.isBot = true;
        utterance.channel = "";
        utterance.channelType = "";
        utterance.channelBotName = getName();
        utterance.channelName = "";
        invoke("publishUtterance", utterance);
        Response res = new Response("friend", getName(), response, null);
        invoke("publishResponse", res);
        return res;
    }

    public String findModelPath(String model) {
        // First, we loop over all user-defined
        // model directories
        for (String dir : config.modelPaths) {
            File path = new File(dir + fs + model);
            if (path.exists()) {
                return path.getAbsolutePath();
            }
        }

        // Now, we check our data directory for any downloaded models
        File path = new File(getDataDir() + fs + model);
        if (path.exists()) {
            return path.getAbsolutePath();
        } else if (config.modelUrls.containsKey(model)){
            // Model was not in data but we do have a URL for it
            try (FileOutputStream fileOutputStream = new FileOutputStream(path)){
                ReadableByteChannel readableByteChannel = Channels.newChannel(new URL(config.modelUrls.get(model)).openStream());
                FileChannel fileChannel = fileOutputStream.getChannel();
                info("Downloading model %s to path %s from URL %s", model, path, config.modelUrls.get(model));
                fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return path.getAbsolutePath();

        }

        // Cannot find the model anywhere
        error("Could not locate model {}, add its URL to download it or add a directory where it is located", model);
        return null;
    }

    @Override
    public LlamaConfig apply(LlamaConfig c) {
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

    @Override
    public Utterance publishUtterance(Utterance utterance) {
        return utterance;
    }

    @Override
    public Response publishResponse(Response response) {
        return response;
    }

    public static void main(String[] args) {
        try {

            LoggingFactory.init(Level.INFO);

            // Runtime runtime = Runtime.getInstance();
            // Runtime.startConfig("gpt3-01");

            WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
            webgui.autoStartBrowser(false);
            webgui.startService();


            Llama llama = (Llama) Runtime.start("llama", "Llama");

            System.out.println(llama.getResponse("Hello!").msg);


        } catch (Exception e) {
            log.error("main threw", e);
        }
    }
}
