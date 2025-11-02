package com.example.pocket.orm;

import java.util.List;
import java.util.Optional;

/**
 * EntityManager interface for managing entities dynamically based on JSON ORM mappings.
 */
public interface EntityManager {

    /**
     * Saves an entity to the database. If the entity already exists, it updates the record.
     *
     * @param entity The entity to save.
     * @return The saved entity.
     */
    <T> T save(T entity);

    /**
     * Finds an entity by its primary key.
     *
     * @param entityClass The class of the entity.
     * @param primaryKey The primary key value.
     * @return An Optional containing the entity if found, or empty if not found.
     */
    <T> Optional<T> findById(Class<T> entityClass, Object primaryKey);

    /**
     * Finds all entities of a given type.
     *
     * @param entityClass The class of the entity.
     * @return A list of all entities of the given type.
     */
    <T> List<T> findAll(Class<T> entityClass);

    /**
     * Deletes an entity by its primary key.
     *
     * @param entityClass The class of the entity.
     * @param primaryKey The primary key value.
     */
    <T> void deleteById(Class<T> entityClass, Object primaryKey);

    /**
     * Deletes all entities of a given type.
     *
     * @param entityClass The class of the entity.
     */
    <T> void deleteAll(Class<T> entityClass);

    /**
     * Reloads the ORM mapping configuration dynamically at runtime.
     *
     * @param ormMappingPath The path to the JSON ORM mapping file.
     */
    void reloadConfiguration(String ormMappingPath);
}
