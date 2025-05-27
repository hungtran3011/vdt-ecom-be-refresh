package com.hungng3011.vdtecomberefresh.user;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    boolean existsByEmail(String email);
    boolean existsById(Long id);
    UserEntity findByEmail(String email);
    UserEntity findById(long id);
}
