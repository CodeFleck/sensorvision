package org.sensorvision.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LogSanitizationService")
class LogSanitizationServiceTest {

    private LogSanitizationService sanitizationService;

    @BeforeEach
    void setUp() {
        sanitizationService = new LogSanitizationService();
    }

    @Nested
    @DisplayName("sanitize method")
    class SanitizeMethod {

        @Test
        @DisplayName("should return null for null input")
        void shouldReturnNullForNullInput() {
            assertThat(sanitizationService.sanitize(null)).isNull();
        }

        @Test
        @DisplayName("should return empty string for empty input")
        void shouldReturnEmptyStringForEmptyInput() {
            assertThat(sanitizationService.sanitize("")).isEmpty();
        }

        @Test
        @DisplayName("should not modify log line without sensitive data")
        void shouldNotModifyLogLineWithoutSensitiveData() {
            String logLine = "2024-01-01 12:00:00 [main] INFO Application started successfully";
            assertThat(sanitizationService.sanitize(logLine)).isEqualTo(logLine);
        }

        @Nested
        @DisplayName("JWT Token sanitization")
        class JwtTokenSanitization {

            @Test
            @DisplayName("should redact JWT tokens")
            void shouldRedactJwtTokens() {
                String logLine = "Token received: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
                String result = sanitizationService.sanitize(logLine);
                assertThat(result).contains("[JWT_REDACTED]");
                assertThat(result).doesNotContain("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9");
            }

            @Test
            @DisplayName("should redact multiple JWT tokens in same line")
            void shouldRedactMultipleJwtTokens() {
                String logLine = "Old: eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.abc123 New: eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIyIn0.xyz789";
                String result = sanitizationService.sanitize(logLine);
                assertThat(result).doesNotContain("eyJhbGciOiJIUzI1NiJ9");
                // Should have two redactions - count occurrences correctly
                int count = 0;
                int index = 0;
                while ((index = result.indexOf("[JWT_REDACTED]", index)) != -1) {
                    count++;
                    index += "[JWT_REDACTED]".length();
                }
                assertThat(count).isEqualTo(2);
            }
        }

        @Nested
        @DisplayName("Bearer Token sanitization")
        class BearerTokenSanitization {

            @Test
            @DisplayName("should redact Bearer tokens")
            void shouldRedactBearerTokens() {
                String logLine = "Authorization header: Bearer abc123def456ghi789jkl012mno345";
                String result = sanitizationService.sanitize(logLine);
                assertThat(result).contains("[TOKEN_REDACTED]");
                assertThat(result).doesNotContain("abc123def456ghi789jkl012mno345");
            }

            @Test
            @DisplayName("should preserve Bearer keyword while redacting value")
            void shouldPreserveBearerKeyword() {
                String logLine = "Header: Bearer verylongtokenvalue12345678901234567890";
                String result = sanitizationService.sanitize(logLine);
                assertThat(result).contains("Bearer");
                assertThat(result).contains("[TOKEN_REDACTED]");
            }
        }

        @Nested
        @DisplayName("Password sanitization")
        class PasswordSanitization {

            @Test
            @DisplayName("should redact password= values")
            void shouldRedactPasswordEquals() {
                String logLine = "Config: password=mysecretpassword123";
                String result = sanitizationService.sanitize(logLine);
                assertThat(result).contains("[REDACTED]");
                assertThat(result).doesNotContain("mysecretpassword123");
            }

            @Test
            @DisplayName("should redact password: values")
            void shouldRedactPasswordColon() {
                String logLine = "Config: password: mysecretpassword123";
                String result = sanitizationService.sanitize(logLine);
                assertThat(result).contains("[REDACTED]");
                assertThat(result).doesNotContain("mysecretpassword123");
            }

            @Test
            @DisplayName("should redact passwd values")
            void shouldRedactPasswd() {
                String logLine = "passwd=admin123";
                String result = sanitizationService.sanitize(logLine);
                assertThat(result).contains("[REDACTED]");
                assertThat(result).doesNotContain("admin123");
            }

            @Test
            @DisplayName("should redact pwd values")
            void shouldRedactPwd() {
                String logLine = "pwd=secret";
                String result = sanitizationService.sanitize(logLine);
                assertThat(result).contains("[REDACTED]");
                assertThat(result).doesNotContain("secret");
            }

            @Test
            @DisplayName("should redact secret values")
            void shouldRedactSecret() {
                String logLine = "secret=topsecretvalue";
                String result = sanitizationService.sanitize(logLine);
                assertThat(result).contains("[REDACTED]");
                assertThat(result).doesNotContain("topsecretvalue");
            }

