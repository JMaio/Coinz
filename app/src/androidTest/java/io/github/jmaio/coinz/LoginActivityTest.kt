package io.github.jmaio.coinz


import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import androidx.test.runner.AndroidJUnit4
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class LoginActivityTest {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(LoginActivity::class.java)

    @Rule
    @JvmField
    var mGrantPermissionRule =
            GrantPermissionRule.grant(
                    "android.permission.ACCESS_FINE_LOCATION")

    @Test
    fun loginActivityTest() {
        if (mActivityTestRule.activity.user == null) {
            Thread.sleep(4000)

            val appCompatAutoCompleteTextView = onView(
                    allOf(withId(R.id.email_input),
                            childAtPosition(
                                    childAtPosition(
                                            withId(R.id.email_input_field),
                                            0),
                                    0)))
            appCompatAutoCompleteTextView.perform(scrollTo(), replaceText("user@email.com"), closeSoftKeyboard())

            Thread.sleep(500)

            val appCompatEditText = onView(
                    allOf(withId(R.id.password_input),
                            childAtPosition(
                                    childAtPosition(
                                            withId(R.id.password_input_field),
                                            0),
                                    0)))
            appCompatEditText.perform(scrollTo(), replaceText("123456"), closeSoftKeyboard())

            Thread.sleep(500)

            val materialButton = onView(
                    allOf(withId(R.id.sign_in_button), withText("Sign in"),
                            childAtPosition(
                                    childAtPosition(
                                            withId(R.id.login_form),
                                            4),
                                    1)))
            materialButton.perform(scrollTo(), click())

            Thread.sleep(500)
        }

    }

    private fun childAtPosition(
            parentMatcher: Matcher<View>, position: Int): Matcher<View> {

        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("Child at position $position in parent ")
                parentMatcher.describeTo(description)
            }

            public override fun matchesSafely(view: View): Boolean {
                val parent = view.parent
                return parent is ViewGroup && parentMatcher.matches(parent)
                        && view == parent.getChildAt(position)
            }
        }
    }

}
