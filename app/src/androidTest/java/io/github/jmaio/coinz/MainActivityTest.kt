package io.github.jmaio.coinz


import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import androidx.test.runner.AndroidJUnit4
import com.google.android.material.internal.NavigationMenu
import com.google.android.material.internal.NavigationMenuItemView
import com.google.android.material.navigation.NavigationView
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(MainActivity::class.java)

    @Rule
    @JvmField
    var mGrantPermissionRule =
            GrantPermissionRule.grant(
                    "android.permission.ACCESS_FINE_LOCATION")

    @Test
    fun mainActivityTest() {
        if (mActivityTestRule.activity.user != null) {

            Thread.sleep(3000)
            onView(withId(R.id.fab)).perform(click())

            Thread.sleep(2000)
            pressBack()

            Thread.sleep(2000)
            onView(withId(R.id.app_bar_leaderboard)).perform(click())

            Thread.sleep(2000)
            pressBack()

            Thread.sleep(2000)
            onView(withContentDescription("Navigate up")).perform(click())

            Thread.sleep(2000)
            pressBack()

            Thread.sleep(2000)
        }
    }
}