            @Test
            @DisplayName("should be case insensitive for password")
            void shouldBeCaseInsensitive() {
                String logLine = "PASSWORD=MySecret123 Password=AnotherSecret";
                String result = sanitizationService.sanitize(logLine);
                assertThat(result).doesNotContain("MySecret123");
                assertThat(result).doesNotContain("AnotherSecret");
            }
        }

        @Nested
        @DisplayName("API Key sanitization")
        class ApiKeySanitization {

            @Test
            @DisplayName("should redact api_key values")
            void shouldRedactApiKey() {
                String logLine = "Request with api_key=ak_1234567890abcdef";
                String result = sanitizationService.sanitize(logLine);
                assertThat(result).contains("[REDACTED]");
                assertThat(result).doesNotContain("ak_1234567890abcdef");
            }

            @Test
            @DisplayName("should redact apikey values")
            void shouldRedactApiKeyNoDash() {
                String logLine = "apikey=myapikey12345";
                String result = sanitizationService.sanitize(logLine);
                assertThat(result).contains("[REDACTED]");
                assertThat(result).doesNotContain("myapikey12345");
            }

            @Test
            @DisplayName("should redact api-key values")
            void shouldRedactApiKeyWithDash() {
                String logLine = "api-key: secretapikey999";
                String result = sanitizationService.sanitize(logLine);
                assertThat(result).contains("[REDACTED]");
                assertThat(result).doesNotContain("secretapikey999");
            }
        }

        @Nested
        @DisplayName("Device Token sanitization")
        class DeviceTokenSanitization {

            @Test
            @DisplayName("should redact device_token UUID values")
            void shouldRedactDeviceToken() {
                String logLine = "device_token=550e8400-e29b-41d4-a716-446655440000";
                String result = sanitizationService.sanitize(logLine);
                assertThat(result).contains("[REDACTED]");
                assertThat(result).doesNotContain("550e8400-e29b-41d4-a716-446655440000");
            }

            @Test
            @DisplayName("should redact X-Device-Token header")
            void shouldRedactXDeviceTokenHeader() {
                String logLine = "Header X-Device-Token: 550e8400-e29b-41d4-a716-446655440000";
                String result = sanitizationService.sanitize(logLine);
                assertThat(result).contains("[REDACTED]");
                assertThat(result).doesNotContain("550e8400-e29b-41d4-a716-446655440000");
            }

            @Test
            @DisplayName("should redact X-API-Key header")
            void shouldRedactXApiKeyHeader() {
                String logLine = "Header X-API-Key: abcdef123456789012345678901234567890";
                String result = sanitizationService.sanitize(logLine);
                assertThat(result).contains("[REDACTED]");
                assertThat(result).doesNotContain("abcdef123456789012345678901234567890");
            }
        }

        @Nested
        @DisplayName("Database connection string sanitization")
        class DatabaseConnectionSanitization {

            @Test
            @DisplayName("should redact JDBC password in connection string")
            void shouldRedactJdbcPassword() {
                String logLine = "Connecting to jdbc:postgresql://localhost:5432/mydb?password=dbpass123&user=admin";
                String result = sanitizationService.sanitize(logLine);
                assertThat(result).contains("[REDACTED]");
                assertThat(result).doesNotContain("dbpass123");
            }

            @Test
            @DisplayName("should redact Spring datasource password")
            void shouldRedactSpringDatasourcePassword() {
                String logLine = "spring.datasource.password = secretdbpassword";
                String result = sanitizationService.sanitize(logLine);
                assertThat(result).contains("[REDACTED]");
                assertThat(result).doesNotContain("secretdbpassword");
            }
        }

        @Nested
        @DisplayName("MQTT credentials sanitization")
        class MqttCredentialsSanitization {

            @Test
            @DisplayName("should redact MQTT_PASSWORD")
            void shouldRedactMqttPassword() {
                String logLine = "MQTT_PASSWORD=mqttsecret123";
                String result = sanitizationService.sanitize(logLine);
                assertThat(result).contains("[REDACTED]");
                assertThat(result).doesNotContain("mqttsecret123");
            }

            @Test
            @DisplayName("should redact mqtt.password property")
            void shouldRedactMqttPasswordProperty() {
                String logLine = "mqtt.password: brokerpassword";
                String result = sanitizationService.sanitize(logLine);
                assertThat(result).contains("[REDACTED]");
                assertThat(result).doesNotContain("brokerpassword");
            }
        }

