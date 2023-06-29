package utils;

import com.google.gson.Gson;

public class Constants {
    public final static String BASE_DOMAIN = "localhost";
    private final static String BASE_URL = "http://" + BASE_DOMAIN + ":8080";
    private final static String CONTEXT_PATH = "/ServerApp";
    public final static String FULL_SERVER_PATH = BASE_URL + CONTEXT_PATH;

    // GSON instance
    public final static Gson GSON_INSTANCE = new Gson();
}
