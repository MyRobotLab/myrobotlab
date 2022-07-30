package org.myrobotlab.codec.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import io.gsonfire.GsonFireBuilder;
import io.gsonfire.PostProcessor;

public class GsonPolymorphicSerDeser {
    public static GsonFireBuilder addPolymorphicExtensions(GsonFireBuilder builder) {
        builder.registerTypeSelector(Object.class, readElement -> {
            if(readElement.isJsonPrimitive()) {
                JsonPrimitive primitive = readElement.getAsJsonPrimitive();
                if(primitive.isString())
                    return String.class;
                if(primitive.isBoolean())
                    return Boolean.class;
                if(primitive.isJsonArray())
                    return null;
                if(primitive.isNumber())
                    return Number.class;

            }
            if (!readElement.isJsonObject())
                return null;
            if (!readElement.getAsJsonObject().has("class"))
                return null;
            String kind = readElement.getAsJsonObject().get("class").getAsString();
            try {
                return Class.forName(kind);
            } catch (ClassNotFoundException exception) {
                return null;
            }
        }).registerPostProcessor(Object.class, new PostProcessor<>() {

            @Override
            public void postDeserialize(Object o, JsonElement jsonElement, Gson gson) {
            }

            @Override
            public void postSerialize(JsonElement jsonElement, Object o, Gson gson) {
                if (jsonElement.isJsonObject()) {
                    jsonElement.getAsJsonObject().add("class", new JsonPrimitive(o.getClass().getName()));
                }
            }

        });
        return builder;
    }

    public static GsonFireBuilder createPolymorphicGsonFireBuilder() {
        return addPolymorphicExtensions(new GsonFireBuilder());
    }

    public static GsonBuilder createPolymorphicGsonBuilder() {
        return createPolymorphicGsonFireBuilder().createGsonBuilder();
    }

    public static Gson createPolymorphicGson() {
        return createPolymorphicGsonBuilder().create();
    }
}
