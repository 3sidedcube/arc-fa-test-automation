package tests;

import com.cube.qa.framework.pages.home.LearnTabPage;
import com.cube.qa.framework.pages.home.PersonalizeExperiencePage;
import com.cube.qa.framework.pages.home.TabPage;
import com.cube.qa.framework.pages.home.TabPage.Tab;
import com.cube.qa.framework.pages.onboarding.TooltipsPage;
import com.cube.qa.framework.testdata.loader.ContentBundleLoader;
import com.cube.qa.framework.testdata.model.PersonalizationTag;
import com.cube.qa.framework.utils.BaseTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Test cases for the Personalize Experience modal opened from the Learn tab.
 * Testiny folder: ARC First Aid - v4.0.0 > Learn Tab (With Microlearning) > Personalize Experience
 */
public class PersonalizeExperienceTest extends BaseTest {

    private TabPage tabPage;
    private LearnTabPage learnTabPage;
    private PersonalizeExperiencePage personalizePage;

    @BeforeMethod(alwaysRun = true, dependsOnMethods = "setUp")
    public void setUpTest() {
        tabPage = pages.tabPage();
        learnTabPage = pages.learnTabPage();
        personalizePage = pages.personalizeExperiencePage();

        // Precondition: land on the Learn tab with the Personalize CTA visible.
        // walkOnboardingAsGuest drops us on the home screen under the onboarding
        // tooltips overlay — dismiss those so the tab bar + Learn content are
        // interactable. Same pattern as TooltipsTest.TC8693.
        walkOnboardingAsGuest();

        TooltipsPage tooltips = pages.tooltipsPage();
        int safety = 0;
        while (safety++ < 8 && tooltips.isGotItButtonPresent()) {
            try { tooltips.tapGotIt(); } catch (RuntimeException e) { break; }
        }

        // walkOnboardingAsGuest should leave us on Learn — make it explicit so
        // the test is robust to future changes in onboarding's landing tab.
        // Use non-throwing presence checks here: isPersonalizeCtaVisible() waits
        // 30s before throwing if the CTA isn't on screen, which would dominate
        // setup time when we just need a quick "are we there yet" check.
        if (!learnTabPage.isPersonalizeCtaPresent() && tabPage.isTabPresent(Tab.LEARN)) {
            tabPage.tapTab(Tab.LEARN);
        }
    }

    /**
     * Opens the Personalize Experience modal and confirms it rendered.
     * Shared precondition for every test in this class except TC31872
     * (which verifies the Learn-tab state BEFORE opening the modal).
     */
    private void openPersonalizeModal() {
        Assert.assertTrue(learnTabPage.isPersonalizeCtaVisible(),
                "Personalize Experience CTA should be visible before opening modal");
        learnTabPage.tapPersonalizeCta();
        Assert.assertTrue(personalizePage.isModalVisible(),
                "Personalize modal should open after tapping the CTA");
    }

    /**
     * Pick the first personalization tag from the CDN bundle (CMS order #1).
     * Keeps tests env-agnostic: as content authors add/rename/reorder tags,
     * the bundle leads and the test follows.
     */
    private String firstTagLabel() {
        return ContentBundleLoader.allTags().stream()
                .min(Comparator.comparingInt(t -> t.order))
                .map(PersonalizationTag::titleEn)
                .orElseThrow(() -> new RuntimeException("No personalization tags in bundle"));
    }

    // ========================================================================
    // TC31872 — Empty state display on Learn tab (before personalization)
    // ========================================================================
    @Test(description = "TC31872 - Validate Empty State Display on Learn Tab", groups = {"regression"})
    public void TC31872() {
        // Precondition: fresh install, user has not personalized yet.
        // walkOnboardingAsGuest + fullReset=true satisfy both.

        Assert.assertTrue(learnTabPage.isEmptyStateTitleVisible(),
                "Empty-state title should be visible on the Learn tab");
        Assert.assertTrue(learnTabPage.isEmptyStateSubtitleVisible(),
                "Empty-state subheading should be visible on the Learn tab");
        Assert.assertTrue(learnTabPage.isPersonalizeCtaVisible(),
                "'Personalize Experience' CTA should be visible on the empty-state card");

        log("✅ TC31872: Learn-tab empty state shows title, subheading, and Personalize CTA");
    }

