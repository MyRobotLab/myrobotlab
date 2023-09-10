package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.service.config.ServiceConfig;

public class Whisper extends Service<ServiceConfig> {
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
}
