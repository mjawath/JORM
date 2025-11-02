package com.example.pocket.orm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the EntityManager interface.
 */
class EntityManagerTest {

    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        // Mock the EntityManager implementation
        entityManager = Mockito.mock(EntityManager.class);
    }

    @Test
    void testSave() {
        Object entity = new Object();
        when(entityManager.save(entity)).thenReturn(entity);

        Object savedEntity = entityManager.save(entity);

        assertNotNull(savedEntity);
        assertEquals(entity, savedEntity);
        verify(entityManager, times(1)).save(entity);
    }

    @Test
    void testFindById() {
        Class<Object> entityClass = Object.class;
        Object primaryKey = 1;
        Object entity = new Object();
        when(entityManager.findById(entityClass, primaryKey)).thenReturn(Optional.of(entity));

        Optional<Object> foundEntity = entityManager.findById(entityClass, primaryKey);

        assertTrue(foundEntity.isPresent());
        assertEquals(entity, foundEntity.get());
        verify(entityManager, times(1)).findById(entityClass, primaryKey);
    }

    @Test
    void testFindAll() {
        Class<Object> entityClass = Object.class;
        List<Object> entities = List.of(new Object(), new Object());
        when(entityManager.findAll(entityClass)).thenReturn(entities);

        List<Object> foundEntities = entityManager.findAll(entityClass);

        assertNotNull(foundEntities);
        assertEquals(2, foundEntities.size());
        verify(entityManager, times(1)).findAll(entityClass);
    }

    @Test
    void testDeleteById() {
        Class<Object> entityClass = Object.class;
        Object primaryKey = 1;

        doNothing().when(entityManager).deleteById(entityClass, primaryKey);

        entityManager.deleteById(entityClass, primaryKey);

        verify(entityManager, times(1)).deleteById(entityClass, primaryKey);
    }

    @Test
    void testDeleteAll() {
        Class<Object> entityClass = Object.class;

        doNothing().when(entityManager).deleteAll(entityClass);

        entityManager.deleteAll(entityClass);

        verify(entityManager, times(1)).deleteAll(entityClass);
    }

    @Test
    void testReloadConfiguration() {
        String ormMappingPath = "path/to/orm.json";

        doNothing().when(entityManager).reloadConfiguration(ormMappingPath);

        entityManager.reloadConfiguration(ormMappingPath);

        verify(entityManager, times(1)).reloadConfiguration(ormMappingPath);
    }
}
