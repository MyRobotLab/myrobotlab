package org.myrobotlab.codec.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.ObjectIdWriter;
import com.fasterxml.jackson.databind.ser.std.BeanSerializerBase;
import org.myrobotlab.codec.CodecUtils;

import java.io.IOException;
import java.util.Set;


/**
 * A Jackson serializer that injects {@link CodecUtils#CLASS_META_KEY}
 * into the generated JSON. The value of this key is the object's
 * fully qualified class name. The class name enables other deserializers to
 * choose the correct type to deserialize the json into.
 *
 * @author AutonomicPerfectionist
 */
public class JacksonPolymorphicSerializer extends BeanSerializerBase {

    //Have to override a bunch of creation methods from the abstract class, which
    //we delegate to our constructors which delegate to the superclass constructors.
    //Basically, a lot of boilerplate.

    public JacksonPolymorphicSerializer(BeanSerializerBase source) {
        super(source);
    }

    public JacksonPolymorphicSerializer(JacksonPolymorphicSerializer source,
                                 ObjectIdWriter objectIdWriter) {
        super(source, objectIdWriter);
    }

    public JacksonPolymorphicSerializer(JacksonPolymorphicSerializer source,
                                 String[] toIgnore) {
        super(source, toIgnore);
    }

    public JacksonPolymorphicSerializer(JacksonPolymorphicSerializer jacksonPolymorphicSerializer, Set<String> set, Set<String> set1) {
        super(jacksonPolymorphicSerializer, set, set1);
    }

    public JacksonPolymorphicSerializer(JacksonPolymorphicSerializer jacksonPolymorphicSerializer, ObjectIdWriter objectIdWriter, Object o) {
        super(jacksonPolymorphicSerializer, objectIdWriter, o);
    }


    public JacksonPolymorphicSerializer(JacksonPolymorphicSerializer jacksonPolymorphicSerializer, BeanPropertyWriter[] beanPropertyWriters, BeanPropertyWriter[] beanPropertyWriters1) {
        super(jacksonPolymorphicSerializer, beanPropertyWriters, beanPropertyWriters1);
    }

    @Override

    public BeanSerializerBase withObjectIdWriter(
            ObjectIdWriter objectIdWriter) {
        return new JacksonPolymorphicSerializer(this, objectIdWriter);
    }

    @Override
    protected BeanSerializerBase withByNameInclusion(Set<String> set, Set<String> set1) {
        return new JacksonPolymorphicSerializer(this, set, set1);
    }

    @Override
    protected BeanSerializerBase withIgnorals(String[] toIgnore) {
        return new JacksonPolymorphicSerializer(this, toIgnore);
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
            return new JacksonPolymorphicSerializer(this);
        }
        // already is one, so:
        return this;
    }

    @Override
    public BeanSerializerBase withFilterId(Object o) {
        return new JacksonPolymorphicSerializer(this, _objectIdWriter, o);

    }

    @Override
    protected BeanSerializerBase withProperties(BeanPropertyWriter[] beanPropertyWriters, BeanPropertyWriter[] beanPropertyWriters1) {
        return new JacksonPolymorphicSerializer(this, beanPropertyWriters, beanPropertyWriters1);
    }


    //This is the meat of the class
    @Override
    public void serialize(Object o, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        serializeFields(o, jsonGenerator, serializerProvider);
        jsonGenerator.writeStringField(CodecUtils.CLASS_META_KEY, o.getClass().getName());
        jsonGenerator.writeEndObject();

    }
}