package org.example.demography;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;

@Provider
@PreMatching
@Priority(Priorities.AUTHORIZATION)
public class CorsFilter implements ContainerRequestFilter, ContainerResponseFilter {
    private static final String ALLOWED_ORIGIN = System.getProperty(
            "client.allowed.origin", "https://localhost:8443");

    @Context
    private HttpHeaders headers;

    @Override
    public void filter(ContainerRequestContext request) throws IOException {
        if ("OPTIONS".equals(request.getMethod())) {
            request.abortWith(Response.noContent().build());
        }
    }

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException {
        if (ALLOWED_ORIGIN.equals(headers.getHeaderString("Origin"))) {
            response.getHeaders().putSingle("Access-Control-Allow-Origin", ALLOWED_ORIGIN);
            response.getHeaders().putSingle("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            response.getHeaders().putSingle("Access-Control-Allow-Headers", "Content-Type, Accept");
            response.getHeaders().putSingle("Access-Control-Max-Age", "600");
            response.getHeaders().putSingle("Vary", "Origin");
        }
    }
}
