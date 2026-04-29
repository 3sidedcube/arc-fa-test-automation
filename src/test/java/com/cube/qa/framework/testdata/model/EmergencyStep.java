package com.cube.qa.framework.testdata.model;

/**
 * One numbered step inside an {@code emergencyArticle}. Decoded by
 * {@link ArticleDetail#emergencySteps()} from the loosely-typed
 * {@link ArticleComponent#data} map.
 *
 * <p>{@link #descriptionEn} is optional in the CDN — many steps have only a
 * title. When present, it may include "bullet" lines marked with {@code »}
 * (U+00BB) rather than a literal bullet glyph; {@link #hasBullet()} surfaces
 * that for the bullet-rendering test.
 */
public class EmergencyStep {

    public int number;
    public String titleEn;
    public String descriptionEn;

    public boolean hasDescription() {
        return descriptionEn != null && !descriptionEn.isBlank();
    }

    /** True if any line in the description starts with {@code »}. */
    public boolean hasBullet() {
        return descriptionEn != null && descriptionEn.contains("\u00BB");
    }
}
