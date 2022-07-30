package org.myrobotlab.codec.json;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.myrobotlab.codec.CodecUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

public class GsonPolymorphicTypeAdapterFactory implements TypeAdapterFactory {
    private static final String CLASS_META_KEY="class";

    Gson gson;

    TypeAdapter<JsonElement> elementAdapter;
    TypeAdapterFactory taf;

    @SuppressWarnings("unchecked")
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if(!Object.class.isAssignableFrom(type.getRawType()) || String.class.isAssignableFrom(type.getRawType())
                || CodecUtils.WRAPPER_TYPES.contains(type.getRawType()) || Object[].class.isAssignableFrom(type.getRawType())
                || Collection.class.isAssignableFrom(type.getRawType()) || Map.class.isAssignableFrom(type.getRawType()))
            return null;
        this.gson=gson;
        this.taf=this;
        TypeAdapter<Object> delegate = (TypeAdapter<Object>) gson.getDelegateAdapter(this, type);
        elementAdapter = gson.getAdapter(JsonElement.class);
        TypeAdapter<T> result = (TypeAdapter<T>) new PolymorphicTypeAdapter(type, delegate);
        return result.nullSafe();

    }

    class PolymorphicTypeAdapter extends TypeAdapter<Object> {

        TypeToken<?> type;
        TypeAdapter<Object> delegate;

        public PolymorphicTypeAdapter(TypeToken<?> type, TypeAdapter<Object> delegate) {
            this.type = type;
            this.delegate = delegate;
        }

        @Override
        public void write(JsonWriter out, Object value) throws IOException {
            if(value != null) {
                try {
                    JsonElement element = delegate.toJsonTree(value);
                    if(element.isJsonObject()) {
                        JsonObject object = delegate.toJsonTree(value).getAsJsonObject();
                        object.addProperty(CLASS_META_KEY, value.getClass().getCanonicalName());
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
                if (object.has(CLASS_META_KEY)) {
                    String className=object.get(CLASS_META_KEY).getAsString();
                    try {
                        Class<?> clz = Class.forName(className);
                        TypeAdapter<?> adapter = gson.getDelegateAdapter(taf, TypeToken.get(clz));
                        return adapter.fromJsonTree(element);
                    }
                    catch (Exception e) {
                        return delegate.fromJsonTree(element);
                    }
                }
                else
                    return delegate.fromJsonTree(element);
            } else
                return delegate.fromJsonTree(element);
        }
    }
}
