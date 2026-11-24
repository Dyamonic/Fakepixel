package ru.yooxa.http.netty;

import lombok.Getter;
import lombok.Setter;
import ru.yooxa.http.header.Header;
import ru.yooxa.json.JArray;
import ru.yooxa.json.JElement;
import ru.yooxa.json.JObject;
import ru.yooxa.json.JParser;
import ru.yooxa.json.object.JsonObject;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Getter
public class HttpResponse implements JsonObject {

    private final int code;
    private final String message;
    private final Map<String, Header> headers;
    private final byte[] data;
    //region Stats
    public boolean keepAlive = false;
    public long acceptTime;
    public long requestTime;
    public long responseTime;
    public HttpRequestData request;
    @Setter
    private long readFirstTime;
    @Setter
    private long readFullTime;
    private JElement json;
    //endregion

    public HttpResponse(int code, String message, Map<String, Header> headers, byte[] data) {
        this.code = code;
        this.message = message;
        this.headers = headers;
        this.data = data;
    }

    public Header getHeader(String name) {
        return this.headers.get(name.toLowerCase());
    }

    public JElement asJson() {
        if (this.json != null) {
            return this.json;
        }

        if (this.data == null || this.data.length == 0) {
            return null;
        }

        return this.json = JParser.parse(this.data);
    }

    public String asString() {
        return new String(data, StandardCharsets.UTF_8);
    }

    @Override
    public String toString() {
        return toJsonString(1);
    }

    @Override
    public JObject toJSON() {
        JObject json = new JObject();

        json.addProperty("code", code);
        json.addProperty("message", message);
        json.addProperty("data", data);

        JArray<JObject> headers = new JArray<>();
        for (Header header : this.headers.values()) {
            headers.add(header.toJSON());
        }

        json.add("headers", headers);

        json.add("request", request.toJSON());

        return json;
    }
}
