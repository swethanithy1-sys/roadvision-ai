package com.roadvision.security;

import com.roadvision.user.User;
import com.roadvision.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Resolves the Spring Security {@link org.springframework.security.core.Authentication} from a
 * Supabase-issued JWT (already signature/expiry-verified by the OAuth2 resource server via
 * Supabase's JWKS endpoint — see application.yml). The JWT's own "role" claim is just the
 * Postgres role ("authenticated"), not our app's CITIZEN/ADMIN split, so that comes from the
 * matching {@link User} profile row instead.
 */
@Component
@RequiredArgsConstructor
public class SupabaseJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserRepository userRepository;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("No profile found for this account"));

        UserPrincipal principal = new UserPrincipal(user.getId(), user.getRole().name());
        return new UsernamePasswordAuthenticationToken(principal, jwt, principal.getAuthorities());
    }
}
