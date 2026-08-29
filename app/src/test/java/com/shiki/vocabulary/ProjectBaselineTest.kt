package com.shiki.vocabulary

import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectBaselineTest {
    @Test
    fun applicationIdentityIsStable() {
        assertEquals("com.page67.vocabularybuilder.clean", BuildConfig.APPLICATION_ID)
        assertEquals("1.0.0", BuildConfig.VERSION_NAME)
    }
}
