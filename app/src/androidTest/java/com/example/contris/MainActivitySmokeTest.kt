package com.example.contris

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import okhttp3.mockwebserver.MockResponse
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class MainActivitySmokeTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        TestNetworkModule.server.enqueue(MockResponse().setBody(fixture("countries_page_0.json")))
        TestNetworkModule.server.enqueue(MockResponse().setBody(fixture("countries_page_1.json")))
        hiltRule.inject()
    }

    @Test
    fun syncsFromMockServer_searches_andOpensDetail() {
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodes(hasText("Germany")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("search_field").performTextInput("Deutschland")
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("Canada")).fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithText("Germany").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Federal Republic of Germany").assertIsDisplayed()
        composeRule.onNodeWithTag("favorite_toggle").performClick()
        composeRule.onNodeWithText("Overview").assertIsDisplayed()
    }

    private fun fixture(name: String): String =
        javaClass.classLoader!!.getResourceAsStream("fixtures/$name")!!.bufferedReader().use { it.readText() }
}
