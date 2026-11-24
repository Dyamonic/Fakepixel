package ru.yooxa.http;

import ru.yooxa.http.header.Header;
import ru.yooxa.http.header.HttpHeaders;
import ru.yooxa.http.netty.HttpRequestData;
import ru.yooxa.http.netty.HttpResponse;

import java.net.InetAddress;
import java.util.concurrent.CompletableFuture;

public class YHttpRequest extends HttpRequest {

    public HttpClient client;

    public YHttpRequest(Type type, String host, InetAddress address) {
        super(type, host, address);
    }

    public YHttpRequest(Type type, String host) {
        this(type, host, null);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("YHttpRequest{");
        sb.append("type=").append(this.type);
        sb.append(", network='").append(this.protocol).append('\'');
        sb.append(", host='").append(this.host).append('\'');
        sb.append(", path='").append(this.path).append('\'');
        if (this.data != null) {
            sb.append(", data=").append(new String(this.data));
        }
        sb.append('}');
        return sb.toString();
    }

    @Override
    public CompletableFuture<HttpResponse> sendRequest(boolean priority) {
        HttpPool pooled = HttpPool.getConnection(this.local, this.host, this.protocol.equals("https"), this.address, priority);

        return this.sendRequest(pooled);
    }

    @Override
    public CompletableFuture<HttpResponse> sendRequestWithoutSslValidation(boolean priority) {
        HttpPool pooled = HttpPool.getConnection(this.local, this.host, false, this.address, priority);

        return this.sendRequest(pooled);
    }

    public CompletableFuture<HttpResponse> sendRequest(HttpPool pooled) {
        if (pooled == null) {
            return sendRequest();
        }

        this.client = pooled.get();

        boolean contentType = false;
        boolean host = false;
        boolean conn = false;

        for (Header header : this.headers) {
            if (header.getName().equalsIgnoreCase(HttpHeaders.CONTENT_TYPE)) {
                contentType = true;
            } else if (header.getName().equalsIgnoreCase(HttpHeaders.HOST)) {
                host = true;
            } else if (header.getName().equalsIgnoreCase(HttpHeaders.CONNECTION)) {
                conn = true;
            }
        }

        if (!host) {
            this.headers.add(new Header("Host", this.host));
        }

        if (!conn) {
            String connection = pooled.isKeepAlive() ? "keep-alive" : "close";
            this.headers.add(new Header("Connection", connection));
        }

        if (this.data != null) {
            if (!contentType) {
                this.headers.add(new Header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded"));
            }

            this.headers.add(new Header(HttpHeaders.CONTENT_LENGTH, String.valueOf(data.length)));
        }

        this.client.setConnectionTimeout(this.connectTimeout);
        this.client.setReadTimeout(this.readTimeout);

        HttpRequestData requestData = new HttpRequestData(this.type, this.path, this.headers, this.data);

        return this.client.sendRequest(requestData).whenComplete((response, cause) -> {
            if (cause != null) {
                pooled.close();
            }

            Header connection = response.getHeader(HttpHeaders.CONNECTION);
            boolean serverKeepAlive = connection != null && connection.getValue().equalsIgnoreCase("keep-alive");

            if (!pooled.isKeepAlive() || !serverKeepAlive) {
                pooled.close();
            } else {
                pooled.free();
            }
        });
    }

}
