package com.cube.qa.framework.testdata.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A lesson nested inside a {@link LearnTopicDetail#lessons} list. Carries the
 * full deck of {@link LessonCard cards} the Lessons tests step through.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Lesson {
    public String id;
    public Map<String, String> title = Collections.emptyMap();
    public Map<String, String> description = Collections.emptyMap();
    public String imageId;
    public String analyticsPageName;
    public int durationInMinutes;
    public List<LessonCard> cards = Collections.emptyList();

    public String titleEn() {
        return title == null ? null : title.get("en-US");
    }

    /**
     * en-US lesson description shown on the Topic Detail lesson card. Empty
     * string ({@code ""}) and {@code null} both mean "bundle did not provide
     * a description for this lesson" — callers should treat them the same.
     */
    public String descriptionEn() {
        return description == null ? null : description.get("en-US");
    }
}
