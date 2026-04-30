package com.cube.qa.framework.testdata.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Collections;
import java.util.Map;

/**
 * Top-level quiz manifest entry — one row in
 * {@code <cdn>/v3/quiz-manifest.json}, keyed by quiz id.
 *
 * <p>The manifest only carries the title and badge id; the question list lives
 * in {@code <cdn>/v3/quizzes/{id}.json}, modeled by {@link QuizDetail}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Quiz {

    public String id;
    public Map<String, String> title = Collections.emptyMap();
    public String badgeId;
    public long timestamp;

    public String titleEn() {
        return title == null ? null : title.get("en-US");
    }
}
