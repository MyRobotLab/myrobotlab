package org.myrobotlab.codec;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.ResolvableSerializer;
import com.fasterxml.jackson.databind.ser.impl.ObjectIdWriter;
import com.fasterxml.jackson.databind.ser.std.BeanSerializerBase;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.module.noctordeser.NoCtorDeserModule;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import io.gsonfire.GsonFireBuilder;
import io.gsonfire.PostProcessor;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Registration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


public final class PolymorphicSerializer {

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

    public static class CustomDeserializer<T> extends StdDeserializer<T> implements ResolvableDeserializer {

        public JsonDeserializer<T> originalDeserializer;
        Class<?> clazz;

        public CustomDeserializer(JsonDeserializer<T> deserializer, Class<?> clazz) {
            super(clazz);
            this.clazz = clazz;
            originalDeserializer = deserializer;
        }

        //Default implementation delegates to the other overload, causing infinite loop.
        //We delegate to the original deserializer which then fills the object correctly
        @Override
        public T deserialize(JsonParser jsonParser, DeserializationContext deserializationContext, T o) throws IOException{
            return originalDeserializer.deserialize(jsonParser, deserializationContext, o);
        }

        @SuppressWarnings("unchecked")
        @Override
        public T deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {

            //This is a mess and needs cleaned up

            JsonNode node = jsonParser.readValueAsTree();
            //Need to save the json string because
            //part of it will be consumed
            //by getting the type
            String json = node.toString();
            String typeAsString = node.get("class").asText();
            Class<?> type;
            try {
                type = Class.forName(typeAsString);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }


            JsonParser parser = new JsonFactory().createParser(node.toString());
            parser.setCodec(jsonParser.getCodec());
            parser.nextToken();


            if (type.equals(clazz)) {
                for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext(); ) {
                    Map.Entry<String, JsonNode> field = it.next();
                    if (field.getKey().equals("class"))
                        it.remove();
                }

                parser = new JsonFactory().createParser(node.toString());
                parser.setCodec(jsonParser.getCodec());
                parser.nextToken();
                return (T) originalDeserializer.deserialize(parser, deserializationContext);
            }

            return (T) deserializationContext.readValue(parser, type);

        }


        // for some reason you have to implement ResolvableDeserializer when modifying BeanDeserializer
        // otherwise deserializing throws JsonMappingException??
        @Override public void resolve(DeserializationContext ctxt) throws JsonMappingException
        {
            ((ResolvableDeserializer) originalDeserializer).resolve(ctxt);
        }
    }

    public static class CustomSerializer extends BeanSerializerBase {

        //Have to implement all the interface because there's no abstract to use...

        CustomSerializer(BeanSerializerBase source) {
            super(source);
        }

        CustomSerializer(CustomSerializer source,
                             ObjectIdWriter objectIdWriter) {
            super(source, objectIdWriter);
        }

        CustomSerializer(CustomSerializer source,
                             String[] toIgnore) {
            super(source, toIgnore);
        }

        public CustomSerializer(CustomSerializer customSerializer, Set<String> set, Set<String> set1) {
            super(customSerializer, set, set1);
        }

        public CustomSerializer(CustomSerializer customSerializer, ObjectIdWriter objectIdWriter, Object o) {
            super(customSerializer, objectIdWriter, o);
        }

        public CustomSerializer(CustomSerializer customSerializer, BeanPropertyWriter[] beanPropertyWriters, BeanPropertyWriter[] beanPropertyWriters1) {
            super(customSerializer, beanPropertyWriters, beanPropertyWriters1);
        }


        public BeanSerializerBase withObjectIdWriter(
                ObjectIdWriter objectIdWriter) {
            return new CustomSerializer(this, objectIdWriter);
        }

        @Override
        protected BeanSerializerBase withByNameInclusion(Set<String> set, Set<String> set1) {
            return new CustomSerializer(this, set, set1);
        }

        protected BeanSerializerBase withIgnorals(String[] toIgnore) {
            return new CustomSerializer(this, toIgnore);
        }

        @Override
        protected BeanSerializerBase asArraySerializer() {
            /* Cannot:
             *
             * - have Object Id (may be allowed in future)
             * - have "any getter"
             * - have per-property filters
             */
            if ((_objectIdWriter == null)
                    && (_anyGetterWriter == null)
                    && (_propertyFilterId == null)
            ) {
                return new CustomSerializer(this);
            }
            // already is one, so:
            return this;
        }

        @Override
        public BeanSerializerBase withFilterId(Object o) {
            return new CustomSerializer(this, _objectIdWriter, o);

        }

        @Override
        protected BeanSerializerBase withProperties(BeanPropertyWriter[] beanPropertyWriters, BeanPropertyWriter[] beanPropertyWriters1) {
            return new CustomSerializer(this, beanPropertyWriters, beanPropertyWriters1);
        }

        @Override
        public void serialize(Object o, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            jsonGenerator.writeStartObject();
            serializeFields(o, jsonGenerator, serializerProvider);
            jsonGenerator.writeStringField("class", o.getClass().getCanonicalName());
            jsonGenerator.writeEndObject();

        }
    }

    public static void main(String[] args) throws JsonProcessingException {
        //GSON support still works but is kinda wonky
//        Gson gson = createPolymorphicGson();
//        String msgString = gson.toJson(
//                Message.createMessage("runtime", "runtime", "getId",
//                        new Registration("obsidian", "runtime", "Runtime")
//                )
//        );
        //Have to use `Serializable.class` because `Object.class` uses the default treemap deserializer, no way to override
        //So it technically works, but is a little odd to use
//        Message msgFromString = (Message) gson.fromJson(msgString, Serializable.class);
//        System.out.println(msgString);
//        System.out.println(msgFromString.getClass().getName());

        //Jackson support
        ObjectMapper mapper = new ObjectMapper();

        //This allows Jackson to work just like GSON when no default constructor is available
        mapper.registerModule(new NoCtorDeserModule());


        SimpleModule module = new SimpleModule();
        module.setDeserializerModifier(new BeanDeserializerModifier() {
            @Override
            public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config, BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
                //return super.modifyDeserializer(config, beanDesc, deserializer);
                if (deserializer instanceof ResolvableDeserializer) {


                    return new CustomDeserializer<>(deserializer, beanDesc.getBeanClass());
                }
                return deserializer;
            }
        });
        module.setSerializerModifier(new BeanSerializerModifier() {
            @Override
            public JsonSerializer<?> modifySerializer(SerializationConfig config, BeanDescription beanDesc, JsonSerializer<?> serializer) {
                if(serializer instanceof BeanSerializerBase)
                    return new CustomSerializer((BeanSerializerBase) serializer);
                return serializer;
            }
        });
        mapper.registerModule(module);

        //Disables Jackson's automatic property detection
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        mapper.setVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.ANY);

        //All ready to use now

        String msgString = mapper.writeValueAsString(
                Message.createMessage("runtime", "runtime", "getId",
                        new Registration("obsidian", "runtime", "Runtime")
                )
        );

        //Can use Object.class just fine, so generic objects are deserialized correctly
        //since type erasure makes everything look like an Object
        Message msg = (Message) mapper.readValue(msgString, Object.class);
        System.out.println(msg);
    }
}
