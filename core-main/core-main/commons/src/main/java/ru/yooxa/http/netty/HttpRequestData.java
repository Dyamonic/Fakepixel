package ru.yooxa.http.netty;

import lombok.Getter;
import ru.yooxa.http.HttpRequest;
import ru.yooxa.http.header.Header;
import ru.yooxa.json.JArray;
import ru.yooxa.json.JObject;
import ru.yooxa.json.object.JsonObject;

import java.util.Collection;

/**
 * Class of project CryptoPidors on 06.08.2019 in 16:00.
 */
@Getter
public class HttpRequestData implements JsonObject {

    //region debug
    public final long createTime = System.currentTimeMillis();
    private final HttpRequest.Type type;
    private final String path;
    private final Collection<Header> headers;
    private final byte[] data;
    public long startEncoding;
    public long endEncoding;

    //endregion

    public HttpRequestData(HttpRequest.Type type, String path, Collection<Header> headers, byte[] data) {
        this.type = type;
        this.path = path;
        this.headers = headers;
        this.data = data;
    }

    @Override
    public String toString() {
        return "HttpRequestData{" +
                "type=" + type +
                ", path='" + path + '\'' +
                ", headers=" + headers +
                ", data=" + new String(data) +
                '}';
    }

    @Override
    public JObject toJSON() {
        JObject json = new JObject();

        json.addProperty("type", type);
        json.addProperty("path", path);
        json.addProperty("data", data);

        JArray<JObject> headers = new JArray<>();
        for (Header header : this.headers) {
            headers.add(header.toJSON());
        }

        json.add("headers", headers);

        return json;
    }
}
