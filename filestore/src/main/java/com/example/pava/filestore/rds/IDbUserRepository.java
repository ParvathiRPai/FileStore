package com.example.pava.filestore.rds;

import org.springframework.data.repository.CrudRepository;

public interface IDbUserRepository extends CrudRepository<DbUser, String> {

}