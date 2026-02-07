package com.ingvarruulib.bank2firefly4u;

import java.net.http.HttpRequest;

public record Multipart(String contentType, HttpRequest.BodyPublisher body) {

}
