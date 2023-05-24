package org.myrobotlab.service.interfaces;

import java.util.List;

public interface TextEmbeddingGenerator extends TextListener {

    List<Float> generateEmbeddings(String words);
    List<Float> publishEmbeddings(List<Float> embeddings);
}
