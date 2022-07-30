package org.myrobotlab.codec.json;

import com.google.gson.*;

public class GsonPolymorphicBuilderFactory {

    public static GsonBuilder createPolymorphicGsonBuilder() {
        return new GsonBuilder().registerTypeAdapterFactory(new GsonPolymorphicTypeAdapterFactory());
    }

    public static Gson createPolymorphicGson() {
        return createPolymorphicGsonBuilder().create();
    }
}
