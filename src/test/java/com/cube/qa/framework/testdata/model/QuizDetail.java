package com.cube.qa.framework.testdata.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Per-quiz body fetched from {@code <cdn>/v3/quizzes/{id}.json}.
 *
 * <p>A quiz is an ordered list of {@link QuizQuestion}s. Questions share a
 * common {@code type} discriminator (e.g. {@code textSelectionQuestion},
 * {@code imageSelectionQuestion}) and a {@code uiType} sub-discriminator
 * within {@code data} (e.g. {@code DEFAULT}, {@code FILL_IN_THE_BLANK}).
 * Tests targeting one variant filter on both.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuizDetail {

    public String id;
    public Map<String, String> title = Collections.emptyMap();
    public Map<String, String> failureMessage = Collections.emptyMap();
    public String badgeId;
    public List<QuizQuestion> questions = Collections.emptyList();

    public String titleEn() {
        return title == null ? null : title.get("en-US");
    }

    public String failureMessageEn() {
        return failureMessage == null ? null : failureMessage.get("en-US");
    }
}
