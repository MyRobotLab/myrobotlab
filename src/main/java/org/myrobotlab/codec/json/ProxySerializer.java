package org.myrobotlab.codec.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.TimeoutException;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.service.Runtime;

import java.io.IOException;
import java.lang.reflect.Proxy;

public class ProxySerializer extends StdSerializer<Proxy> {

    public ProxySerializer() {
        this(Proxy.class);
    }

    public ProxySerializer(Class<Proxy> t) {
        super(t);
    }

    @Override
    public void serialize(
            Proxy value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException {

        if (value instanceof ServiceInterface) {
            ServiceInterface si = (ServiceInterface) value;
            jgen.writeStartObject();
            jgen.writeStringField("name", si.getName());
            jgen.writeStringField("type", si.getType());
            jgen.writeStringField("id", si.getId());
            try {
                jgen.writeStringField("serviceClass", (String) Runtime.get().sendBlocking(si.getName(), "getServiceClass"));
            } catch (InterruptedException | TimeoutException e) {
                throw new RuntimeException(e);
            }
            jgen.writeEndObject();
        }
    }
}
