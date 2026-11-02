package com.cloudscope.moneyprinter

import android.content.Context
import android.content.SharedPreferences
import com.cloudscope.moneyprinter.data.local.SecurePreferences
import com.cloudscope.moneyprinter.data.model.ConnectionStatus
import com.cloudscope.moneyprinter.data.repository.MarketDataRepositoryImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class MarketDataRepositoryTest {

    // Simple test double for SecurePreferences in unit test environment
    private class FakeSecurePreferences : SecurePreferences(DummyContext()) {
        private var key = ""
        private var token = ""
        private var darkTheme = true

        override fun saveCredentials(apiKey: String, accessToken: String) {
            this.key = apiKey
            this.token = accessToken
        }

        override fun getCredentials(): com.cloudscope.moneyprinter.data.model.BrokerCredentials {
            return com.cloudscope.moneyprinter.data.model.BrokerCredentials(key, token, key.isNotBlank() && token.isNotBlank())
        }

        override fun clearCredentials() {
            key = ""
            token = ""
        }

        override fun isDarkThemeEnabled(): Boolean = darkTheme
        override fun setDarkThemeEnabled(enabled: Boolean) { darkTheme = enabled }
    }

    private class DummyContext : android.content.ContextWrapper(null)

    private lateinit var fakePrefs: FakeSecurePreferences
    private lateinit var repository: MarketDataRepositoryImpl

    @Before
    fun setUp() {
        fakePrefs = FakeSecurePreferences()
        repository = MarketDataRepositoryImpl(fakePrefs)
    }

    @Test
    fun testUnconfiguredState_RequiresSetup() {
        val state = repository.connectionState.value
        assertEquals(ConnectionStatus.NEEDS_SETUP, state.status)
        assertFalse(repository.getCredentials().isConfigured)
    }

    @Test
    fun testRefreshWithoutCredentials_FailsGracefully() = runBlocking {
        val result = repository.refreshMarketData()
        assertTrue("Refresh without credentials must fail", result.isFailure)
        assertEquals(ConnectionStatus.NEEDS_SETUP, repository.connectionState.value.status)
    }

    @Test
    fun testSaveAndClearCredentials() {
        repository.saveCredentials("test_key", "test_token")
        assertTrue(repository.getCredentials().isConfigured)
        assertEquals("test_key", repository.getCredentials().apiKey)

        repository.clearCredentials()
        assertFalse(repository.getCredentials().isConfigured)
        assertEquals(ConnectionStatus.NEEDS_SETUP, repository.connectionState.value.status)
    }
}
