package util;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;

public class MessageTypeAdapterFactory implements TypeAdapterFactory {

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (!Message.class.isAssignableFrom(type.getRawType())) {
            return null; // Let Gson handle non-Message types
        }

        return new TypeAdapter<T>() {
            private final TypeAdapter<T> delegate = (TypeAdapter<T>) new MessageAdapter().nullSafe();

            @Override
            public void write(com.google.gson.stream.JsonWriter out, T value) throws IOException {
                delegate.write(out, value);
            }

            @Override
            public T read(com.google.gson.stream.JsonReader in) throws IOException {
                return delegate.read(in);
            }
        }.nullSafe();
    }

}
