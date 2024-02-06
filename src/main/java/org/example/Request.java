package org.example;

import org.apache.http.NameValuePair;
import org.apache.http.RequestLine;
import org.apache.http.client.utils.URLEncodedUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class Request {
    private final RequestLine requestLine;
    private final List<String> headers;
    private String body;
    private List<NameValuePair> queryParams;

    public Request(RequestLine requestLine, List<String> headers) throws URISyntaxException {
        this.requestLine = requestLine;
        this.headers = headers;
        queryParams = URLEncodedUtils.parse(new URI(requestLine.getMethod()), StandardCharsets.UTF_8);
    }

    public Request(RequestLine requestLine, List<String> headers, String body) throws URISyntaxException {
        this.requestLine = requestLine;
        this.headers = headers;
        this.body = body;
        queryParams = URLEncodedUtils.parse(new URI(requestLine.getMethod()), StandardCharsets.UTF_8);
    }

    public RequestLine getRequestLine() {
        return requestLine;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getMethod() {
        return this.getRequestLine().getMethod();
    }

    public String getPath() {
        return this.getRequestLine().getMethod();
    }

    public List<String> getQueryParam(String param) {
        return queryParams.stream()
                .filter(o -> o.getName().startsWith(param))
                .map(NameValuePair::getValue)
                .collect(Collectors.toList());
    }

    public List<NameValuePair> getQueryParams() {
        return queryParams;
    }

    public void setQueryParams(List<NameValuePair> params) {
        this.queryParams = params;
    }

    @Override
    public String toString() {
        return "Request{ " +
                "requestLine=" + requestLine +
                ", headers=" + headers +
                ", body='" + body + '\'' +
                '}';
    }
}
