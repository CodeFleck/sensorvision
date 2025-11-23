package org.sensorvision.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sensorvision.model.*;
import org.sensorvision.repository.DataPluginRepository;
import org.sensorvision.repository.InstalledPluginRepository;
import org.sensorvision.repository.PluginRegistryRepository;

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

/**
 * Unit tests for PluginInstallationService
 */
@ExtendWith(MockitoExtension.class)
class PluginInstallationServiceTest {

    @Mock
    private InstalledPluginRepository installedPluginRepository;

    @Mock
    private PluginRegistryRepository pluginRegistryRepository;

    @Mock
    private DataPluginRepository dataPluginRepository;

    @Mock
    private PluginRegistryService pluginRegistryService;

    @InjectMocks
    private PluginInstallationService pluginInstallationService;

    @Captor
    private ArgumentCaptor<InstalledPlugin> installedPluginCaptor;

    @Captor
    private ArgumentCaptor<DataPlugin> dataPluginCaptor;

    private Organization testOrganization;
    private PluginRegistry testPluginRegistry;
    private InstalledPlugin testInstalledPlugin;
    private DataPlugin testDataPlugin;
    private JsonNode testConfiguration;

    @BeforeEach
    void setUp() throws Exception {
        testOrganization = Organization.builder()
                .id(1L)
                .name("Test Organization")
                .build();

        testPluginRegistry = new PluginRegistry();
        testPluginRegistry.setId(1L);
        testPluginRegistry.setPluginKey("lorawan-ttn");
        testPluginRegistry.setName("LoRaWAN TTN Integration");
        testPluginRegistry.setDescription("Connect to The Things Network");
        testPluginRegistry.setVersion("1.0.0");
        testPluginRegistry.setCategory(PluginCategory.PROTOCOL_PARSER);
        testPluginRegistry.setPluginType(PluginType.PROTOCOL_PARSER);
        testPluginRegistry.setPluginProvider(PluginProvider.LORAWAN_TTN);

        testInstalledPlugin = new InstalledPlugin(
                testOrganization,
                testPluginRegistry,
                "lorawan-ttn",
                "1.0.0"
        );
        testInstalledPlugin.setId(1L);
        testInstalledPlugin.setStatus(PluginInstallationStatus.INACTIVE);
        testInstalledPlugin.setInstalledAt(Instant.now());

        testDataPlugin = new DataPlugin();
        testDataPlugin.setId(1L);
        testDataPlugin.setOrganization(testOrganization);
        testDataPlugin.setName("LoRaWAN TTN Integration");
        testDataPlugin.setPluginType(PluginType.PROTOCOL_PARSER);
        testDataPlugin.setProvider(PluginProvider.LORAWAN_TTN);
        testDataPlugin.setEnabled(true);

        ObjectMapper mapper = new ObjectMapper();
        testConfiguration = mapper.readTree("{\"apiKey\":\"test-key\",\"appId\":\"test-app\"}");
    }

    @Test
    void shouldInstallPlugin() {
        // Arrange
        when(installedPluginRepository.existsByOrganizationAndPluginKey(testOrganization, "lorawan-ttn"))
                .thenReturn(false);
        when(pluginRegistryRepository.findByPluginKey("lorawan-ttn"))
                .thenReturn(Optional.of(testPluginRegistry));
        when(installedPluginRepository.save(any(InstalledPlugin.class)))
                .thenReturn(testInstalledPlugin);

        // Act
        InstalledPlugin result = pluginInstallationService.installPlugin(
                "lorawan-ttn",
                testOrganization,
                testConfiguration
        );

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getPluginKey()).isEqualTo("lorawan-ttn");
        assertThat(result.getStatus()).isEqualTo(PluginInstallationStatus.INACTIVE);

        verify(installedPluginRepository).save(installedPluginCaptor.capture());
        InstalledPlugin saved = installedPluginCaptor.getValue();
        assertThat(saved.getConfiguration()).isEqualTo(testConfiguration);
        assertThat(saved.getInstalledVersion()).isEqualTo("1.0.0");

