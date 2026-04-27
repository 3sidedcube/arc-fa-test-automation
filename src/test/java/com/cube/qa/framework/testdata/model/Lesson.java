package com.cube.qa.framework.testdata.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Collections;
import java.util.Map;

/**
 * A lesson nested inside a {@link LearnTopicDetail#lessons} list. The CDN
 * carries the full lesson card content here; for the Topic Detail tests we
 * only care about the headline fields. Lesson-card rendering will be
 * exercised by future Lessons tests.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Lesson {
    public String id;
    public Map<String, String> title = Collections.emptyMap();
    public String imageId;
    public String analyticsPageName;
    public int durationInMinutes;

    public String titleEn() {
        return title == null ? null : title.get("en-US");
    }
}
