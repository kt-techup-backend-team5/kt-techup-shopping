package com.kt.integration.openai.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OpenAIResponse {
	public record VectorCreate(
			String id,
			String object,
			@JsonProperty("created_at")
			Long createdAt,
			String name,
			String description,
			Long bytes,
			@JsonProperty("file_counts")
			FileCounts fileCounts
	) {

	}

	public record FileCounts(
			@JsonProperty("in_progress")
			int inProgress,
			int completed,
			int failed,
			int cancelled,
			int total
	) {
	}

	public record Upload(
			String id,
			String object,
			Long bytes,
			@JsonProperty("created_at")
			Long createdAt,
			@JsonProperty("expires_at")
			Long expiresAt,
			String filename,
			String purpose
	) {
	}

	public record Search(
			String object,
			@JsonProperty("search_query")
			List<String> searchQuery,
			List<SearchData> data,
			@JsonProperty("has_more")
			Boolean hasMore,
			@JsonProperty("next_page")
			Object nextPage
	) {
	}

	public record SearchData(
			@JsonProperty("file_id")
			String fileId,
			String filename,
			Double score,
			Attribute attributes,
			List<Content> content
	) {
	}

	public record Content(
			String type,
			String text
	) {
	}

	public record Attribute(
			String author,
			String date
	) {
	}
}