    // ========================================================================
    // TC31873 — Modal launch + title/subtitle/tags-in-CMS-order/disabled CTA
    // ========================================================================
    @Test(description = "TC31873 - Validate Personalize Experience Modal Launch", groups = {"regression"})
    public void TC31873() {
        openPersonalizeModal();

        Assert.assertTrue(personalizePage.isTitleVisible(),
                "Modal should render a title");
        Assert.assertTrue(personalizePage.isSubtitleVisible(),
                "Modal should render a subtitle");

        Assert.assertTrue(personalizePage.isConfirmCtaVisible(),
                "Primary 'PERSONALIZE EXPERIENCE' CTA should be visible inside the modal");
        Assert.assertTrue(personalizePage.isConfirmCtaDisabled(),
                "Primary CTA should be disabled by default (no topic tag selected)");

        // Topic tags render in CMS order. The CMS order is the personalization_tags
        // bundle sorted by the `order` field — ContentBundleLoader fetches the
        // same bundle the app is rendering from, so any drift between the two
        // surfaces here as an ordering mismatch.
        List<String> expectedTagOrder = ContentBundleLoader.allTags().stream()
                .sorted(Comparator.comparingInt(t -> t.order))
                .map(PersonalizationTag::titleEn)
                .collect(Collectors.toList());

        List<String> actualTagOrder = personalizePage.collectAllTopicTagsInOrder();

        log("📋 Expected tag order (from bundle): " + expectedTagOrder);
        log("📋 Actual tag order (from modal):    " + actualTagOrder);

        Assert.assertEquals(actualTagOrder, expectedTagOrder,
                "Topic tags in the modal should match the CMS order from the personalization_tags bundle");

        log("✅ TC31873: Personalize Experience modal launched with all required fields and CMS-ordered tags");
    }

    // ========================================================================
    // TC31874 — Close (X) dismisses the modal
    // ========================================================================
    @Test(description = "TC31874 - Validate Modal Can Be Closed", groups = {"regression"})
    public void TC31874() {
        openPersonalizeModal();

        personalizePage.tapClose();

        Assert.assertTrue(personalizePage.isModalInvisible(),
                "Modal should no longer be visible after tapping the close (X) icon");
        // Sanity: we should be back on the Learn tab with the CTA reachable again.
        Assert.assertTrue(learnTabPage.isPersonalizeCtaVisible(),
                "Learn-tab Personalize CTA should be reachable again after closing the modal");

        log("✅ TC31874: Modal closed via X icon");
    }

    // ========================================================================
    // TC31875 — CTA inactive without selection
    // ========================================================================
    @Test(description = "TC31875 - Validate CTA Inactive Without Selection", groups = {"regression"})
    public void TC31875() {
        openPersonalizeModal();

        // No tags tapped — CTA should be disabled.
        Assert.assertTrue(personalizePage.isConfirmCtaDisabled(),
                "'PERSONALIZE EXPERIENCE' CTA should be disabled while no tags are selected");

        log("✅ TC31875: CTA remains disabled with zero tags selected");
    }

    // ========================================================================
    // TC31876 — CTA activates after a tag is selected
    // ========================================================================
    @Test(description = "TC31876 - Validate CTA Activation After Tag Selection", groups = {"regression"})
    public void TC31876() {
        openPersonalizeModal();

        Assert.assertTrue(personalizePage.isConfirmCtaDisabled(),
                "CTA should start disabled before any tag is selected");

        String tag = firstTagLabel();
        personalizePage.tapTopicTag(tag);

        Assert.assertTrue(personalizePage.isTopicTagSelected(tag),
                "Tapped tag '" + tag + "' should report as selected");
        Assert.assertTrue(personalizePage.isConfirmCtaEnabled(),
                "CTA should become enabled once at least one tag is selected");

        log("✅ TC31876: CTA activates after selecting '" + tag + "'");
    }

