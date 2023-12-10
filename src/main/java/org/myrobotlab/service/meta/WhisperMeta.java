package org.myrobotlab.service.meta;

import org.myrobotlab.service.meta.abstracts.MetaData;

public class WhisperMeta extends MetaData {
    public WhisperMeta() {
        addDescription("A local speech recognition service leveraging the popular whisper.cpp project.");
        addDependency("io.github.givimad", "whisper-jni", "1.4.2-6");
    }
}
