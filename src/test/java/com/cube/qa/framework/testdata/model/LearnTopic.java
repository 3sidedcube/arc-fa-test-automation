package com.cube.qa.framework.testdata.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A learn topic (e.g. a Learn tab tile) from
 * {@code <cdn>/v3/learn-topic-manifest.json}.
 *
 * The {@code id} is assigned by the loader from the outer map key.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LearnTopic {

    /** Content ID — set by the loader, not present in the per-entry JSON. */
    public String id;

    public long timestamp;

    /** Localized title, e.g. {@code {"en-US": "CPR"}}. */
    public Map<String, String> title = Collections.emptyMap();

    public String iconId;

    public boolean isFeatured;

    public String analyticsPageName;

    public List<String> lessonIds = Collections.emptyList();

    public List<String> personalizationTagIds = Collections.emptyList();

    /** Convenience: English title, or null if not localized for en-US. */
    public String titleEn() {
        return title == null ? null : title.get("en-US");
    }
}
