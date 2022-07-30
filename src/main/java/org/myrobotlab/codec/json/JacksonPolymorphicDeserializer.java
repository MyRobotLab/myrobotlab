package org.myrobotlab.codec.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.myrobotlab.codec.CodecUtils;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

public class JacksonPolymorphicDeserializer<T> extends StdDeserializer<T> implements ResolvableDeserializer {

    public JsonDeserializer<T> originalDeserializer;
    Class<?> clazz;

    public JacksonPolymorphicDeserializer(JsonDeserializer<T> deserializer, Class<?> clazz) {
        super(clazz);
        this.clazz = clazz;
        originalDeserializer = deserializer;
    }

    //Default implementation delegates to the other overload, causing infinite loop.
    //We delegate to the original deserializer which then fills the object correctly
    @Override
    public T deserialize(JsonParser jsonParser, DeserializationContext deserializationContext, T o) throws IOException {
        return originalDeserializer.deserialize(jsonParser, deserializationContext, o);
    }


    private T deserializeNoPolymorphic(JsonParser jsonParser, DeserializationContext deserializationContext, JsonNode node) throws IOException {
        for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> field = it.next();
            if (field.getKey().equals(CodecUtils.CLASS_META_KEY))
                it.remove();
        }

        JsonParser parser = resetNode(node, jsonParser.getCodec());
        return originalDeserializer.deserialize(parser, deserializationContext);
    }

    private JsonParser resetNode(JsonNode node, ObjectCodec codec) throws IOException {
        JsonParser parser = new JsonFactory().createParser(node.toString());
        parser.setCodec(codec);
        parser.nextToken();
        return parser;
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
        if(!node.has(CodecUtils.CLASS_META_KEY))
            return deserializeNoPolymorphic(jsonParser, deserializationContext, node);

        String typeAsString = node.get(CodecUtils.CLASS_META_KEY).asText();
        Class<?> type;
        try {
            type = Class.forName(typeAsString);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }





        if (type.equals(clazz)) {
            deserializeNoPolymorphic(jsonParser, deserializationContext, node);
        }

        return (T) deserializationContext.readValue(resetNode(node, jsonParser.getCodec()), type);

    }


    // for some reason you have to implement ResolvableDeserializer when modifying BeanDeserializer
    // otherwise deserializing throws JsonMappingException??
    @Override public void resolve(DeserializationContext ctxt) throws JsonMappingException
    {
        ((ResolvableDeserializer) originalDeserializer).resolve(ctxt);
    }
}