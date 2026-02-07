package com.ingvarruulib.bank2firefly4u.Multipart;

import java.net.http.HttpRequest;

public record Multipart(String contentType, HttpRequest.BodyPublisher body) {

}
