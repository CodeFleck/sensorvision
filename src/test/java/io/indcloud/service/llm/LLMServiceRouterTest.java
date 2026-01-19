package io.indcloud.service.llm;

import io.indcloud.dto.llm.LLMRequest;
import io.indcloud.dto.llm.LLMResponse;
import io.indcloud.model.LLMFeatureType;
import io.indcloud.model.LLMProvider;
import io.indcloud.model.Organization;
import io.indcloud.model.User;
import io.indcloud.repository.LLMUsageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for LLMServiceRouter.
 * Verifies provider routing, fallback behavior, and error handling.
 */
@ExtendWith(MockitoExtension.class)
class LLMServiceRouterTest {

    @Mock
    private LLMUsageRepository usageRepository;

    private LLMServiceRouter router;

    @Nested
    @DisplayName("No Provider Configured Tests")
    class NoProviderConfiguredTests {

        @BeforeEach
        void setUp() {
            // Create mock LLM services that are NOT available
            LLMService unavailableClaude = createUnavailableService(LLMProvider.CLAUDE);
            LLMService unavailableOpenAI = createUnavailableService(LLMProvider.OPENAI);
            LLMService unavailableGemini = createUnavailableService(LLMProvider.GEMINI);

            router = new LLMServiceRouter(
                    List.of(unavailableClaude, unavailableOpenAI, unavailableGemini),
                    usageRepository,
                    "CLAUDE"
            );
        }

        @Test
        @DisplayName("Should return failure response when no provider is configured")
        void shouldReturnFailureWhenNoProviderConfigured() {
            Organization org = Organization.builder().id(1L).name("Test Org").build();
            User user = User.builder().id(1L).username("testuser").organization(org).build();

            LLMRequest request = LLMRequest.builder()
                    .featureType(LLMFeatureType.NATURAL_LANGUAGE_QUERY)
                    .systemPrompt("Test system prompt")
                    .userMessage("What is the temperature?")
                    .build();

            LLMResponse response = router.complete(request, org, user).block();

            assertNotNull(response);
            assertFalse(response.isSuccess(), "Response should indicate failure");
            assertEquals("No LLM provider is configured", response.getErrorMessage());
            assertEquals(LLMProvider.CLAUDE, response.getProvider());
            assertNull(response.getContent());
        }

        @Test
        @DisplayName("Should not attempt to track usage when no provider available")
        void shouldNotTrackUsageWhenNoProviderAvailable() {
            Organization org = Organization.builder().id(1L).name("Test Org").build();

            LLMRequest request = LLMRequest.builder()
                    .featureType(LLMFeatureType.NATURAL_LANGUAGE_QUERY)
                    .userMessage("Test query")
                    .build();

            LLMResponse response = router.complete(request, org).block();

            assertNotNull(response);
            assertFalse(response.isSuccess());

            // Verify usage was not tracked
            verify(usageRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should report no available providers")
        void shouldReportNoAvailableProviders() {
            assertTrue(router.getAvailableProviders().isEmpty());
            assertFalse(router.isAnyProviderAvailable());
        }
    }

    @Nested
    @DisplayName("Provider Fallback Tests")
    class ProviderFallbackTests {

        @BeforeEach
        void setUp() {
            // Create mock services: Claude unavailable, OpenAI available
            LLMService unavailableClaude = createUnavailableService(LLMProvider.CLAUDE);
            LLMService availableOpenAI = createAvailableService(LLMProvider.OPENAI);

            router = new LLMServiceRouter(
                    List.of(unavailableClaude, availableOpenAI),
                    usageRepository,
                    "CLAUDE"
            );
        }

        @Test
        @DisplayName("Should fallback to available provider when default is unavailable")
        void shouldFallbackToAvailableProvider() {
            Organization org = Organization.builder().id(1L).name("Test Org").build();
            User user = User.builder().id(1L).username("testuser").organization(org).build();

            LLMRequest request = LLMRequest.builder()
                    .featureType(LLMFeatureType.NATURAL_LANGUAGE_QUERY)
                    .userMessage("Test query")
                    .build();

            LLMResponse response = router.complete(request, org, user).block();

            assertNotNull(response);
            assertTrue(response.isSuccess());
            assertEquals(LLMProvider.OPENAI, response.getProvider());
            assertEquals("Mock response from OPENAI", response.getContent());
        }

        @Test
        @DisplayName("Should report only available provider")
        void shouldReportOnlyAvailableProvider() {
            List<LLMProvider> available = router.getAvailableProviders();
            assertEquals(1, available.size());
            assertTrue(available.contains(LLMProvider.OPENAI));
            assertTrue(router.isAnyProviderAvailable());
        }
    }

    @Nested
    @DisplayName("Successful Completion Tests")
    class SuccessfulCompletionTests {

        @BeforeEach
        void setUp() {
            LLMService availableClaude = createAvailableService(LLMProvider.CLAUDE);

            router = new LLMServiceRouter(
                    List.of(availableClaude),
                    usageRepository,
                    "CLAUDE"
            );
        }

        @Test
        @DisplayName("Should complete request successfully")
        void shouldCompleteRequestSuccessfully() {
            Organization org = Organization.builder().id(1L).name("Test Org").build();
            User user = User.builder().id(1L).username("testuser").organization(org).build();

            LLMRequest request = LLMRequest.builder()
                    .featureType(LLMFeatureType.NATURAL_LANGUAGE_QUERY)
                    .userMessage("What is the temperature?")
                    .build();

            LLMResponse response = router.complete(request, org, user).block();

            assertNotNull(response);
            assertTrue(response.isSuccess());
            assertEquals(LLMProvider.CLAUDE, response.getProvider());
            assertNotNull(response.getContent());
        }

        @Test
        @DisplayName("Should track usage on successful completion")
        void shouldTrackUsageOnSuccess() {
            Organization org = Organization.builder().id(1L).name("Test Org").build();
            User user = User.builder().id(1L).username("testuser").organization(org).build();

            LLMRequest request = LLMRequest.builder()
                    .featureType(LLMFeatureType.NATURAL_LANGUAGE_QUERY)
                    .userMessage("Test query")
                    .build();

            // Execute and wait for completion
            router.complete(request, org, user).block();

            // Verify usage was tracked
            verify(usageRepository).save(any());
        }
    }

    // Helper methods to create mock services

    private LLMService createUnavailableService(LLMProvider provider) {
        return new LLMService() {
            @Override
            public LLMProvider getProvider() {
                return provider;
            }

            @Override
            public boolean isAvailable() {
                return false;
            }

            @Override
            public Mono<LLMResponse> complete(LLMRequest request) {
                return Mono.just(LLMResponse.failure(provider, "Service not available"));
            }

            @Override
            public String getDefaultModelId() {
                return "mock-model";
            }

            @Override
            public int estimateCostCents(int inputTokens, int outputTokens) {
                return 0;
            }
        };
    }

    private LLMService createAvailableService(LLMProvider provider) {
        return new LLMService() {
            @Override
            public LLMProvider getProvider() {
                return provider;
            }

            @Override
            public boolean isAvailable() {
                return true;
            }

            @Override
            public Mono<LLMResponse> complete(LLMRequest request) {
                return Mono.just(LLMResponse.success(
                        provider,
                        "mock-model",
                        "Mock response from " + provider,
                        100,
                        50,
                        1000
                ));
            }

            @Override
            public String getDefaultModelId() {
                return "mock-model";
            }

            @Override
            public int estimateCostCents(int inputTokens, int outputTokens) {
                return 1;
            }
        };
    }
}
