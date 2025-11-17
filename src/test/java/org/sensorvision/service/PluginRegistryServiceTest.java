package org.sensorvision.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sensorvision.model.*;
import org.sensorvision.repository.InstalledPluginRepository;
import org.sensorvision.repository.PluginRatingRepository;
import org.sensorvision.repository.PluginRegistryRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for PluginRegistryService
 */
@ExtendWith(MockitoExtension.class)
class PluginRegistryServiceTest {

    @Mock
    private PluginRegistryRepository pluginRegistryRepository;

    @Mock
    private InstalledPluginRepository installedPluginRepository;

    @Mock
    private PluginRatingRepository pluginRatingRepository;

    @InjectMocks
    private PluginRegistryService pluginRegistryService;

    @Captor
    private ArgumentCaptor<PluginRegistry> pluginCaptor;

    @Captor
    private ArgumentCaptor<PluginRating> ratingCaptor;

    private Organization testOrganization;
    private PluginRegistry testPlugin;
    private PluginRating testRating;

    @BeforeEach
    void setUp() {
        testOrganization = Organization.builder()
                .id(1L)
                .name("Test Organization")
                .build();

        testPlugin = new PluginRegistry();
        testPlugin.setId(1L);
        testPlugin.setPluginKey("lorawan-ttn");
        testPlugin.setName("LoRaWAN TTN Integration");
        testPlugin.setDescription("Connect to The Things Network");
        testPlugin.setVersion("1.0.0");
        testPlugin.setAuthor("SensorVision Team");
        testPlugin.setCategory(PluginCategory.PROTOCOL_PARSER);
        testPlugin.setIsOfficial(true);
        testPlugin.setIsVerified(true);
        testPlugin.setInstallationCount(100);
        testPlugin.setRatingAverage(BigDecimal.valueOf(4.5));
        testPlugin.setRatingCount(20);
        // createdAt and updatedAt are set automatically by @PrePersist

        testRating = new PluginRating(testPlugin, testOrganization, 5);
        testRating.setReviewText("Excellent plugin!");
    }