        @Nested
        @DisplayName("OAuth sanitization")
        class OAuthSanitization {

            @Test
            @DisplayName("should redact client_secret")
            void shouldRedactClientSecret() {
                String logLine = "OAuth config client_secret=oauthsecret123456";
                String result = sanitizationService.sanitize(logLine);
                assertThat(result).contains("[REDACTED]");
                assertThat(result).doesNotContain("oauthsecret123456");
            }

            @Test
            @DisplayName("should redact GOOGLE_CLIENT_SECRET")
            void shouldRedactGoogleClientSecret() {
                String logLine = "GOOGLE_CLIENT_SECRET=googlesecret789";
                String result = sanitizationService.sanitize(logLine);
                assertThat(result).contains("[REDACTED]");
                assertThat(result).doesNotContain("googlesecret789");
            }
        }

        @Nested
        @DisplayName("BCrypt hash sanitization")
        class BcryptHashSanitization {

            @Test
            @DisplayName("should redact BCrypt hashes")
            void shouldRedactBcryptHashes() {
                String logLine = "Password hash: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
                String result = sanitizationService.sanitize(logLine);
                assertThat(result).contains("[BCRYPT_HASH_REDACTED]");
                assertThat(result).doesNotContain("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");
            }

            @Test
            @DisplayName("should redact BCrypt 2b variant")
            void shouldRedactBcrypt2b() {
                String logLine = "Hash: $2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4kMHuOGZxMG0.rBa";
                String result = sanitizationService.sanitize(logLine);
                assertThat(result).contains("[BCRYPT_HASH_REDACTED]");
            }
        }

        @Nested
        @DisplayName("Authorization header sanitization")
        class AuthorizationHeaderSanitization {

            @Test
            @DisplayName("should redact Basic auth credentials")
            void shouldRedactBasicAuth() {
                String logLine = "Authorization: Basic dXNlcm5hbWU6cGFzc3dvcmQ=";
                String result = sanitizationService.sanitize(logLine);
                assertThat(result).contains("[REDACTED]");
                assertThat(result).doesNotContain("dXNlcm5hbWU6cGFzc3dvcmQ=");
            }
        }

        @Nested
        @DisplayName("Third-party service credentials")
        class ThirdPartyCredentials {

            @Test
            @DisplayName("should redact TWILIO_AUTH_TOKEN")
            void shouldRedactTwilioAuthToken() {
                String logLine = "TWILIO_AUTH_TOKEN=twiliotoken123456";
                String result = sanitizationService.sanitize(logLine);
                assertThat(result).contains("[REDACTED]");
                assertThat(result).doesNotContain("twiliotoken123456");
            }

            @Test
            @DisplayName("should redact SMTP_PASSWORD")
            void shouldRedactSmtpPassword() {
                String logLine = "SMTP_PASSWORD=emailpassword789";
                String result = sanitizationService.sanitize(logLine);
                assertThat(result).contains("[REDACTED]");
                assertThat(result).doesNotContain("emailpassword789");
            }
        }

        @Nested
        @DisplayName("AWS credentials sanitization")
        class AwsCredentialsSanitization {

            @Test
            @DisplayName("should redact AWS_SECRET_ACCESS_KEY")
            void shouldRedactAwsSecretAccessKey() {
                String logLine = "AWS_SECRET_ACCESS_KEY=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY";
                String result = sanitizationService.sanitize(logLine);
                assertThat(result).contains("[REDACTED]");
                assertThat(result).doesNotContain("wJalrXUtnFEMI");
            }

            @Test
            @DisplayName("should redact AWS_ACCESS_KEY_ID")
            void shouldRedactAwsAccessKeyId() {
                String logLine = "AWS_ACCESS_KEY_ID=AKIAIOSFODNN7EXAMPLE";
                String result = sanitizationService.sanitize(logLine);
                assertThat(result).contains("[REDACTED]");
                assertThat(result).doesNotContain("AKIAIOSFODNN7EXAMPLE");
            }
        }

        @Nested
        @DisplayName("Private key sanitization")
        class PrivateKeySanitization {

            @Test
            @DisplayName("should redact PEM private keys")
            void shouldRedactPemPrivateKey() {
                String logLine = "Key loaded: -----BEGIN PRIVATE KEY-----\nMIIEvgIBADANBgkqh...\n-----END PRIVATE KEY-----";
                String result = sanitizationService.sanitize(logLine);
                assertThat(result).contains("[PRIVATE_KEY_REDACTED]");
                assertThat(result).doesNotContain("MIIEvgIBADANBgkqh");
            }