    // ========================================================================
    // TC31877 — Multiple tag selection
    // ========================================================================
    @Test(description = "TC31877 - Validate Multiple Tag Selection", groups = {"regression"})
    public void TC31877() {
        openPersonalizeModal();

        // Pick the first two tags from CMS order — both guaranteed on-screen
        // at the top of the list.
        List<String> picks = ContentBundleLoader.allTags().stream()
                .sorted(Comparator.comparingInt(t -> t.order))
                .limit(2)
                .map(PersonalizationTag::titleEn)
                .collect(Collectors.toList());

        for (String tag : picks) {
            personalizePage.tapTopicTag(tag);
        }

        // Selection state is the most reliable proxy for "tags highlight as
        // selected" — Appium can't assert color/styling, but the UI's highlight
        // is driven off the underlying checked/switch-value that we verify here.
        for (String tag : picks) {
            Assert.assertTrue(personalizePage.isTopicTagSelected(tag),
                    "Tag '" + tag + "' should be marked selected after tapping");
        }

        log("✅ TC31877: Multiple tags selected: " + picks);
    }

    // ========================================================================
    // TC31878 — All tags can be selected
    // ========================================================================
    @Test(description = "TC31878 - Validate All Tag Selection", groups = {"regression"})
    public void TC31878() {
        openPersonalizeModal();

        List<String> allTags = ContentBundleLoader.allTags().stream()
                .sorted(Comparator.comparingInt(t -> t.order))
                .map(PersonalizationTag::titleEn)
                .collect(Collectors.toList());

        for (String tag : allTags) {
            personalizePage.tapTopicTag(tag);
        }

        // Check selection state for every tag. findTagElement scrolls as needed,
        // so this works even for tags off the initial fold.
        for (String tag : allTags) {
            Assert.assertTrue(personalizePage.isTopicTagSelected(tag),
                    "Tag '" + tag + "' should be selected after tapping every tag");
        }

        log("✅ TC31878: All " + allTags.size() + " tags selected: " + allTags);
    }

    // ========================================================================
    // TC31879 — Deselecting tags
    // ========================================================================
    @Test(description = "TC31879 - Validate Deselecting Tags", groups = {"regression"})
    public void TC31879() {
        openPersonalizeModal();

        String tag = firstTagLabel();

        // Select first — precondition for the deselect step.
        personalizePage.tapTopicTag(tag);
        Assert.assertTrue(personalizePage.isTopicTagSelected(tag),
                "Tag '" + tag + "' should be selected before we deselect it");

        // Tap again — should deselect.
        personalizePage.tapTopicTag(tag);
        Assert.assertFalse(personalizePage.isTopicTagSelected(tag),
                "Tag '" + tag + "' should be deselected after tapping it a second time");

        // Bonus: with zero selected tags, the CTA should be disabled again.
        Assert.assertTrue(personalizePage.isConfirmCtaDisabled(),
                "CTA should return to disabled once the only selected tag is deselected");

        log("✅ TC31879: Tag '" + tag + "' toggled selected → deselected");
    }

    // ========================================================================
    // TC31880 — Submitting personalization closes the modal
    // ========================================================================
    @Test(description = "TC31880 - Validate Submit Personalization Flow", groups = {"smoke"})
    public void TC31880() {
        openPersonalizeModal();

        String tag = firstTagLabel();
        personalizePage.tapTopicTag(tag);
        Assert.assertTrue(personalizePage.isConfirmCtaEnabled(),
                "CTA should be enabled before submitting");

        personalizePage.tapConfirmCta();

        Assert.assertTrue(personalizePage.isModalInvisible(),
                "Modal should close after submitting with at least one tag selected");

        log("✅ TC31880: Personalization submitted, modal closed");
    }
}
