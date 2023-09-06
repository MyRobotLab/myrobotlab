package org.myrobotlab.service.meta;

import org.myrobotlab.service.meta.abstracts.MetaData;

public class AutoEjectFIFOMeta extends MetaData {
    public AutoEjectFIFOMeta() {
        addDescription("A simple sized FIFO that will auto-eject the oldest element when it reaches the given max size.");
    }

}
