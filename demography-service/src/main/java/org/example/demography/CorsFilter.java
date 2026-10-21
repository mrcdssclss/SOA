package org.example.demography;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;

@Provider
@PreMatching
@Priority(Priorities.AUTHORIZATION)
public class CorsFilter implements ContainerRequestFilter, ContainerResponseFilter {
    private static final String LOCAL_ORIGIN = "https://localhost:28443";
    private static final String HELIOS_ORIGIN = "https://helios.cs.ifmo.ru:28443";

    @Override
    public void filter(ContainerRequestContext request) throws IOException {
        if ("OPTIONS".equals(request.getMethod())) {
            request.abortWith(Response.noContent().build());
        }
    }

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException {
        String origin = request.getHeaderString("Origin");

        if (LOCAL_ORIGIN.equals(origin) || HELIOS_ORIGIN.equals(origin)) {
            response.getHeaders().putSingle("Access-Control-Allow-Origin", origin);
            response.getHeaders().putSingle("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            response.getHeaders().putSingle("Access-Control-Allow-Headers", "Content-Type, Accept");
            response.getHeaders().putSingle("Access-Control-Max-Age", "600");
            response.getHeaders().putSingle("Vary", "Origin");
        }
    }
}