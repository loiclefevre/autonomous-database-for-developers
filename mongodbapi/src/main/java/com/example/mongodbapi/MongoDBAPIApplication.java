package com.example.mongodbapi;

import com.example.mongodbapi.model.Snippet;
import com.example.mongodbapi.model.SnippetCountPerLanguage;
import com.example.mongodbapi.repository.SnippetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

@SpringBootApplication
public class MongoDBAPIApplication implements CommandLineRunner {
	private static final Logger LOG = LoggerFactory.getLogger(MongoDBAPIApplication.class);

	@Autowired
	private SnippetRepository snippetRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Override
	public void run(String... args) throws Exception {
		LOG.info("=".repeat(126));
		LOG.warn("Oracle Database API for MongoDB: easy way!");

		Snippet snippet = new Snippet("alert('test');", "javascript");
		Snippet saved = snippetRepository.insert(snippet);
		LOG.info("Saved snippet: {}", saved);

		load1000Snippets();

		for (Snippet s : snippetRepository.findByLanguage("javascript").subList(1, 3)) {
			LOG.info("JavaScript snippet: {}", s);
		}

		LOG.info("=".repeat(126));

		LOG.warn("Doing some JSON data analytics using SQL:");

		final List<SnippetCountPerLanguage> analyticResults = Objects.requireNonNull(jdbcTemplate.query(
				"""
						 					select s.data.language, count(*)
						 					  from snippet s
						 				  group by s.data.language
						 				  order by 2 desc 
						 				  fetch first 3 rows only
						""",
				(rs, rowNum) -> new SnippetCountPerLanguage(rs.getString(1), rs.getLong(2))));

		for (SnippetCountPerLanguage ar : analyticResults) {
			LOG.info("- language {} has {} snippet(s)", ar.language(), ar.count());
		}
	}

	private void load1000Snippets() {
		Random random = new Random(1234);

		final List<Snippet> listOfSnippets = new ArrayList<>();
		for (int i = 0; i < 1000; i++) {
			Snippet snippet;

			switch (random.nextInt(0, 3)) {
				case 0 -> snippet = new Snippet(String.format("alert('test %d');", i), "javascript");
				case 1 -> snippet = new Snippet(String.format("System.out.println(\"test %d\");", i), "java");
				default -> snippet = new Snippet(String.format("print('test %d')", i), "python");
			}

			listOfSnippets.add(snippet);
		}

		snippetRepository.insert(listOfSnippets);
	}

	public static void main(String[] args) {
		SpringApplication.run(MongoDBAPIApplication.class, args);
		LOG.info("=".repeat(126));
	}

}
