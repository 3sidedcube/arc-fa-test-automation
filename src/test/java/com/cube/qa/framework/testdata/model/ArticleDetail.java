package com.cube.qa.framework.testdata.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Per-article detail fetched from {@code <cdn>/v3/articles/{id}.json}.
 *
 * <p>Articles are heterogeneous: an emergency article's body is an ordered
 * list of {@link ArticleComponent}s where each component has a {@code type}
 * (e.g. {@code emergencyStep}, {@code imageComponent}) and a free-form
 * {@code data} payload. Tests interested in the numbered step content use
 * {@link #emergencySteps()} which extracts and types the relevant entries.
 *
 * <p>Bullet markers in step descriptions are the {@code »} character
 * (U+00BB), not literal bullets — see {@link EmergencyStep#hasBullet()}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ArticleDetail {

    public String id;
    public Map<String, String> name = Collections.emptyMap();
    public String type;
    public List<ArticleComponent> components = Collections.emptyList();

    public String nameEn() {
        return name == null ? null : name.get("en-US");
    }

    /**
     * Ordered list of {@code emergencyStep} components, parsed into typed
     * {@link EmergencyStep}s. Other component types (images, etc.) are
     * skipped — tests that need them can read {@link #components} directly.
     */
    public List<EmergencyStep> emergencySteps() {
        if (components == null) return Collections.emptyList();
        List<EmergencyStep> out = new ArrayList<>();
        for (ArticleComponent c : components) {
            if (!"emergencyStep".equals(c.type) || c.data == null) continue;
            EmergencyStep step = new EmergencyStep();
            Object num = c.data.get("number");
            if (num instanceof Number) {
                step.number = ((Number) num).intValue();
            }
            step.titleEn = pluckEn(c.data.get("title"));
            step.descriptionEn = pluckEn(c.data.get("description"));
            out.add(step);
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static String pluckEn(Object localized) {
        if (localized instanceof Map) {
            Object v = ((Map<String, Object>) localized).get("en-US");
            return v == null ? null : v.toString();
        }
        return null;
    }
}
