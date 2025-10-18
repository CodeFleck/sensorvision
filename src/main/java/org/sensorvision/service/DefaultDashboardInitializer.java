package org.sensorvision.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.sensorvision.model.Dashboard;
import org.sensorvision.model.Organization;
import org.sensorvision.repository.DashboardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DefaultDashboardInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DefaultDashboardInitializer.class);

    private final DashboardRepository dashboardRepository;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager entityManager;

    public DefaultDashboardInitializer(DashboardRepository dashboardRepository, ObjectMapper objectMapper) {
        this.dashboardRepository = dashboardRepository;
        this.objectMapper = objectMapper;
    }

        @Transactional
    public void ensureDefaultDashboardExists(Organization organization) {
        if (organization == null) {
            return;
        }

        if (dashboardRepository.findByOrganizationAndIsDefaultTrue(organization).isPresent()) {
            return;
        }

        Dashboard dashboard = new Dashboard();
        dashboard.setName("Default Dashboard");
        dashboard.setDescription("Automatically generated default dashboard");
        dashboard.setIsDefault(true);
        dashboard.setLayoutConfig(objectMapper.createObjectNode());
        dashboard.setOrganization(entityManager.getReference(Organization.class, organization.getId()));

        Dashboard saved = dashboardRepository.save(dashboard);
        logger.info("Initialized default dashboard {} for organization {}", saved.getId(), organization.getId());
    }
}
