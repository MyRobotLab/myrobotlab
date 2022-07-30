package org.myrobotlab.codec;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.noctordeser.NoCtorDeserModule;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.myrobotlab.codec.json.GsonPolymorphicTypeAdapterFactory;
import org.myrobotlab.codec.json.JacksonPolymorphicModule;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Registration;

import java.io.Serializable;


public class PolymorphicSerializer {

    public static void main(String[] args) throws JsonProcessingException {
        //GSON support still works but is kinda wonky
        Gson gson = new GsonBuilder().registerTypeAdapterFactory(new GsonPolymorphicTypeAdapterFactory()).create();
        String msgString = gson.toJson(
                Message.createMessage("runtime", "runtime", "getId",
                        new Registration("obsidian", "runtime", "Runtime")
                )
        );
        //Have to use `Serializable.class` because `Object.class` uses the default treemap deserializer, no way to override
        //So it technically works, but is a little odd to use
        Message msgFromString = (Message) gson.fromJson(msgString, Serializable.class);
        //Notice that the message data is not preserved, because it is an Object type in Message
        //and GSON does not dispatch to custom de/serializers when the type is Object.
        System.out.println(msgFromString);

        //Jackson setup
        ObjectMapper mapper = new ObjectMapper();

        //This allows Jackson to work just like GSON when no default constructor is available
        mapper.registerModule(new NoCtorDeserModule());

        //Actually add our polymorphic support
        mapper.registerModule(JacksonPolymorphicModule.getPolymorphicModule());

        //Disables Jackson's automatic property detection
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        mapper.setVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.ANY);

        //All ready to use now

        msgString = mapper.writeValueAsString(
                Message.createMessage("runtime", "runtime", "getId",
                        new Registration("obsidian", "runtime", "Runtime")
                )
        );

        //Can use Object.class just fine, so generic objects are deserialized correctly
        //since type erasure makes everything look like an Object
        //Notably, Message works just fine even though it stores an Object array
        Message msg = (Message) mapper.readValue(msgString, Object.class);
        System.out.println(msg);

        System.out.println(new ObjectMapper().readValue("{\"help\": 10}", Object.class).getClass());
    }
}
