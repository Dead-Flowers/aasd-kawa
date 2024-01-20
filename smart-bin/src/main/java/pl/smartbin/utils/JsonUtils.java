package pl.smartbin.utils;

import com.google.gson.*;
import jade.core.AID;

import java.lang.reflect.Type;

public class JsonUtils {
    public static final Gson instance = new GsonBuilder()
            .registerTypeAdapter(AID.class, new JsonSerializer<AID>() {

                @Override
                public JsonElement serialize(AID aid, Type type, JsonSerializationContext jsonSerializationContext) {
                    return new JsonPrimitive(aid.getName());
                }
            })
            .registerTypeAdapter(AID.class, new JsonDeserializer<AID>() {
                @Override
                public AID deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
                    return new AID(jsonElement.getAsString(), true);
                }
            })
            .enableComplexMapKeySerialization()
            .create();

    public static String toJson(Object o) {
        return instance.toJson(o);
    }

    public static <T> T fromJson(String json, Class<T> classOfT) {
        return instance.fromJson(json, classOfT);
    }

    public static <T> T fromJson(String json, Type type) {
        return instance.fromJson(json, type);
    }
}
