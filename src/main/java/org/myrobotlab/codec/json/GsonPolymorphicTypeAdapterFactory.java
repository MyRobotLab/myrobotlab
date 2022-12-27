package org.myrobotlab.codec.json;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.myrobotlab.codec.CodecUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * A {@link TypeAdapterFactory} that enables polymorphic operation.
 * The serialization adapter adds a field with the name taken from
 * {@link CodecUtils#CLASS_META_KEY} and a value equal
 * to the object's fully qualified class name.
 *
 * The deserialization adapter checks if the JSON has
 * {@link CodecUtils#CLASS_META_KEY}, and if so it will use
 * the value as the target type.
 *
 * @author AutonomicPerfectionist
 */
public class GsonPolymorphicTypeAdapterFactory implements TypeAdapterFactory {

    /**
     * The TypeAdapter used to create JsonElements
     */
    protected TypeAdapter<JsonElement> elementAdapter;
    protected TypeAdapterFactory taf;

    @SuppressWarnings("unchecked")
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if(!Object.class.isAssignableFrom(type.getRawType()) || String.class.isAssignableFrom(type.getRawType())
                || CodecUtils.WRAPPER_TYPES.contains(type.getRawType()) || Object[].class.isAssignableFrom(type.getRawType())
                || Collection.class.isAssignableFrom(type.getRawType()) || Map.class.isAssignableFrom(type.getRawType()))
            return null;
        this.taf=this;
        TypeAdapter<Object> delegate = (TypeAdapter<Object>) gson.getDelegateAdapter(this, type);
        elementAdapter = gson.getAdapter(JsonElement.class);
        TypeAdapter<T> result = (TypeAdapter<T>) new PolymorphicTypeAdapter(type, delegate, gson);
        return result.nullSafe();

    }

    /**
     * A type adapter to perform polymorphic deserialization
     * and serialization operations. Should only be created with
     * {@link GsonPolymorphicTypeAdapterFactory#create(Gson, TypeToken)}.
     *
     * @author AutonomicPerfectionist
     */
    protected class PolymorphicTypeAdapter extends TypeAdapter<Object> {

        protected Gson gson;
        protected TypeToken<?> type;
        protected TypeAdapter<Object> delegate;

        public PolymorphicTypeAdapter(TypeToken<?> type, TypeAdapter<Object> delegate, Gson gson) {
            this.type = type;
            this.delegate = delegate;
            this.gson = gson;
        }

        @Override
        public void write(JsonWriter out, Object value) throws IOException {
            if(value != null) {
                try {
                    JsonElement element = delegate.toJsonTree(value);
                    if(element.isJsonObject()) {
                        JsonObject object = delegate.toJsonTree(value).getAsJsonObject();
                        object.addProperty(CodecUtils.CLASS_META_KEY, value.getClass().getName());
                        elementAdapter.write(out, object);
                    } else
                        delegate.write(out, value);

                } catch (IllegalArgumentException iae) {
                    delegate.write(out, value);
                }
            }
            else {
                delegate.write(out, null);
            }
        }

        @Override
        public Object read(JsonReader in) throws IOException {
            JsonElement element = elementAdapter.read(in);
            if (element.isJsonObject()) {
                JsonObject object = element.getAsJsonObject();
                if (object.has(CodecUtils.CLASS_META_KEY)) {
                    String className=object.get(CodecUtils.CLASS_META_KEY).getAsString();
                    try {
                        Class<?> clz = Class.forName(className);
                        if(type.getRawType().isAssignableFrom(clz)) {
                            TypeAdapter<?> adapter = gson.getDelegateAdapter(taf, TypeToken.get(clz));
                            return adapter.fromJsonTree(element);
                        }
                    }
                    catch (Exception ignored) {
                    }
                }
            }

            //If element is not a json object, doesn't have the key,
            //an exception occurs, or requested class is not a superclass
            //of embedded class, will fallthrough to here
            return delegate.fromJsonTree(element);
        }
    }
}
