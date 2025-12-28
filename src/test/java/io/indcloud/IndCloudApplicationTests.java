package io.indcloud;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class IndCloudApplicationTests {

    @MockBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockBean
    private JavaMailSender mailSender;

    @Test
    void contextLoads() {
    }
}
