package __PACKAGE_NAME__.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.security.web.savedrequest.NullRequestCache;

@Configuration(proxyBeanMethods = false)
public class LocalBearerSecurityConfiguration {

    @Bean
    OpaqueTokenIntrospector localOpaqueTokenIntrospector(
            @Value("${pilot.auth.caller-a-token}") String callerAToken,
            @Value("${pilot.auth.caller-b-token}") String callerBToken) {
        requireValidToken(callerAToken);
        requireValidToken(callerBToken);
        byte[] callerA = digest(callerAToken);
        byte[] callerB = digest(callerBToken);
        if (MessageDigest.isEqual(callerA, callerB)) {
            throw new IllegalStateException("Pilot Bearer tokens must be distinct");
        }
        return token -> {
            byte[] candidate = digest(token);
            boolean isA = MessageDigest.isEqual(candidate, callerA);
            boolean isB = MessageDigest.isEqual(candidate, callerB);
            if (!isA && !isB) {
                throw new BadOpaqueTokenException("Invalid bearer token");
            }
            String name = isA ? "caller-a" : "caller-b";
            return new DefaultOAuth2AuthenticatedPrincipal(name, Map.of("sub", name),
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        };
    }

    @Bean
    SecurityFilterChain bearerApiSecurity(HttpSecurity http,
                                        OpaqueTokenIntrospector introspector) throws Exception {
        var resolver = new DefaultBearerTokenResolver();
        resolver.setAllowFormEncodedBodyParameter(false);
        resolver.setAllowUriQueryParameter(false);

        return http
            // Only Authorization-header Bearer credentials are accepted; no browser cookie auth.
            .csrf(AbstractHttpConfigurer::disable)
            .cors(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .securityContext(context -> context.securityContextRepository(new NullSecurityContextRepository()))
            .requestCache(cache -> cache.requestCache(new NullRequestCache()))
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/liveness",
                    "/actuator/health/readiness").permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(resourceServer -> resourceServer
                .bearerTokenResolver(resolver)
                .opaqueToken(opaque -> opaque.introspector(introspector)))
            .build();
    }

    private static void requireValidToken(String token) {
        if (token == null || token.length() < 32 || token.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalStateException("Each pilot Bearer token must contain at least 32 non-whitespace characters");
        }
    }

    private static byte[] digest(String token) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}
