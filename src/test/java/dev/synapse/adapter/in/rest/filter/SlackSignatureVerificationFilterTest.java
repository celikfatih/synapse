package dev.synapse.adapter.in.rest.filter;

import dev.synapse.shared.config.SlackProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SlackSignatureVerificationFilterTest {

    @Mock
    private SlackProperties slackProperties;

    @Mock
    private FilterChain filterChain;

    private SlackSignatureVerificationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new SlackSignatureVerificationFilter(slackProperties);
    }

    @Test
    void doFilterInternal_ShouldPassThroughWhenSigningSecretIsNull() throws ServletException, IOException {
        when(slackProperties.getSigningSecret()).thenReturn(null);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/slack/events");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void doFilterInternal_ShouldPassThroughWhenSigningSecretIsBlank() throws ServletException, IOException {
        when(slackProperties.getSigningSecret()).thenReturn("   ");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/slack/events");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void doFilter_ShouldPassThroughWhenRequestIsNotSlackApi() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/tasks/submit");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void doFilterInternal_ShouldReturn401WhenSignatureHeaderIsMissing() throws ServletException, IOException {
        when(slackProperties.getSigningSecret()).thenReturn("test-secret");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/slack/events");
        request.addHeader("X-Slack-Request-Timestamp", String.valueOf(Instant.now().getEpochSecond()));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void doFilterInternal_ShouldReturn401WhenTimestampIsTooOld() throws ServletException, IOException {
        when(slackProperties.getSigningSecret()).thenReturn("test-secret");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/slack/events");
        long oldTimestamp = Instant.now().getEpochSecond() - 600; // 10 minutes ago
        request.addHeader("X-Slack-Request-Timestamp", String.valueOf(oldTimestamp));
        request.addHeader("X-Slack-Signature", "v0=dummy");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void doFilterInternal_ShouldReturn401WhenSignatureIsInvalid() throws ServletException, IOException {
        when(slackProperties.getSigningSecret()).thenReturn("test-secret");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/slack/events");
        long timestamp = Instant.now().getEpochSecond();
        request.addHeader("X-Slack-Request-Timestamp", String.valueOf(timestamp));
        request.addHeader("X-Slack-Signature", "v0=invalid-hex-signature");
        request.setContent("{\"type\":\"url_verification\"}".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void doFilterInternal_ShouldPassThroughWhenSignatureIsValid() throws Exception {
        String secret = "test-secret";
        when(slackProperties.getSigningSecret()).thenReturn(secret);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/slack/events");
        long timestamp = Instant.now().getEpochSecond();
        String body = "{\"type\":\"url_verification\"}";
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        request.addHeader("X-Slack-Request-Timestamp", String.valueOf(timestamp));

        String baseString = "v0:" + timestamp + ":" + body;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] rawHmac = mac.doFinal(baseString.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : rawHmac) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        String validSignature = "v0=" + hexString;
        request.addHeader("X-Slack-Signature", validSignature);

        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void doFilterInternal_ShouldPassThroughWhenSlackPropertiesIsNull() throws ServletException, IOException {
        SlackSignatureVerificationFilter nullPropFilter = new SlackSignatureVerificationFilter(null);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/slack/events");
        MockHttpServletResponse response = new MockHttpServletResponse();

        nullPropFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void doFilterInternal_ShouldReturn401WhenTimestampIsInvalidNumber() throws ServletException, IOException {
        when(slackProperties.getSigningSecret()).thenReturn("test-secret");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/slack/events");
        request.addHeader("X-Slack-Request-Timestamp", "not-a-number");
        request.addHeader("X-Slack-Signature", "v0=dummy");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void doFilterInternal_ShouldVerifyCachedRequestMethods() throws Exception {
        String secret = "test-secret";
        when(slackProperties.getSigningSecret()).thenReturn(secret);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/slack/events");
        long timestamp = Instant.now().getEpochSecond();
        String body = "{\"type\":\"url_verification\"}";
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        request.addHeader("X-Slack-Request-Timestamp", String.valueOf(timestamp));

        String baseString = "v0:" + timestamp + ":" + body;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] rawHmac = mac.doFinal(baseString.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : rawHmac) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        request.addHeader("X-Slack-Signature", "v0=" + hexString);

        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, (req, _) -> {
            assertThat(req.getReader().readLine()).isEqualTo(body);
            jakarta.servlet.ServletInputStream sis = req.getInputStream();
            assertThat(sis.isReady()).isTrue();
            sis.readAllBytes();
            assertThat(sis.isFinished()).isTrue();
            sis.setReadListener(null);
        });

        assertThat(response.getStatus()).isEqualTo(200);
    }
}