        verify(pluginRegistryService).incrementInstallationCount("lorawan-ttn");
    }

    @Test
    void shouldThrowExceptionWhenPluginAlreadyInstalled() {
        // Arrange
        when(installedPluginRepository.existsByOrganizationAndPluginKey(testOrganization, "lorawan-ttn"))
                .thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> pluginInstallationService.installPlugin(
                "lorawan-ttn",
                testOrganization,
                testConfiguration
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Plugin already installed");
    }

    @Test
    void shouldThrowExceptionWhenPluginNotFoundInRegistry() {
        // Arrange
        when(installedPluginRepository.existsByOrganizationAndPluginKey(testOrganization, "non-existent"))
                .thenReturn(false);
        when(pluginRegistryRepository.findByPluginKey("non-existent"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> pluginInstallationService.installPlugin(
                "non-existent",
                testOrganization,
                testConfiguration
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Plugin not found in registry");
    }

    @Test
    void shouldActivatePlugin() {
        // Arrange
        when(installedPluginRepository.findByOrganizationAndPluginKey(testOrganization, "lorawan-ttn"))
                .thenReturn(Optional.of(testInstalledPlugin));
        when(dataPluginRepository.save(any(DataPlugin.class))).thenReturn(testDataPlugin);
        when(installedPluginRepository.save(any(InstalledPlugin.class))).thenReturn(testInstalledPlugin);

        // Act
        InstalledPlugin result = pluginInstallationService.activatePlugin(
                "lorawan-ttn",
                testOrganization
        );

        // Assert
        verify(dataPluginRepository).save(any(DataPlugin.class));
        verify(installedPluginRepository).save(installedPluginCaptor.capture());

        InstalledPlugin saved = installedPluginCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo(PluginInstallationStatus.ACTIVE);
        assertThat(saved.getActivatedAt()).isNotNull();
        assertThat(saved.getLastUsedAt()).isNotNull();
    }

    @Test
    void shouldNotActivateAlreadyActivePlugin() {
        // Arrange
        testInstalledPlugin.setStatus(PluginInstallationStatus.ACTIVE);
        when(installedPluginRepository.findByOrganizationAndPluginKey(testOrganization, "lorawan-ttn"))
                .thenReturn(Optional.of(testInstalledPlugin));

        // Act
        InstalledPlugin result = pluginInstallationService.activatePlugin(
                "lorawan-ttn",
                testOrganization
        );

        // Assert
        assertThat(result.getStatus()).isEqualTo(PluginInstallationStatus.ACTIVE);
        verify(dataPluginRepository, never()).save(any());
        verify(installedPluginRepository, never()).save(any());
    }

    @Test
    void shouldCreateDataPluginWhenActivating() {
        // Arrange
        when(installedPluginRepository.findByOrganizationAndPluginKey(testOrganization, "lorawan-ttn"))
                .thenReturn(Optional.of(testInstalledPlugin));
        when(dataPluginRepository.save(any(DataPlugin.class))).thenReturn(testDataPlugin);
        when(installedPluginRepository.save(any(InstalledPlugin.class))).thenReturn(testInstalledPlugin);

        // Act
        pluginInstallationService.activatePlugin("lorawan-ttn", testOrganization);

        // Assert
        verify(dataPluginRepository).save(dataPluginCaptor.capture());
        DataPlugin saved = dataPluginCaptor.getValue();
        assertThat(saved.getName()).isEqualTo("LoRaWAN TTN Integration");
        assertThat(saved.getPluginType()).isEqualTo(PluginType.PROTOCOL_PARSER);
        assertThat(saved.getEnabled()).isTrue();
        assertThat(saved.getOrganization()).isEqualTo(testOrganization);
    }

    @Test
    void shouldDeactivatePlugin() {
        // Arrange
        testInstalledPlugin.setStatus(PluginInstallationStatus.ACTIVE);
        testInstalledPlugin.setDataPlugin(testDataPlugin);

        when(installedPluginRepository.findByOrganizationAndPluginKey(testOrganization, "lorawan-ttn"))
                .thenReturn(Optional.of(testInstalledPlugin));
        when(dataPluginRepository.save(any(DataPlugin.class))).thenReturn(testDataPlugin);
        when(installedPluginRepository.save(any(InstalledPlugin.class))).thenReturn(testInstalledPlugin);

        // Act
        InstalledPlugin result = pluginInstallationService.deactivatePlugin(
                "lorawan-ttn",
                testOrganization
        );

        // Assert
        verify(dataPluginRepository).save(dataPluginCaptor.capture());
        DataPlugin savedDataPlugin = dataPluginCaptor.getValue();
        assertThat(savedDataPlugin.getEnabled()).isFalse();

        verify(installedPluginRepository).save(installedPluginCaptor.capture());
        InstalledPlugin savedInstalled = installedPluginCaptor.getValue();
        assertThat(savedInstalled.getStatus()).isEqualTo(PluginInstallationStatus.INACTIVE);
    }

    @Test
    void shouldNotDeactivateAlreadyInactivePlugin() {
        // Arrange
        testInstalledPlugin.setStatus(PluginInstallationStatus.INACTIVE);
        when(installedPluginRepository.findByOrganizationAndPluginKey(testOrganization, "lorawan-ttn"))
                .thenReturn(Optional.of(testInstalledPlugin));

        // Act
        InstalledPlugin result = pluginInstallationService.deactivatePlugin(
                "lorawan-ttn",
                testOrganization
        );

        // Assert
        assertThat(result.getStatus()).isEqualTo(PluginInstallationStatus.INACTIVE);
        verify(dataPluginRepository, never()).save(any());
        verify(installedPluginRepository, never()).save(any());
    }

    @Test
    void shouldUninstallPlugin() {
        // Arrange
        testInstalledPlugin.setDataPlugin(testDataPlugin);
        when(installedPluginRepository.findByOrganizationAndPluginKey(testOrganization, "lorawan-ttn"))
                .thenReturn(Optional.of(testInstalledPlugin));

        // Act
        pluginInstallationService.uninstallPlugin("lorawan-ttn", testOrganization);

        // Assert
        verify(dataPluginRepository).delete(testDataPlugin);
        verify(installedPluginRepository).delete(testInstalledPlugin);
        verify(pluginRegistryService).incrementInstallationCount("lorawan-ttn");
    }

    @Test
    void shouldUninstallPluginWithoutDataPlugin() {
        // Arrange
        testInstalledPlugin.setDataPlugin(null);
        when(installedPluginRepository.findByOrganizationAndPluginKey(testOrganization, "lorawan-ttn"))
                .thenReturn(Optional.of(testInstalledPlugin));

        // Act
        pluginInstallationService.uninstallPlugin("lorawan-ttn", testOrganization);

        // Assert
        verify(dataPluginRepository, never()).delete(any());
        verify(installedPluginRepository).delete(testInstalledPlugin);
    }

    @Test
    void shouldUpdatePluginConfiguration() {
        // Arrange
        testInstalledPlugin.setDataPlugin(testDataPlugin);
        when(installedPluginRepository.findByOrganizationAndPluginKey(testOrganization, "lorawan-ttn"))
                .thenReturn(Optional.of(testInstalledPlugin));
        when(dataPluginRepository.save(any(DataPlugin.class))).thenReturn(testDataPlugin);
        when(installedPluginRepository.save(any(InstalledPlugin.class))).thenReturn(testInstalledPlugin);

        // Act
        InstalledPlugin result = pluginInstallationService.updatePluginConfiguration(
                "lorawan-ttn",
                testOrganization,
                testConfiguration
        );

        // Assert
        verify(installedPluginRepository).save(installedPluginCaptor.capture());
        InstalledPlugin savedInstalled = installedPluginCaptor.getValue();
        assertThat(savedInstalled.getConfiguration()).isEqualTo(testConfiguration);

        verify(dataPluginRepository).save(dataPluginCaptor.capture());
        DataPlugin savedDataPlugin = dataPluginCaptor.getValue();
        assertThat(savedDataPlugin.getConfiguration()).isEqualTo(testConfiguration);
    }

    @Test
    void shouldUpdateConfigurationWithoutDataPlugin() {
        // Arrange
        testInstalledPlugin.setDataPlugin(null);
        when(installedPluginRepository.findByOrganizationAndPluginKey(testOrganization, "lorawan-ttn"))
                .thenReturn(Optional.of(testInstalledPlugin));
        when(installedPluginRepository.save(any(InstalledPlugin.class))).thenReturn(testInstalledPlugin);

        // Act
        InstalledPlugin result = pluginInstallationService.updatePluginConfiguration(
                "lorawan-ttn",
                testOrganization,
                testConfiguration
        );

        // Assert
        verify(installedPluginRepository).save(any(InstalledPlugin.class));
        verify(dataPluginRepository, never()).save(any());
    }

    @Test
    void shouldGetInstalledPlugins() {
        // Arrange
        List<InstalledPlugin> plugins = Arrays.asList(testInstalledPlugin);
        when(installedPluginRepository.findByOrganization(testOrganization)).thenReturn(plugins);

        // Act
        List<InstalledPlugin> result = pluginInstallationService.getInstalledPlugins(testOrganization);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPluginKey()).isEqualTo("lorawan-ttn");
        verify(installedPluginRepository).findByOrganization(testOrganization);
    }

    @Test
    void shouldGetActivePlugins() {
        // Arrange
        testInstalledPlugin.setStatus(PluginInstallationStatus.ACTIVE);
        List<InstalledPlugin> plugins = Arrays.asList(testInstalledPlugin);
        when(installedPluginRepository.findActivePluginsByOrganization(testOrganization))
                .thenReturn(plugins);

        // Act
        List<InstalledPlugin> result = pluginInstallationService.getActivePlugins(testOrganization);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(PluginInstallationStatus.ACTIVE);
        verify(installedPluginRepository).findActivePluginsByOrganization(testOrganization);
    }

    @Test
    void shouldGetInstalledPlugin() {
        // Arrange
        when(installedPluginRepository.findByOrganizationAndPluginKey(testOrganization, "lorawan-ttn"))
                .thenReturn(Optional.of(testInstalledPlugin));

        // Act
        Optional<InstalledPlugin> result = pluginInstallationService.getInstalledPlugin(
                "lorawan-ttn",
                testOrganization
        );

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getPluginKey()).isEqualTo("lorawan-ttn");
    }

    @Test
    void shouldCheckIfPluginIsInstalled() {
        // Arrange
        when(installedPluginRepository.existsByOrganizationAndPluginKey(testOrganization, "lorawan-ttn"))
                .thenReturn(true);
        when(installedPluginRepository.existsByOrganizationAndPluginKey(testOrganization, "not-installed"))
                .thenReturn(false);

        // Act & Assert
        assertThat(pluginInstallationService.isPluginInstalled("lorawan-ttn", testOrganization))
                .isTrue();
        assertThat(pluginInstallationService.isPluginInstalled("not-installed", testOrganization))
                .isFalse();
    }

    @Test
    void shouldThrowExceptionWhenActivatingNonInstalledPlugin() {
        // Arrange
        when(installedPluginRepository.findByOrganizationAndPluginKey(testOrganization, "non-existent"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> pluginInstallationService.activatePlugin(
                "non-existent",
                testOrganization
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Plugin not installed");
    }

    @Test
    void shouldThrowExceptionWhenDeactivatingNonInstalledPlugin() {
        // Arrange
        when(installedPluginRepository.findByOrganizationAndPluginKey(testOrganization, "non-existent"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> pluginInstallationService.deactivatePlugin(
                "non-existent",
                testOrganization
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Plugin not installed");
    }

    @Test
    void shouldThrowExceptionWhenUninstallingNonInstalledPlugin() {
        // Arrange
        when(installedPluginRepository.findByOrganizationAndPluginKey(testOrganization, "non-existent"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> pluginInstallationService.uninstallPlugin(
                "non-existent",
                testOrganization
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Plugin not installed");
    }
}
