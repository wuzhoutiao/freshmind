package com.example.freshmind;

import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;

@RunWith(AndroidJUnit4.class)
public class InventorySeededUiTest {

    @Before
    public void setUp() {
        Context context = getTargetContext();
        TestInventoryStorageSeeder.seedThirtyStorageItems(context);
    }

    @After
    public void tearDown() {
        TestInventoryStorageSeeder.clear(getTargetContext());
    }

    @Test
    public void inventoryScreen_shouldShowSeededStorageItems() {
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.navInventory)).perform(click());

            onView(withText(containsString("Egg will expire in 1 day(s)")))
                    .check(matches(isDisplayed()));
            onView(withText(containsString("Rice will expire in 730 day(s)")))
                    .check(matches(isDisplayed()));
        }
    }

    private Context getTargetContext() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }
}
