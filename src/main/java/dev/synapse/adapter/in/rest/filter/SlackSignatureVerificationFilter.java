package dev.synapse.adapter.in.rest.filter;

import dev.synapse.shared.config.SlackProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class SlackSignatureVerificationFilter extends OncePerRequestFilter {

    private static final String SLACK_SIGNATURE_HEADER = "X-Slack-Signature";
    private static final String SLACK_TIMESTAMP_HEADER = "X-Slack-Request-Timestamp";
    private static final long MAX_TIMESTAMP_AGE_SECONDS = 300L; // 5 minutes

    private final SlackProperties slackProperties;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null || !path.startsWith("/api/slack/");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String signingSecret = slackProperties != null && slackProperties.getSigningSecret() != null
                ? slackProperties.getSigningSecret().trim() : null;
        if (signingSecret == null || signingSecret.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();

        String timestampHeader = request.getHeader(SLACK_TIMESTAMP_HEADER);
        String signatureHeader = request.getHeader(SLACK_SIGNATURE_HEADER);

        if (timestampHeader == null || signatureHeader == null) {
            log.warn("Missing Slack verification headers across [{}]", path);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        long timestamp;
        try {
            timestamp = Long.parseLong(timestampHeader);
        } catch (NumberFormatException _) {
            log.warn("Invalid Slack timestamp format: {}", timestampHeader);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        long now = Instant.now().getEpochSecond();
        if (Math.abs(now - timestamp) > MAX_TIMESTAMP_AGE_SECONDS) {
            log.warn("Slack request timestamp is too old or in future: {}", timestamp);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
        String body = new String(cachedRequest.getCachedBody(), StandardCharsets.UTF_8);

        String baseString = "v0:" + timestamp + ":" + body;
        String expectedSignature = computeHmacSha256(baseString, signingSecret);

        if (!signatureHeader.equalsIgnoreCase(expectedSignature)) {
            log.warn("Slack signature verification failed across [{}]. Expected [{}], but received [{}]. Ensure SYNAPSE_SLACK_SIGNING_SECRET is configured correctly.", path, expectedSignature, signatureHeader);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(cachedRequest, response);
    }

    private String computeHmacSha256(String baseString, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] rawHmac = mac.doFinal(baseString.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : rawHmac) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return "v0=" + hexString;
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate HMAC-SHA256 for Slack signature verification", e);
        }
    }

    @Getter
    private static class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {
        private final byte[] cachedBody;
        private final Map<String, String[]> formParameters;

        public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
            super(request);
            this.cachedBody = request.getInputStream().readAllBytes();
            String contentType = request.getContentType();
            if (contentType != null && contentType.toLowerCase().contains("application/x-www-form-urlencoded")) {
                this.formParameters = parseFormParameters(this.cachedBody, request.getCharacterEncoding());
            } else {
                this.formParameters = null;
            }
        }

        private Map<String, String[]> parseFormParameters(byte[] body, String characterEncoding) {
            Map<String, List<String>> map = new LinkedHashMap<>();
            Charset charset = StandardCharsets.UTF_8;
            if (characterEncoding != null && !characterEncoding.isBlank()) {
                try {
                    charset = Charset.forName(characterEncoding);
                } catch (Exception _) {
                    log.warn("Invalid character encoding: {}", characterEncoding);
                }
            }
            String content = new String(body, charset);
            if (!content.isEmpty()) {
                for (String pair : content.split("&")) {
                    int idx = pair.indexOf('=');
                    String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), charset) : pair;
                    String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1), charset) : "";
                    map.computeIfAbsent(key, _ -> new ArrayList<>()).add(value);
                }
            }
            Map<String, String[]> result = new LinkedHashMap<>();
            map.forEach((k, v) -> result.put(k, v.toArray(new String[0])));
            return result;
        }

        @Override
        public ServletInputStream getInputStream() {
            return new CachedBodyServletInputStream(cachedBody);
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }

        @Override
        public String getParameter(String name) {
            if (formParameters != null && formParameters.containsKey(name)) {
                String[] values = formParameters.get(name);
                return values != null && values.length > 0 ? values[0] : null;
            }
            return super.getParameter(name);
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            if (formParameters != null) {
                return Collections.unmodifiableMap(formParameters);
            }
            return super.getParameterMap();
        }

        @Override
        public Enumeration<String> getParameterNames() {
            if (formParameters != null) {
                return Collections.enumeration(formParameters.keySet());
            }
            return super.getParameterNames();
        }

        @Override
        public String[] getParameterValues(String name) {
            if (formParameters != null && formParameters.containsKey(name)) {
                return formParameters.get(name);
            }
            return super.getParameterValues(name);
        }
    }

    private static class CachedBodyServletInputStream extends ServletInputStream {
        private final ByteArrayInputStream inputStream;

        public CachedBodyServletInputStream(byte[] cachedBody) {
            this.inputStream = new ByteArrayInputStream(cachedBody);
        }

        @Override
        public boolean isFinished() {
            return inputStream.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            // No-op for synchronous byte array input stream
        }

        @Override
        public int read() {
            return inputStream.read();
        }
    }
}