    @Test
    void shouldGetAllPlugins() {
        // Arrange
        List<PluginRegistry> plugins = Arrays.asList(testPlugin);
        when(pluginRegistryRepository.findAll()).thenReturn(plugins);

        // Act
        List<PluginRegistry> result = pluginRegistryService.getAllPlugins();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPluginKey()).isEqualTo("lorawan-ttn");
        verify(pluginRegistryRepository).findAll();
    }

    @Test
    void shouldGetPluginByKey() {
        // Arrange
        when(pluginRegistryRepository.findByPluginKey("lorawan-ttn"))
                .thenReturn(Optional.of(testPlugin));

        // Act
        Optional<PluginRegistry> result = pluginRegistryService.getPluginByKey("lorawan-ttn");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("LoRaWAN TTN Integration");
        verify(pluginRegistryRepository).findByPluginKey("lorawan-ttn");
    }

    @Test
    void shouldSearchPlugins() {
        // Arrange
        List<PluginRegistry> plugins = Arrays.asList(testPlugin);
        when(pluginRegistryRepository.searchPlugins("lorawan")).thenReturn(plugins);

        // Act
        List<PluginRegistry> result = pluginRegistryService.searchPlugins("lorawan");

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).contains("LoRaWAN");
        verify(pluginRegistryRepository).searchPlugins("lorawan");
    }

    @Test
    void shouldReturnAllPluginsWhenSearchTermIsEmpty() {
        // Arrange
        List<PluginRegistry> plugins = Arrays.asList(testPlugin);
        when(pluginRegistryRepository.findAll()).thenReturn(plugins);

        // Act
        List<PluginRegistry> result = pluginRegistryService.searchPlugins("");

        // Assert
        assertThat(result).hasSize(1);
        verify(pluginRegistryRepository).findAll();
        verify(pluginRegistryRepository, never()).searchPlugins(any());
    }

    @Test
    void shouldGetPluginsByCategory() {
        // Arrange
        List<PluginRegistry> plugins = Arrays.asList(testPlugin);
        when(pluginRegistryRepository.findByCategory(PluginCategory.PROTOCOL_PARSER))
                .thenReturn(plugins);

        // Act
        List<PluginRegistry> result = pluginRegistryService.getPluginsByCategory(
                PluginCategory.PROTOCOL_PARSER
        );

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategory()).isEqualTo(PluginCategory.PROTOCOL_PARSER);
        verify(pluginRegistryRepository).findByCategory(PluginCategory.PROTOCOL_PARSER);
    }

    @Test
    void shouldSearchPluginsByCategory() {
        // Arrange
        List<PluginRegistry> plugins = Arrays.asList(testPlugin);
        when(pluginRegistryRepository.searchPluginsByCategory(
                PluginCategory.PROTOCOL_PARSER, "lorawan"
        )).thenReturn(plugins);

        // Act
        List<PluginRegistry> result = pluginRegistryService.searchPluginsByCategory(
                PluginCategory.PROTOCOL_PARSER, "lorawan"
        );

        // Assert
        assertThat(result).hasSize(1);
        verify(pluginRegistryRepository).searchPluginsByCategory(
                PluginCategory.PROTOCOL_PARSER, "lorawan"
        );
    }

    @Test
    void shouldGetOfficialAndVerifiedPlugins() {
        // Arrange
        List<PluginRegistry> plugins = Arrays.asList(testPlugin);
        when(pluginRegistryRepository.findOfficialAndVerified()).thenReturn(plugins);

        // Act
        List<PluginRegistry> result = pluginRegistryService.getOfficialAndVerifiedPlugins();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIsOfficial()).isTrue();
        assertThat(result.get(0).getIsVerified()).isTrue();
        verify(pluginRegistryRepository).findOfficialAndVerified();
    }

    @Test
    void shouldGetMostPopularPlugins() {
        // Arrange
        List<PluginRegistry> plugins = Arrays.asList(testPlugin);
        when(pluginRegistryRepository.findMostPopular()).thenReturn(plugins);

        // Act
        List<PluginRegistry> result = pluginRegistryService.getMostPopularPlugins();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getInstallationCount()).isEqualTo(100);
        verify(pluginRegistryRepository).findMostPopular();
    }

    @Test
    void shouldGetTopRatedPlugins() {
        // Arrange
        List<PluginRegistry> plugins = Arrays.asList(testPlugin);
        when(pluginRegistryRepository.findTopRated()).thenReturn(plugins);

        // Act
        List<PluginRegistry> result = pluginRegistryService.getTopRatedPlugins();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRatingAverage()).isEqualByComparingTo(BigDecimal.valueOf(4.5));
        verify(pluginRegistryRepository).findTopRated();
    }

    @Test
    void shouldGetRecentPlugins() {
        // Arrange
        List<PluginRegistry> plugins = Arrays.asList(testPlugin);
        when(pluginRegistryRepository.findRecent()).thenReturn(plugins);

        // Act
        List<PluginRegistry> result = pluginRegistryService.getRecentPlugins();

        // Assert
        assertThat(result).hasSize(1);
        verify(pluginRegistryRepository).findRecent();
    }

    @Test
    void shouldRegisterNewPlugin() {
        // Arrange
        when(pluginRegistryRepository.save(any(PluginRegistry.class))).thenReturn(testPlugin);

        // Act
        PluginRegistry result = pluginRegistryService.registerPlugin(testPlugin);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getPluginKey()).isEqualTo("lorawan-ttn");
        verify(pluginRegistryRepository).save(testPlugin);
    }

    @Test
    void shouldUpdatePlugin() {
        // Arrange
        PluginRegistry updatedPlugin = new PluginRegistry();
        updatedPlugin.setName("Updated Name");
        updatedPlugin.setDescription("Updated description");
        updatedPlugin.setVersion("1.1.0");

        when(pluginRegistryRepository.findByPluginKey("lorawan-ttn"))
                .thenReturn(Optional.of(testPlugin));
        when(pluginRegistryRepository.save(any(PluginRegistry.class))).thenReturn(testPlugin);

        // Act
        PluginRegistry result = pluginRegistryService.updatePlugin("lorawan-ttn", updatedPlugin);

        // Assert
        verify(pluginRegistryRepository).save(pluginCaptor.capture());
        PluginRegistry saved = pluginCaptor.getValue();
        assertThat(saved.getName()).isEqualTo("Updated Name");
        assertThat(saved.getVersion()).isEqualTo("1.1.0");
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentPlugin() {
        // Arrange
        when(pluginRegistryRepository.findByPluginKey("non-existent"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> pluginRegistryService.updatePlugin("non-existent", testPlugin))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Plugin not found");
    }

    @Test
    void shouldRatePlugin() {
        // Arrange
        when(pluginRegistryRepository.findByPluginKey("lorawan-ttn"))
                .thenReturn(Optional.of(testPlugin));
        when(pluginRatingRepository.findByPluginRegistryAndOrganization(testPlugin, testOrganization))
                .thenReturn(Optional.empty());
        when(pluginRatingRepository.save(any(PluginRating.class))).thenReturn(testRating);
        when(pluginRatingRepository.calculateAverageRating(testPlugin))
                .thenReturn(BigDecimal.valueOf(4.6));
        when(pluginRatingRepository.countRatings(testPlugin)).thenReturn(21L);

        // Act
        PluginRating result = pluginRegistryService.ratePlugin(
                "lorawan-ttn", testOrganization, 5, "Excellent plugin!"
        );

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getRating()).isEqualTo(5);
        verify(pluginRatingRepository).save(any(PluginRating.class));
        verify(pluginRegistryRepository).save(testPlugin);
    }

    @Test
    void shouldUpdateExistingRating() {
        // Arrange
        when(pluginRegistryRepository.findByPluginKey("lorawan-ttn"))
                .thenReturn(Optional.of(testPlugin));
        when(pluginRatingRepository.findByPluginRegistryAndOrganization(testPlugin, testOrganization))
                .thenReturn(Optional.of(testRating));
        when(pluginRatingRepository.save(any(PluginRating.class))).thenReturn(testRating);
        when(pluginRatingRepository.calculateAverageRating(testPlugin))
                .thenReturn(BigDecimal.valueOf(4.7));
        when(pluginRatingRepository.countRatings(testPlugin)).thenReturn(20L);

        // Act
        PluginRating result = pluginRegistryService.ratePlugin(
                "lorawan-ttn", testOrganization, 4, "Updated review"
        );

        // Assert
        verify(pluginRatingRepository).save(ratingCaptor.capture());
        PluginRating saved = ratingCaptor.getValue();
        assertThat(saved.getRating()).isEqualTo(4);
        assertThat(saved.getReviewText()).isEqualTo("Updated review");
    }

    @Test
    void shouldRejectInvalidRating() {
        // Arrange
        lenient().when(pluginRegistryRepository.findByPluginKey("lorawan-ttn"))
                .thenReturn(Optional.of(testPlugin));

        // Act & Assert
        assertThatThrownBy(() -> pluginRegistryService.ratePlugin(
                "lorawan-ttn", testOrganization, 6, "Invalid rating"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rating must be between 1 and 5");

        assertThatThrownBy(() -> pluginRegistryService.ratePlugin(
                "lorawan-ttn", testOrganization, 0, "Invalid rating"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rating must be between 1 and 5");
    }

    @Test
    void shouldUpdatePluginRatingStats() {
        // Arrange
        when(pluginRatingRepository.calculateAverageRating(testPlugin))
                .thenReturn(BigDecimal.valueOf(4.55));
        when(pluginRatingRepository.countRatings(testPlugin)).thenReturn(25L);
        when(pluginRegistryRepository.save(any(PluginRegistry.class))).thenReturn(testPlugin);

        // Act
        pluginRegistryService.updatePluginRatingStats(testPlugin);

        // Assert
        verify(pluginRegistryRepository).save(pluginCaptor.capture());
        PluginRegistry saved = pluginCaptor.getValue();
        assertThat(saved.getRatingAverage()).isEqualByComparingTo(BigDecimal.valueOf(4.55));
        assertThat(saved.getRatingCount()).isEqualTo(25);
    }

    @Test
    void shouldIncrementInstallationCount() {
        // Arrange
        when(pluginRegistryRepository.updateInstallationCount("lorawan-ttn"))
                .thenReturn(1); // 1 row updated

        // Act
        pluginRegistryService.incrementInstallationCount("lorawan-ttn");

        // Assert
        verify(pluginRegistryRepository).updateInstallationCount("lorawan-ttn");
    }

    @Test
    void shouldGetPluginRatings() {
        // Arrange
        List<PluginRating> ratings = Arrays.asList(testRating);
        when(pluginRegistryRepository.findByPluginKey("lorawan-ttn"))
                .thenReturn(Optional.of(testPlugin));
        when(pluginRatingRepository.findByPluginRegistry(testPlugin)).thenReturn(ratings);

        // Act
        List<PluginRating> result = pluginRegistryService.getPluginRatings("lorawan-ttn");

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRating()).isEqualTo(5);
        verify(pluginRatingRepository).findByPluginRegistry(testPlugin);
    }

    @Test
    void shouldCheckIfPluginExists() {
        // Arrange
        when(pluginRegistryRepository.findByPluginKey("lorawan-ttn"))
                .thenReturn(Optional.of(testPlugin));
        when(pluginRegistryRepository.findByPluginKey("non-existent"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThat(pluginRegistryService.pluginExists("lorawan-ttn")).isTrue();
        assertThat(pluginRegistryService.pluginExists("non-existent")).isFalse();
    }
}
