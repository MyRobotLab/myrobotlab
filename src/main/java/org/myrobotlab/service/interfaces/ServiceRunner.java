package org.myrobotlab.service.interfaces;
import org.myrobotlab.framework.interfaces.ServiceInterface;

import java.util.List;

public interface ServiceRunner extends ServiceInterface {

    /**
     * Get the runner's supported service programming language
     * keys, such as {@code  py} or {@code kt}. Used to route
     * service creation to a Runtime that supports the language
     * the service is written in.
     *
     * @return The language type keys the runner supports
     */
    List<String>  getSupportedLanguageKeys();

    /**
     * Gets the list of service classes the runner
     * can start and manage on its own.
     *
     * @return The list of available service types
     */
    List<String> getAvailableServiceTypes();

    ServiceInterface createService(String name, String type, String inId);
}
