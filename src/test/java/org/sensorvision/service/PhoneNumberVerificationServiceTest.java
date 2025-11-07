package org.sensorvision.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sensorvision.model.User;
import org.sensorvision.model.UserPhoneNumber;
import org.sensorvision.repository.UserPhoneNumberRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PhoneNumberVerificationServiceTest {

    @Mock
    private UserPhoneNumberRepository phoneNumberRepository;

    @Mock
    private SmsNotificationService smsNotificationService;

    @InjectMocks
    private PhoneNumberVerificationService verificationService;

    private User testUser;
    private UserPhoneNumber testPhoneNumber;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testPhoneNumber = UserPhoneNumber.builder()
            .id(UUID.randomUUID())
            .user(testUser)
            .phoneNumber("+15551234567")
            .countryCode("US")
            .verified(false)
            .verificationCode("123456")
            .verificationExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
            .isPrimary(false)
            .enabled(true)
            .build();
    }

    @Test
    void testAddPhoneNumber_FirstPhone_SetAsPrimary() {
        // Given
        when(phoneNumberRepository.findByUserIdAndPhoneNumber(1L, "+15551234567"))
            .thenReturn(Optional.empty());
        when(phoneNumberRepository.findByUserId(1L))
            .thenReturn(Collections.emptyList()); // No existing phones
        when(phoneNumberRepository.save(any(UserPhoneNumber.class)))
            .thenReturn(testPhoneNumber);

        // When
        UserPhoneNumber result = verificationService.addPhoneNumber(
            testUser,
            "+15551234567",
            "US"
        );

        // Then
        assertNotNull(result);
        verify(phoneNumberRepository).save(argThat(phone ->
            phone.getPhoneNumber().equals("+15551234567") &&
            phone.getIsPrimary() == true &&
            phone.getVerified() == false &&
            phone.getVerificationCode() != null
        ));
    }

    @Test
    void testAddPhoneNumber_SecondPhone_NotPrimary() {
        // Given
        UserPhoneNumber existingPhone = UserPhoneNumber.builder()
            .id(UUID.randomUUID())
            .user(testUser)
            .phoneNumber("+15559876543")
            .isPrimary(true)
            .verified(true)
            .build();

        when(phoneNumberRepository.findByUserIdAndPhoneNumber(1L, "+15551234567"))
            .thenReturn(Optional.empty());
        when(phoneNumberRepository.findByUserId(1L))
            .thenReturn(List.of(existingPhone)); // One existing phone
        when(phoneNumberRepository.save(any(UserPhoneNumber.class)))
            .thenReturn(testPhoneNumber);

        // When
        UserPhoneNumber result = verificationService.addPhoneNumber(
            testUser,
            "+15551234567",
            "US"
        );

        // Then
        assertNotNull(result);
        verify(phoneNumberRepository).save(argThat(phone ->
            phone.getPhoneNumber().equals("+15551234567") &&
            phone.getIsPrimary() == false
        ));
    }

    @Test
    void testAddPhoneNumber_DuplicatePhone_ThrowsException() {
        // Given
        when(phoneNumberRepository.findByUserIdAndPhoneNumber(1L, "+15551234567"))
            .thenReturn(Optional.of(testPhoneNumber));

        // When / Then
        assertThrows(IllegalArgumentException.class, () ->
            verificationService.addPhoneNumber(testUser, "+15551234567", "US")
        );

        verify(phoneNumberRepository, never()).save(any());
    }

    @Test
    void testVerifyPhoneNumber_ValidCode_ReturnsTrue() {
        // Given
        when(phoneNumberRepository.findByUserIdAndPhoneNumber(1L, "+15551234567"))
            .thenReturn(Optional.of(testPhoneNumber));
        when(phoneNumberRepository.save(any(UserPhoneNumber.class)))
            .thenReturn(testPhoneNumber);

        // When
        boolean result = verificationService.verifyPhoneNumber(
            testUser,
            "+15551234567",
            "123456"
        );

        // Then
        assertTrue(result);
        verify(phoneNumberRepository).save(argThat(phone ->
            phone.getVerified() == true &&
            phone.getVerificationCode() == null &&
            phone.getVerificationExpiresAt() == null
        ));
    }

    @Test
    void testVerifyPhoneNumber_InvalidCode_ReturnsFalse() {
        // Given
        when(phoneNumberRepository.findByUserIdAndPhoneNumber(1L, "+15551234567"))
            .thenReturn(Optional.of(testPhoneNumber));

        // When
        boolean result = verificationService.verifyPhoneNumber(
            testUser,
            "+15551234567",
            "999999" // Wrong code
        );

        // Then
        assertFalse(result);
        verify(phoneNumberRepository, never()).save(any());
    }

    @Test
    void testVerifyPhoneNumber_ExpiredCode_ReturnsFalse() {
        // Given
        testPhoneNumber.setVerificationExpiresAt(
            Instant.now().minus(1, ChronoUnit.HOURS) // Expired
        );
        when(phoneNumberRepository.findByUserIdAndPhoneNumber(1L, "+15551234567"))
            .thenReturn(Optional.of(testPhoneNumber));

        // When
        boolean result = verificationService.verifyPhoneNumber(
            testUser,
            "+15551234567",
            "123456"
        );

        // Then
        assertFalse(result);
        verify(phoneNumberRepository, never()).save(any());
    }

    @Test
    void testVerifyPhoneNumber_PhoneNotFound_ReturnsFalse() {
        // Given
        when(phoneNumberRepository.findByUserIdAndPhoneNumber(1L, "+15551234567"))
            .thenReturn(Optional.empty());

        // When
        boolean result = verificationService.verifyPhoneNumber(
            testUser,
            "+15551234567",
            "123456"
        );

        // Then
        assertFalse(result);
        verify(phoneNumberRepository, never()).save(any());
    }

    @Test
    void testResendVerificationCode_AlreadyVerified_ThrowsException() {
        // Given
        testPhoneNumber.setVerified(true);
        when(phoneNumberRepository.findByUserIdAndPhoneNumber(1L, "+15551234567"))
            .thenReturn(Optional.of(testPhoneNumber));

        // When / Then
        assertThrows(IllegalStateException.class, () ->
            verificationService.resendVerificationCode(testUser, "+15551234567")
        );

        verify(phoneNumberRepository, never()).save(any());
    }

    @Test
    void testResendVerificationCode_ValidRequest_GeneratesNewCode() {
        // Given
        when(phoneNumberRepository.findByUserIdAndPhoneNumber(1L, "+15551234567"))
            .thenReturn(Optional.of(testPhoneNumber));
        when(phoneNumberRepository.save(any(UserPhoneNumber.class)))
            .thenReturn(testPhoneNumber);

        String oldCode = testPhoneNumber.getVerificationCode();

        // When
        verificationService.resendVerificationCode(testUser, "+15551234567");

        // Then
        verify(phoneNumberRepository).save(argThat(phone ->
            phone.getVerificationCode() != null &&
            !phone.getVerificationCode().equals(oldCode) &&
            phone.getVerificationExpiresAt() != null
        ));
    }

    @Test
    void testSetPrimaryPhoneNumber_UnverifiedPhone_ThrowsException() {
        // Given
        when(phoneNumberRepository.findById(testPhoneNumber.getId()))
            .thenReturn(Optional.of(testPhoneNumber));

        // When / Then
        assertThrows(IllegalStateException.class, () ->
            verificationService.setPrimaryPhoneNumber(testUser, testPhoneNumber.getId())
        );
    }

    @Test
    void testSetPrimaryPhoneNumber_VerifiedPhone_UpdatesPrimary() {
        // Given
        testPhoneNumber.setVerified(true);
        UserPhoneNumber oldPrimary = UserPhoneNumber.builder()
            .id(UUID.randomUUID())
            .user(testUser)
            .phoneNumber("+15559876543")
            .isPrimary(true)
            .verified(true)
            .build();

        when(phoneNumberRepository.findById(testPhoneNumber.getId()))
            .thenReturn(Optional.of(testPhoneNumber));
        when(phoneNumberRepository.findByUserId(1L))
            .thenReturn(List.of(oldPrimary, testPhoneNumber));

        // When
        verificationService.setPrimaryPhoneNumber(testUser, testPhoneNumber.getId());

        // Then
        verify(phoneNumberRepository).save(argThat(phone ->
            phone.getId().equals(oldPrimary.getId()) && phone.getIsPrimary() == false
        ));
        verify(phoneNumberRepository).save(argThat(phone ->
            phone.getId().equals(testPhoneNumber.getId()) && phone.getIsPrimary() == true
        ));
    }

    @Test
    void testRemovePhoneNumber_LastPhone_Allowed() {
        // Given
        testPhoneNumber.setIsPrimary(true);
        when(phoneNumberRepository.findById(testPhoneNumber.getId()))
            .thenReturn(Optional.of(testPhoneNumber));
        when(phoneNumberRepository.findByUserId(1L))
            .thenReturn(List.of(testPhoneNumber)); // Only one phone

        // When
        verificationService.removePhoneNumber(testUser, testPhoneNumber.getId());

        // Then
        verify(phoneNumberRepository).delete(testPhoneNumber);
    }

    @Test
    void testRemovePhoneNumber_PrimaryWithOthers_ThrowsException() {
        // Given
        testPhoneNumber.setIsPrimary(true);
        UserPhoneNumber otherPhone = UserPhoneNumber.builder()
            .id(UUID.randomUUID())
            .user(testUser)
            .phoneNumber("+15559876543")
            .isPrimary(false)
            .build();

        when(phoneNumberRepository.findById(testPhoneNumber.getId()))
            .thenReturn(Optional.of(testPhoneNumber));
        when(phoneNumberRepository.findByUserId(1L))
            .thenReturn(List.of(testPhoneNumber, otherPhone)); // Two phones

        // When / Then
        assertThrows(IllegalStateException.class, () ->
            verificationService.removePhoneNumber(testUser, testPhoneNumber.getId())
        );

        verify(phoneNumberRepository, never()).delete(any());
    }

    @Test
    void testGetUserPhoneNumbers_ReturnsAllPhones() {
        // Given
        List<UserPhoneNumber> phones = List.of(testPhoneNumber);
        when(phoneNumberRepository.findByUserId(1L))
            .thenReturn(phones);

        // When
        List<UserPhoneNumber> result = verificationService.getUserPhoneNumbers(testUser);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testPhoneNumber.getPhoneNumber(), result.get(0).getPhoneNumber());
    }

    @Test
    void testGetVerifiedPhoneNumbers_ReturnsOnlyVerified() {
        // Given
        testPhoneNumber.setVerified(true);
        List<UserPhoneNumber> phones = List.of(testPhoneNumber);
        when(phoneNumberRepository.findByUserIdAndVerifiedTrue(1L))
            .thenReturn(phones);

        // When
        List<UserPhoneNumber> result = verificationService.getVerifiedPhoneNumbers(testUser);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getVerified());
    }

    @Test
    void testGetPrimaryPhoneNumber_Found_ReturnsPhone() {
        // Given
        testPhoneNumber.setIsPrimary(true);
        when(phoneNumberRepository.findByUserIdAndIsPrimaryTrue(1L))
            .thenReturn(Optional.of(testPhoneNumber));

        // When
        Optional<UserPhoneNumber> result = verificationService.getPrimaryPhoneNumber(testUser);

        // Then
        assertTrue(result.isPresent());
        assertTrue(result.get().getIsPrimary());
        assertEquals(testPhoneNumber.getPhoneNumber(), result.get().getPhoneNumber());
    }

    @Test
    void testGetPrimaryPhoneNumber_NotFound_ReturnsEmpty() {
        // Given
        when(phoneNumberRepository.findByUserIdAndIsPrimaryTrue(1L))
            .thenReturn(Optional.empty());

        // When
        Optional<UserPhoneNumber> result = verificationService.getPrimaryPhoneNumber(testUser);

        // Then
        assertFalse(result.isPresent());
    }
}
