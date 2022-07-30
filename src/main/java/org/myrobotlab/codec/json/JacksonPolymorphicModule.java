package org.myrobotlab.codec.json;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.std.BeanSerializerBase;

public class JacksonPolymorphicModule {

    public static SimpleModule getPolymorphicModule() {
        SimpleModule module = new SimpleModule();
        module.setDeserializerModifier(new BeanDeserializerModifier() {
            @Override
            public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config, BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
                //return super.modifyDeserializer(config, beanDesc, deserializer);
                if (deserializer instanceof ResolvableDeserializer) {


                    return new JacksonPolymorphicDeserializer<>(deserializer, beanDesc.getBeanClass());
                }
                return deserializer;
            }
        });
        module.setSerializerModifier(new BeanSerializerModifier() {
            @Override
            public JsonSerializer<?> modifySerializer(SerializationConfig config, BeanDescription beanDesc, JsonSerializer<?> serializer) {
                if(serializer instanceof BeanSerializerBase)
                    return new JacksonPolymorphicSerializer((BeanSerializerBase) serializer);
                return serializer;
            }
        });
        return module;
    }
}
