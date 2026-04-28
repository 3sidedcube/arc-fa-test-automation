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
    public String imageId;
    public String analyticsPageName;
    public int durationInMinutes;
    public List<LessonCard> cards = Collections.emptyList();

    public String titleEn() {
        return title == null ? null : title.get("en-US");
    }
}
