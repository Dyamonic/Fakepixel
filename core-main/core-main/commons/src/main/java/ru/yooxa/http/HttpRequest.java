package ru.yooxa.http;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import ru.yooxa.http.header.Header;
import ru.yooxa.http.netty.HttpResponse;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public abstract class HttpRequest {

    public static final InetAddress LOCAL_IP;

    static {
        InetAddress local = null;

        try {
            local = findLocalIp();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        LOCAL_IP = local;
    }

    @Getter
    protected final Type type;
    protected final String protocol;
    @Getter
    protected final String host;
    @Getter
    protected final InetAddress address;
    protected final List<Header> headers;
    protected final Proxy proxy;
    @Getter
    @Setter
    @NonNull
    protected InetAddress local;
    protected String path;
    @Getter
    protected byte[] data;
    @Getter
    @Setter
    protected int connectTimeout;
    @Getter
    @Setter
    protected int readTimeout = 30_000;
    public HttpRequest(Type type, String host, InetAddress address, Proxy proxy) {
        this.type = type;
        this.proxy = proxy;

        if (host.startsWith("http://")) {
            protocol = "http";
            host = host.substring("http://".length());
        } else if (host.startsWith("https://")) {
            host = host.substring("https://".length());
            protocol = "https";
        } else {
            throw new IllegalStateException("Invalid network");
        }

        String[] split = host.split("/", 2);
        host = split[0];
        if (split.length == 2) {
            path = split[1];
        } else {
            path = "";
        }

        StringBuilder vhost = new StringBuilder(host.length());

        for (char c : host.toCharArray()) {
            if (c >= 'a' && c <= 'z') {
                vhost.append(c);
            } else if (c >= 'A' && c <= 'Z') {
                vhost.append(c);
            } else if (c >= 'а' && c <= 'я') {
                vhost.append(c);
            } else if (c >= 'А' && c <= 'Я') {
                vhost.append(c);
            } else if (c == '.' || c == ':' || c == '-') {
                vhost.append(c);
            } else if (c >= '0' && c <= '9') {
                vhost.append(c);
            }
        }

        this.host = vhost.toString();

        if (address != null) {
            this.address = address;
        } else {
            try {
                int index = this.host.indexOf(":");
                if (index != -1) {
                    host = this.host.substring(0, index);
                }

                this.address = InetAddress.getByName(host);
            } catch (UnknownHostException ex) {
                throw new IllegalStateException("Unknown host - " + host);
            }
        }

        this.headers = new ArrayList<>();
    }

    public HttpRequest(Type type, String host, InetAddress address) {
        this(type, host, address, Proxy.NO_PROXY);
    }

    public HttpRequest(Type type, String host) {
        this(type, host, null);
    }

    public HttpRequest(Type type, URI uri) {
        this.type = type;
        this.host = uri.getHost();
        this.path = uri.getPath();
        this.protocol = uri.getScheme();
        this.headers = new ArrayList<>();
        this.proxy = Proxy.NO_PROXY;

        try {
            this.address = InetAddress.getByName(host);
        } catch (UnknownHostException ex) {
            throw new IllegalStateException("Unknown host - " + host);
        }
    }

    private static InetAddress findLocalIp() throws IOException {
        try (final DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            return socket.getLocalAddress();
        }
    }

    public String getURL() {
        return protocol + "://" + host + "/" + path;
    }

    public void addHeader(String key, String value) {
        headers.add(new Header(key, value));
    }

    public void addHeader(Header header) {
        headers.add(header);
    }

    //Добавить URL
    public HttpRequest addPath(String path) {
        if (path.isEmpty()) return this;

        char last = this.path.length() == 0 ? '/' : this.path.charAt(this.path.length() - 1);
        char next = path.charAt(0);

        if (last == '/' && next == '/') {
            this.path += path.substring(1);
            return this;
        }

        this.path += path;
        return this;
    }

    public String getPath() {
        return path.charAt(0) == '/' ? path : '/' + path;
    }

    public void setPath(String path) {
        if (path.charAt(0) == '/') {
            path = path.substring(1);
        }

        this.path = path;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public void setData(String data) {
        setData(data == null ? null : data.getBytes(StandardCharsets.UTF_8));
    }

    public final CompletableFuture<HttpResponse> sendRequest() {
        return this.sendRequest(false);
    }

    public final CompletableFuture<HttpResponse> sendRequestWithoutSslValidation() {
        return this.sendRequestWithoutSslValidation(false);
    }

    public abstract CompletableFuture<HttpResponse> sendRequest(boolean priority);

    public abstract CompletableFuture<HttpResponse> sendRequestWithoutSslValidation(boolean priority);

    public enum Type {
        GET, POST, PUT, DELETE
    }
}
