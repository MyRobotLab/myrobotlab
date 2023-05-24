package org.myrobotlab.service;

import com.google.common.primitives.Floats;
import com.robrua.nlp.bert.Bert;
import org.myrobotlab.framework.Service;
import org.myrobotlab.service.interfaces.TextEmbeddingGenerator;

import java.io.File;
import java.util.List;

public class EasyBert extends Service implements TextEmbeddingGenerator {

    private Bert bert;
    public final String DEFAULT_BERT_MODEL = "com/robrua/nlp/easy-bert/bert-uncased-L-12-H-768-A-12";


    /**
     * Constructor of service, reservedkey typically is a services name and inId
     * will be its process id
     *
     * @param reservedKey the service name
     * @param inId        process id
     */
    public EasyBert(String reservedKey, String inId) {
        super(reservedKey, inId);
        bert = Bert.load(DEFAULT_BERT_MODEL);
    }

    @Override
    public List<Float> generateEmbeddings(String words) {
        List<Float> embeddings = Floats.asList(bert.embedSequence(words));
        invoke("publishEmbeddings", embeddings);
        return embeddings;
    }

    @Override
    public List<Float> publishEmbeddings(List<Float> embeddings) {
        return embeddings;
    }

    @Override
    public void onText(String text) throws Exception {
        generateEmbeddings(text);
    }

    public void setBertModel(String resource) {
        bert = Bert.load(resource);
    }

    public void setBertModel(File model) {
        bert = Bert.load(model);
    }
}
