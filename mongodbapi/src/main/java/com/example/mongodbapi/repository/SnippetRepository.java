package com.example.mongodbapi.repository;

import com.example.mongodbapi.model.Snippet;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SnippetRepository extends MongoRepository<Snippet, String> {
	@Query
	List<Snippet> findByLanguage(String javascript);
}