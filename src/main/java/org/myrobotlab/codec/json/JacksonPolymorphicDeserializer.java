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

/**
 * A Jackson deserializer that handles polymorphic operations.
 * This class will look at JSON objects and check if they have a field
 * called {@link CodecUtils#CLASS_META_KEY}, if they do it will
 * use the value of that field as the target type to deserialize into,
 * unless the requested type is not a superclass of the embedded type.
 * The requested type may be outside the embedded type's set of superclasses
 * when the user wishes to interpret the value in a different form, such as
 * a {@link Map}.
 *
 * @param <T> Specifies which type (and its subclasses) this deserializer will operate on.
 */
public class JacksonPolymorphicDeserializer<T> extends StdDeserializer<T> implements ResolvableDeserializer {

    /**
     * The original deserializer to dispatch to in
     * order to avoid infinite loops.
     */
    protected JsonDeserializer<T> originalDeserializer;

    /**
     * The class that was requested to deserialize into.
     */
    protected Class<?> clazz;

    /**
     * Create a new deserializer with the delegate deserializer
     * and the requested type.
     *
     * @param deserializer {@link #originalDeserializer}, must also be a {@link ResolvableDeserializer}
     * @param clazz {@link #clazz}
     * @throws IllegalArgumentException if deserializer is not also a {@link ResolvableDeserializer}
     */
    public JacksonPolymorphicDeserializer(JsonDeserializer<T> deserializer, Class<?> clazz) {
        super(clazz);
        if(!(deserializer instanceof ResolvableDeserializer))
            throw new IllegalArgumentException("Deserializer must also be a ResolvableDeserializer, got " + deserializer);
        this.clazz = clazz;
        originalDeserializer = deserializer;
    }

    //Default implementation delegates to the other overload, causing infinite loop.
    //We delegate to the original deserializer which then fills the object correctly
    @Override
    public T deserialize(JsonParser jsonParser, DeserializationContext deserializationContext, T o) throws IOException {
        return originalDeserializer.deserialize(jsonParser, deserializationContext, o);
    }


    /**
     * Deserialize the given node without attempting
     * polymorphic operations.
     *
     * @param jsonParser The parser to use to deserialize
     * @param deserializationContext The context used to deserialize
     * @param node The node to deserialize
     * @return The fully deserialized object, using the original deserializer
     * @throws IOException if deserialization fails
     */
    protected T deserializeNoPolymorphic(JsonParser jsonParser, DeserializationContext deserializationContext, JsonNode node) throws IOException {
        for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> field = it.next();
            if (field.getKey().equals(CodecUtils.CLASS_META_KEY))
                it.remove();
        }

        JsonParser parser = resetNode(node, jsonParser.getCodec());
        return originalDeserializer.deserialize(parser, deserializationContext);
    }

    /**
     * Reset the node to the beginning of the token stream,
     * using the specified codec.
     *
     * @param node The node to be reset
     * @param codec The codec to use
     * @return A new JsonParser that is reset to the beginning of the node's token stream
     * @throws IOException If a new parser cannot be created
     */
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



        //If we are deser to correct target type
        //or if requested type is not a superclass of embedded type,
        //delegate to default deserialization
        if (type.equals(clazz) || clazz.isAssignableFrom(type)) {
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