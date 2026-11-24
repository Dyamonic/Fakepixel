package ru.yooxa.http.netty;

import lombok.Getter;

/**
 * Class of project collector on 13.05.2020 in 4:29.
 */
public class HttpException extends Exception {

    @Getter
    private final HttpRequestData request;

    public HttpException(String message, HttpRequestData request) {
        super(message + " | " + request.toJsonString(1));
        this.request = request;
    }
}
