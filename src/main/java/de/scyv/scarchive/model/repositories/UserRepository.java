package de.scyv.scarchive.model.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import de.scyv.scarchive.model.entities.User;

/**
 * Repository for accessing users from the underlying database.
 */
public interface UserRepository extends JpaRepository<User, Long> {
    // this interface is brought to life by spring boot

    User findByName(String name);
}
