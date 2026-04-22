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
 * Testiny folder: ARC First Aid - v4.0.0 > Learn > Personalize Experience
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
        if (!learnTabPage.isPersonalizeCtaVisible() && tabPage.isTabVisible(Tab.LEARN)) {
            tabPage.tapTab(Tab.LEARN);
        }
    }

    @Test(description = "TC31873 - Validate Personalize Experience Modal Launch", groups = {"regression"})
    public void TC31873() {
        // Precondition check: CTA is visible on Learn tab
        Assert.assertTrue(learnTabPage.isPersonalizeCtaVisible(),
                "Personalize Experience CTA should be visible on the Learn tab before launching the modal");

        // Step 1: Tap the Personalize Experience CTA
        learnTabPage.tapPersonalizeCta();

        // Modal renders with a title, subtitle, question prompt + "Select all"
        // helper, a list of topic tags, and a primary CTA.
        Assert.assertTrue(personalizePage.isModalVisible(),
                "Personalize Experience modal should open after tapping the CTA");
        Assert.assertTrue(personalizePage.isTitleVisible(),
                "Modal should render a title");
        Assert.assertTrue(personalizePage.isSubtitleVisible(),
                "Modal should render a subtitle");

        // Primary CTA is present but disabled by default — no tag selected yet.
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
}
