package com.picturebook.framework.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * JwtAuthFilter 单元测试
 * 覆盖：合法 Token 设置认证上下文 / 无效 Token 清除上下文 / 无 Token 放行
 *
 * @author Agent-test
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain chain;

    @InjectMocks
    private JwtAuthFilter filter;

    private static final String SECRET = "picturebook-secret-key-for-jwt-signing-2026";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(filter, "secret", SECRET);
        ReflectionTestUtils.setField(filter, "header", "Authorization");
        ReflectionTestUtils.setField(filter, "prefix", "Bearer ");
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_validParentToken_shouldSetAuthenticationWithParentRole() throws Exception {
        String token = com.picturebook.common.utils.JwtUtil.generateToken(2L, "demo", "parent", SECRET, 60000L);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        filter.doFilterInternal(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("demo", auth.getName());
        assertEquals(2L, auth.getDetails());
        assertTrue(auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PARENT")));
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilter_validChildToken_shouldSetAuthenticationWithChildRole() throws Exception {
        String token = com.picturebook.common.utils.JwtUtil.generateToken(3L, "kid001", "child", SECRET, 60000L);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        filter.doFilterInternal(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals(3L, auth.getDetails());
        assertTrue(auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CHILD")));
    }

    @Test
    void doFilter_invalidToken_shouldClearContextAndContinue() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid.token.here");

        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilter_noToken_shouldNotSetAuthenticationAndContinue() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilter_wrongPrefixToken_shouldNotSetAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic sometoken");

        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilter_expiredToken_shouldClearContext() throws Exception {
        // 1ms 过期
        String token = com.picturebook.common.utils.JwtUtil.generateToken(1L, "u", "parent", SECRET, 1L);
        Thread.sleep(10L);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }
}
