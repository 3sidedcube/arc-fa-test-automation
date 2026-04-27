package com.cube.qa.framework.testdata.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Per-topic detail JSON fetched from {@code <cdn>/v3/learn-topics/{id}.json}.
 * Richer than {@link LearnTopic} (which only holds top-level manifest fields):
 * carries the full lesson list, related-article references, and the FAQ list
 * the Topic Detail tests assert against.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LearnTopicDetail {

    public String id;
    public Map<String, String> title = Collections.emptyMap();
    public Map<String, String> description = Collections.emptyMap();
    public String iconId;
    public String imageId;
    public List<Lesson> lessons = Collections.emptyList();
    public List<String> relatedArticleIds = Collections.emptyList();
    public List<Faq> faqs = Collections.emptyList();
    public String analyticsPageName;
    public boolean isFeatured;
    public List<String> personalizationTagIds = Collections.emptyList();

    public String titleEn() {
        return title == null ? null : title.get("en-US");
    }

    public String descriptionEn() {
        return description == null ? null : description.get("en-US");
    }
}
