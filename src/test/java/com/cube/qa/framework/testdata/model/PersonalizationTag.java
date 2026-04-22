package com.cube.qa.framework.testdata.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Collections;
import java.util.Map;

/**
 * A personalization tag (e.g. "Cardiac Emergencies") surfaced on the
 * personalization screen. Mirrors the entries in
 * {@code <cdn>/v3/personalization-tags.json}.
 *
 * The tag {@code id} is assigned by the loader from the outer array key — the
 * JSON payload itself doesn't repeat the ID inside the object.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PersonalizationTag {

    /** Content ID — set by the loader, not present in the per-entry JSON. */
    public String id;

    /** Localized title, e.g. {@code {"en-US": "Cardiac Emergencies"}}. */
    public Map<String, String> title = Collections.emptyMap();

    public String iconId;

    public int order;

    /** Convenience: English title, or null if not localized for en-US. */
    public String titleEn() {
        return title == null ? null : title.get("en-US");
    }
}
