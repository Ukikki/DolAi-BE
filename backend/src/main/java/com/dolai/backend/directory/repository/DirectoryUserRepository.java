package com.dolai.backend.directory.repository;

import com.dolai.backend.directory.model.Directory;
import com.dolai.backend.directory.model.DirectoryUser;
import com.dolai.backend.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DirectoryUserRepository extends JpaRepository<DirectoryUser, Long> {
    List<DirectoryUser> findByUserAndDirectoryParentIsNull(User user);
    List<DirectoryUser> findByUserAndDirectory_Parent(User user, Directory parent);
    boolean existsByUserIdAndDirectoryParentAndName(String userId, Directory parent, String name);
    Optional<DirectoryUser> findByDirectoryIdAndUserId(Long directoryId, String userId);
    boolean existsByUserAndName(User user, String name);
}
