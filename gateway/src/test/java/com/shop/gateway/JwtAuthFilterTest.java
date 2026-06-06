package com.shop.gateway;

import com.shop.gateway.filter.JwtAuthGatewayFilterFactory;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthFilterTest {

    private static final String BASE64_SECRET = "bXlzdXBlcnNlY3JldGtleWZvcnRoZXVzZXJzZXJ2aWNlam9l";
    private SecretKey signingKey;
    private JwtAuthGatewayFilterFactory factory;
    private GatewayFilter filter;

    @BeforeEach
    void setUp() {
        byte[] keyBytes = Base64.getDecoder().decode(BASE64_SECRET);
        signingKey = new SecretKeySpec(keyBytes, "HmacSHA256");
        factory = new JwtAuthGatewayFilterFactory(BASE64_SECRET);
        filter = factory.apply(new JwtAuthGatewayFilterFactory.Config());
    }

    private String buildValidToken(String subject) {
        return Jwts.builder()
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(signingKey)
                .compact();
    }

    @Test
    void validToken_allowsRequestThrough() {
        String token = buildValidToken("user-42");

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/orders/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void missingAuthorizationHeader_returns401() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/orders/1").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void invalidToken_returns401() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/orders/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer this.is.not.valid")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void bearerPrefixMissing_returns401() {
        String token = buildValidToken("user-42");

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/orders/1")
                .header(HttpHeaders.AUTHORIZATION, token)  // no "Bearer " prefix
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
