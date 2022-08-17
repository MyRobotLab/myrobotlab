package org.myrobotlab.codec.json;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.std.BeanSerializerBase;

/**
 * A factory class to configure a {@link SimpleModule} for Jackson
 * {@link ObjectMapper}s that adds polymorphic support.
 *
 * @author AutonomicPerfectionist
 */
public class JacksonPolymorphicModule {

    /**
     * Generate a new SimpleModule and add deserialization and
     * serialization modifiers to enable polymorphic support.
     * @return A new module with polymorphic support
     */
    public static SimpleModule getPolymorphicModule() {
        SimpleModule module = new SimpleModule();
        module.setDeserializerModifier(new BeanDeserializerModifier() {
            @Override
            public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config, BeanDescription beanDesc, JsonDeserializer<?> deserializer) {

                //Need a ResolvableDeserializer to instantiate our polymorphic one
                if (deserializer instanceof ResolvableDeserializer) {


                    return new JacksonPolymorphicDeserializer<>(deserializer, beanDesc.getBeanClass());
                }
                return deserializer;
            }
        });
        module.setSerializerModifier(new BeanSerializerModifier() {
            @Override
            public JsonSerializer<?> modifySerializer(SerializationConfig config, BeanDescription beanDesc, JsonSerializer<?> serializer) {

                //Need a BeanDeserializer to instantiate ours
                if(serializer instanceof BeanSerializerBase)
                    return new JacksonPolymorphicSerializer((BeanSerializerBase) serializer);
                return serializer;
            }
        });
        return module;
    }
}
