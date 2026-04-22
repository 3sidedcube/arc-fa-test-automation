package com.cube.qa.framework.testdata.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * An article entry from {@code <cdn>/v3/article-manifest.json}.
 *
 * The {@code id} is assigned by the loader from the outer map key.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Article {

    /** Content ID — set by the loader, not present in the per-entry JSON. */
    public String id;

    public long timestamp;

    public boolean isMenuItem;

    /** Localized title, e.g. {@code {"en-US": "...", "es-US": "..."}}. */
    public Map<String, String> title = Collections.emptyMap();

    public List<String> searchLanguages = Collections.emptyList();

    public String searchLocation;

    public String type;

    public List<String> tabLocation = Collections.emptyList();

    /** Convenience: English title, or null if not localized for en-US. */
    public String titleEn() {
        return title == null ? null : title.get("en-US");
    }
}
