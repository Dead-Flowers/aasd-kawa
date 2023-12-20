package pl.smartbin.utils;

import com.google.gson.Gson;

import java.lang.reflect.Type;

public class JsonUtils {

    public static String toJson(Object o) {
        return new Gson().toJson(o);
    }

    public static <T> T fromJson(String json, Class<T> classOfT) {
        return new Gson().fromJson(json, classOfT);
    }

    public static <T> T fromJson(String json, Type type) {
        return new Gson().fromJson(json, type);
    }
}
