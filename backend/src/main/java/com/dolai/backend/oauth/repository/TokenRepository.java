package com.dolai.backend.oauth.repository;

import com.dolai.backend.user.model.User;
import com.dolai.backend.oauth.jwt.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TokenRepository extends CrudRepository<Token,String> {
    Optional<Token> findByAccessToken(String accessToken);
}