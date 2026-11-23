package com.atlas.atlas_backend.security;

import com.atlas.atlas_backend.usuario.Usuario;
import com.atlas.atlas_backend.usuario.UsuarioRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String jwt = authorizationHeader.substring(7).trim();
            if (!jwt.isEmpty()) {
                try {
                    Claims claims = jwtUtil.parseAndValidateClaims(jwt);
                    String username = claims.getSubject();
                    Integer tokenVersion = jwtUtil.extractTokenVersion(claims);

                    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        Usuario usuario = usuarioRepository.findByUsername(username).orElse(null);
                        if (usuario != null
                                && Boolean.TRUE.equals(usuario.getActivo())
                                && tokenVersion != null
                                && tokenVersion.equals(usuario.getTokenVersion())) {
                            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                            if (userDetails.isEnabled() && username.equals(userDetails.getUsername())) {
                                UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                                        userDetails, null, userDetails.getAuthorities());
                                usernamePasswordAuthenticationToken
                                        .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                                SecurityContextHolder.getContext()
                                        .setAuthentication(usernamePasswordAuthenticationToken);
                            }
                        }
                    }
                } catch (JwtException | IllegalArgumentException | UsernameNotFoundException e) {
                    SecurityContextHolder.clearContext();
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
