package ru.yooxa.http;

import lombok.NonNull;
import ru.yooxa.http.netty.HttpRequestData;
import ru.yooxa.http.netty.HttpResponse;

import java.net.InetAddress;
import java.util.concurrent.CompletableFuture;

/**
 * Class of project CryptoPidors on 07.08.2019 in 14:33.
 */
public interface HttpClient {

    void setLocalAddress(InetAddress local);

    @NonNull
    CompletableFuture<HttpResponse> sendRequest(HttpRequestData request);

    void close();

    void setConnectionTimeout(int time);

    void setReadTimeout(int time);

    boolean isClosed();

}