            @Test
            @DisplayName("should redact RSA private keys")
            void shouldRedactRsaPrivateKey() {
                String logLine = "-----BEGIN RSA PRIVATE KEY-----\nMIIEpAIBAAKCAQEA...\n-----END RSA PRIVATE KEY-----";
                String result = sanitizationService.sanitize(logLine);
                assertThat(result).contains("[PRIVATE_KEY_REDACTED]");
                assertThat(result).doesNotContain("MIIEpAIBAAKCAQEA");
            }
        }

        @Nested
        @DisplayName("Generic secret environment variables")
        class GenericSecretEnvVars {

            @Test
            @DisplayName("should redact generic _SECRET environment variables")
            void shouldRedactGenericSecret() {
                String logLine = "MY_APP_SECRET=verysecretvalue12345";
                String result = sanitizationService.sanitize(logLine);
                assertThat(result).contains("[REDACTED]");
                assertThat(result).doesNotContain("verysecretvalue12345");
            }

            @Test
            @DisplayName("should redact generic _CREDENTIAL environment variables")
            void shouldRedactGenericCredential() {
                String logLine = "DATABASE_CREDENTIAL=dbcredential9876";
                String result = sanitizationService.sanitize(logLine);
                assertThat(result).contains("[REDACTED]");
                assertThat(result).doesNotContain("dbcredential9876");
            }
        }

        @Test
        @DisplayName("should handle multiple sensitive data in one line")
        void shouldHandleMultipleSensitiveData() {
            String logLine = "Config: password=secret123 api_key=apikey456 token=eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.abc123";
            String result = sanitizationService.sanitize(logLine);
            assertThat(result).doesNotContain("secret123");
            assertThat(result).doesNotContain("apikey456");
            assertThat(result).doesNotContain("eyJhbGciOiJIUzI1NiJ9");
        }
    }

    @Nested
    @DisplayName("containsSensitiveData method")
    class ContainsSensitiveDataMethod {

        @Test
        @DisplayName("should return false for null input")
        void shouldReturnFalseForNull() {
            assertThat(sanitizationService.containsSensitiveData(null)).isFalse();
        }

        @Test
        @DisplayName("should return false for empty input")
        void shouldReturnFalseForEmpty() {
            assertThat(sanitizationService.containsSensitiveData("")).isFalse();
        }

        @Test
        @DisplayName("should return false for normal log line")
        void shouldReturnFalseForNormalLog() {
            assertThat(sanitizationService.containsSensitiveData("Application started successfully")).isFalse();
        }

        @Test
        @DisplayName("should return true for password keyword")
        void shouldReturnTrueForPassword() {
            assertThat(sanitizationService.containsSensitiveData("Setting password for user")).isTrue();
        }

        @Test
        @DisplayName("should return true for secret keyword")
        void shouldReturnTrueForSecret() {
            assertThat(sanitizationService.containsSensitiveData("Loading secret from vault")).isTrue();
        }

        @Test
        @DisplayName("should return true for token keyword")
        void shouldReturnTrueForToken() {
            assertThat(sanitizationService.containsSensitiveData("Generated new token")).isTrue();
        }

        @Test
        @DisplayName("should return true for api_key keyword")
        void shouldReturnTrueForApiKey() {
            assertThat(sanitizationService.containsSensitiveData("Using api_key for auth")).isTrue();
        }

        @Test
        @DisplayName("should return true for bearer keyword")
        void shouldReturnTrueForBearer() {
            assertThat(sanitizationService.containsSensitiveData("Header contains bearer")).isTrue();
        }

        @Test
        @DisplayName("should return true for JWT prefix")
        void shouldReturnTrueForJwtPrefix() {
            assertThat(sanitizationService.containsSensitiveData("Token: eyJhbGciOiJIUzI1NiJ9")).isTrue();
        }

        @Test
        @DisplayName("should return true for jdbc connection string")
        void shouldReturnTrueForJdbc() {
            assertThat(sanitizationService.containsSensitiveData("Connecting to jdbc:postgresql://localhost")).isTrue();
        }

        @Test
        @DisplayName("should be case insensitive")
        void shouldBeCaseInsensitive() {
            assertThat(sanitizationService.containsSensitiveData("PASSWORD in config")).isTrue();
            assertThat(sanitizationService.containsSensitiveData("SECRET value")).isTrue();
            assertThat(sanitizationService.containsSensitiveData("TOKEN generated")).isTrue();
        }
    }
}
