package org.sensorvision.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.dto.WebhookTestRequest;
import org.sensorvision.model.Organization;
import org.sensorvision.model.User;
import org.sensorvision.model.WebhookTest;
import org.sensorvision.repository.WebhookTestRepository;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Iterator;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookTestService {

    private final WebhookTestRepository webhookTestRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Transactional
    public WebhookTest executeTest(WebhookTestRequest request, Organization organization, User user) {
        log.info("Executing webhook test: {} to {}", request.httpMethod(), request.url());

        WebhookTest test = new WebhookTest();
        test.setOrganization(organization);
        test.setCreatedBy(user);
        test.setName(request.name());
        test.setUrl(request.url());
        test.setHttpMethod(request.httpMethod() != null ? request.httpMethod() : "POST");
        test.setHeaders(request.headers());
        test.setRequestBody(request.requestBody());

        long startTime = System.currentTimeMillis();

        try {
            // Build headers
            HttpHeaders headers = new HttpHeaders();
            if (request.headers() != null) {
                Iterator<Map.Entry<String, JsonNode>> fields = request.headers().fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    headers.add(field.getKey(), field.getValue().asText());
                }
            }

            // Set content type if not specified
            if (!headers.containsKey(HttpHeaders.CONTENT_TYPE)) {
                headers.setContentType(MediaType.APPLICATION_JSON);
            }

            // Build request entity
            HttpEntity<String> entity = new HttpEntity<>(request.requestBody(), headers);

            // Make HTTP request
            HttpMethod method = HttpMethod.valueOf(test.getHttpMethod());
            ResponseEntity<String> response = restTemplate.exchange(
                    request.url(),
                    method,
                    entity,
                    String.class
            );

            // Record success response
            test.setStatusCode(response.getStatusCode().value());
            test.setResponseBody(response.getBody());

            // Convert response headers to JsonNode
            ObjectNode responseHeadersNode = objectMapper.createObjectNode();
            response.getHeaders().forEach((key, values) -> {
                if (values.size() == 1) {
                    responseHeadersNode.put(key, values.get(0));
                } else {
                    var arrayNode = responseHeadersNode.putArray(key);
                    values.forEach(arrayNode::add);
                }
            });
            test.setResponseHeaders(responseHeadersNode);

            log.info("Webhook test successful: status={}, duration={}ms",
                    test.getStatusCode(), test.getDurationMs());

        } catch (Exception e) {
            log.error("Webhook test failed: {}", e.getMessage(), e);
            test.setErrorMessage(e.getMessage());
            test.setStatusCode(null);
        } finally {
            long endTime = System.currentTimeMillis();
            test.setDurationMs(endTime - startTime);
        }

        return webhookTestRepository.save(test);
    }
}
