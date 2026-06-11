package com.chengxun.gamemaker.web.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * API 请求 Session 异常处理过滤器
 *
 * 解决的问题：当用户的 Redis Session 被失效（如登出后），浏览器仍携带旧的 Session Cookie，
 * 导致 Spring Session Redis 在保存 Session 时抛出 "Session was invalidated" 异常，
 * 最终返回 500 错误。
 *
 * 处理方式：对 API 请求，在 SessionRepositoryFilter 之前拦截，通过包装 Response
 * 捕获 Session 保存时的异常，避免 500 错误。
 *
 * @author chengxun
 * @since 1.0.0
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class ApiSessionFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiSessionFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();

        // 只对 API 路径生效
        if (!uri.startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 包装 response，捕获 Session 保存时的 IllegalStateException
        try {
            filterChain.doFilter(request, new SessionSafeResponseWrapper(response));
        } catch (IllegalStateException e) {
            if (e.getMessage() != null && e.getMessage().contains("Session was invalidated")) {
                log.debug("API请求Session已失效: {}", uri);
                // 不传播异常
            } else {
                throw e;
            }
        }
    }

    /**
     * 包装 HttpServletResponse，捕获 flush 时的 Session 异常
     */
    private static class SessionSafeResponseWrapper extends jakarta.servlet.http.HttpServletResponseWrapper {

        private boolean sessionError = false;

        public SessionSafeResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void flushBuffer() throws IOException {
            try {
                super.flushBuffer();
            } catch (IllegalStateException e) {
                if (e.getMessage() != null && e.getMessage().contains("Session was invalidated")) {
                    sessionError = true;
                    // 忽略 Session 失效异常
                } else {
                    throw e;
                }
            }
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            return new SafeServletOutputStream(super.getOutputStream());
        }

        @Override
        public java.io.PrintWriter getWriter() throws IOException {
            return new SafePrintWriter(super.getWriter());
        }

        private class SafeServletOutputStream extends jakarta.servlet.ServletOutputStream {
            private final jakarta.servlet.ServletOutputStream delegate;

            SafeServletOutputStream(jakarta.servlet.ServletOutputStream delegate) {
                this.delegate = delegate;
            }

            @Override
            public void write(int b) throws IOException {
                try {
                    delegate.write(b);
                } catch (IllegalStateException e) {
                    if (e.getMessage() != null && e.getMessage().contains("Session was invalidated")) {
                        sessionError = true;
                    } else {
                        throw e;
                    }
                }
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                try {
                    delegate.write(b, off, len);
                } catch (IllegalStateException e) {
                    if (e.getMessage() != null && e.getMessage().contains("Session was invalidated")) {
                        sessionError = true;
                    } else {
                        throw e;
                    }
                }
            }

            @Override
            public void flush() throws IOException {
                try {
                    delegate.flush();
                } catch (IllegalStateException e) {
                    if (e.getMessage() != null && e.getMessage().contains("Session was invalidated")) {
                        sessionError = true;
                    } else {
                        throw e;
                    }
                }
            }

            @Override
            public void close() throws IOException {
                try {
                    delegate.close();
                } catch (IllegalStateException e) {
                    if (e.getMessage() != null && e.getMessage().contains("Session was invalidated")) {
                        sessionError = true;
                    } else {
                        throw e;
                    }
                }
            }

            @Override
            public boolean isReady() {
                return delegate.isReady();
            }

            @Override
            public void setWriteListener(WriteListener listener) {
                delegate.setWriteListener(listener);
            }
        }

        private class SafePrintWriter extends java.io.PrintWriter {
            private final java.io.PrintWriter delegate;

            SafePrintWriter(java.io.PrintWriter delegate) {
                super(delegate);
                this.delegate = delegate;
            }

            @Override
            public void flush() {
                try {
                    delegate.flush();
                } catch (IllegalStateException e) {
                    if (e.getMessage() != null && e.getMessage().contains("Session was invalidated")) {
                        sessionError = true;
                    } else {
                        throw e;
                    }
                }
            }

            @Override
            public void close() {
                try {
                    delegate.close();
                } catch (IllegalStateException e) {
                    if (e.getMessage() != null && e.getMessage().contains("Session was invalidated")) {
                        sessionError = true;
                    } else {
                        throw e;
                    }
                }
            }
        }
    }
}
