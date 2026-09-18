package com.gateway.api_gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import javax.crypto.SecretKey;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

@Component
@Order(1)
public class AuthenticationFilter implements Filter {

    public static final String SECRET = "357538782F413F4428472B4B6250655368566D59703373367639792442264529";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 1. Bypass Pre-flight OPTIONS for CORS with full headers
        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            httpResponse.setHeader("Access-Control-Allow-Origin", "http://localhost:3000");
            httpResponse.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            httpResponse.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, *");
            httpResponse.setHeader("Access-Control-Allow-Credentials", "true");
            httpResponse.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        String path = httpRequest.getRequestURI();

        // 2. Bypass Public Routes
        if (path.startsWith("/auth/v1/")) {
            chain.doFilter(request, response);
            return;
        }

        // 3. Extract & Validate JWT
        String authHeader = httpRequest.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.getWriter().write("Unauthorized: Missing or invalid token format");
            return;
        }

        String token = authHeader.substring(7);

        try {
            SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String username = claims.getSubject();
            Object userIdClaim = claims.get("user_id");
            String userId = (userIdClaim != null) ? String.valueOf(userIdClaim) : username;

            // 4. Wrap Request preserving both Custom Headers AND Request Body
            CachedBodyHttpServletRequest requestWrapper = new CachedBodyHttpServletRequest(httpRequest);
            requestWrapper.addHeader("X-User-Id", userId);
            requestWrapper.addHeader("X-Auth-User", username);

            chain.doFilter(requestWrapper, response);

        } catch (Exception e) {
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.getWriter().write("Unauthorized: Token validation failed");
        }
    }

    // Wrapper that caches body bytes so Spring Gateway forwarding doesn't drop payload
    private static class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {
        private final Map<String, String> customHeaders = new HashMap<>();
        private final byte[] cachedBody;

        public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
            super(request);
            this.cachedBody = StreamUtils.copyToByteArray(request.getInputStream());
        }

        public void addHeader(String name, String value) {
            this.customHeaders.put(name, value);
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(this.cachedBody);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return byteArrayInputStream.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {}

                @Override
                public int read() {
                    return byteArrayInputStream.read();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(this.getInputStream()));
        }

        @Override
        public String getHeader(String name) {
            String headerValue = customHeaders.get(name);
            return (headerValue != null) ? headerValue : super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            List<String> names = Collections.list(super.getHeaderNames());
            names.addAll(customHeaders.keySet());
            return Collections.enumeration(names);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            List<String> values = Collections.list(super.getHeaders(name));
            if (customHeaders.containsKey(name)) {
                values.add(customHeaders.get(name));
            }
            return Collections.enumeration(values);
        }
    }
}