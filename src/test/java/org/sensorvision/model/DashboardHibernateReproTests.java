package org.sensorvision.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import jakarta.persistence.TypedQuery;
import org.hibernate.HibernateException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.Map;

@Testcontainers
class DashboardHibernateReproTests {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private static EntityManagerFactory entityManagerFactory;

    private static EntityManagerFactory buildEntityManagerFactory() {
        if (entityManagerFactory == null) {
            Map<String, Object> props = new HashMap<>();
            props.put("jakarta.persistence.jdbc.url", POSTGRES.getJdbcUrl());
            props.put("jakarta.persistence.jdbc.user", POSTGRES.getUsername());
            props.put("jakarta.persistence.jdbc.password", POSTGRES.getPassword());
            props.put("hibernate.hbm2ddl.auto", "create-drop");
            props.put("hibernate.show_sql", "false");
            entityManagerFactory = Persistence.createEntityManagerFactory("dashboard-test", props);
        }
        return entityManagerFactory;
    }

    @AfterAll
    static void tearDown() {
        if (entityManagerFactory != null) {
            entityManagerFactory.close();
        }
    }

    @Test
    void fetchJoinDoesNotTriggerOrphanRemovalOnFlush() {
        EntityManagerFactory emf = buildEntityManagerFactory();
        EntityManager em = emf.createEntityManager();
        ObjectMapper mapper = new ObjectMapper();

        EntityTransaction tx = em.getTransaction();
        tx.begin();

        Organization org = Organization.builder().name("Acme Org").build();
        em.persist(org);

        Dashboard dashboard = new Dashboard();
        dashboard.setName("Default Dashboard");
        dashboard.setIsDefault(true);
        dashboard.setOrganization(org);
        dashboard.setLayoutConfig(mapper.createObjectNode());
        em.persist(dashboard);

        Widget widget = new Widget();
        widget.setDashboard(dashboard);
        widget.setName("Widget");
        widget.setType(WidgetType.METRIC_CARD);
        widget.setPositionX(0);
        widget.setPositionY(0);
        widget.setWidth(4);
        widget.setHeight(3);
        widget.setConfig(mapper.createObjectNode());
        em.persist(widget);

        tx.commit();
        em.clear();

        tx = em.getTransaction();
        tx.begin();

        TypedQuery<Dashboard> query = em.createQuery(
            "SELECT d FROM Dashboard d LEFT JOIN FETCH d.widgets WHERE d.organization = :org AND d.isDefault = true",
            Dashboard.class
        );
        query.setParameter("org", org);
        Dashboard loaded = query.getSingleResult();

        loaded.getWidgets().size();

        try {
            tx.commit();
        } catch (HibernateException ex) {
            Assertions.fail("Hibernate threw exception during commit", ex);
        }

        em.close();
    }
}
