package com.picturebook.framework.config;

import com.picturebook.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Value("${picturebook.jwt.secret}")
    private String secret;

    @Value("${picturebook.jwt.header}")
    private String header;

    @Value("${picturebook.jwt.prefix}")
    private String prefix;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String authHeader = request.getHeader(header);
        if (authHeader != null && authHeader.startsWith(prefix)) {
            String token = authHeader.substring(prefix.length()).trim();
            try {
                Claims claims = JwtUtil.parseToken(token, secret);
                Long userId = JwtUtil.getUserId(claims);
                String username = JwtUtil.getUsername(claims);
                String role = JwtUtil.getRole(claims);

                String authority = "ROLE_" + (role != null ? role.toUpperCase() : "USER");
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        username, null, List.of(new SimpleGrantedAuthority(authority)));
                auth.setDetails(userId);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception e) {
                // Token 无效，不设置认证信息，由 Security 拒绝
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
