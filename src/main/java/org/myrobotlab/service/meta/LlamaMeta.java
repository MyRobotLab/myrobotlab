package org.myrobotlab.service.meta;

import org.myrobotlab.service.meta.abstracts.MetaData;

public class LlamaMeta extends MetaData {

    public LlamaMeta() {
        addDescription(
                "A large language model inference engine based on the widely used " +
                        "llama.cpp project. Can run most GGUF models."
        );

        addDependency("de.kherud", "llama", "1.1.4");
    }
}
