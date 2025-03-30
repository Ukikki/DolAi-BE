package com.dolai.backend.oauth.repository;


import com.dolai.backend.oauth.jwt.Token;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TokenRepository extends CrudRepository<Token,String> {
}