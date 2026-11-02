package com.cloudscope.moneyprinter

import android.content.Context
import android.content.SharedPreferences
import com.cloudscope.moneyprinter.data.local.SecurePreferences
import com.cloudscope.moneyprinter.data.model.BrokerCredentials
import com.cloudscope.moneyprinter.data.model.BrokerType
import com.cloudscope.moneyprinter.data.model.ConnectionStatus
import com.cloudscope.moneyprinter.data.repository.MarketDataRepositoryImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MarketDataRepositoryTest {

    // Simple test double for SecurePreferences in unit test environment
    private class FakeSecurePreferences : SecurePreferences(DummyContext()) {
        private var key = ""
        private var token = ""
        private var broker = BrokerType.NONE_STANDALONE
        private var client = ""
        private var darkTheme = true

        override fun saveCredentials(
            apiKey: String,
            accessToken: String,
            brokerType: BrokerType,
            clientId: String
        ) {
            this.key = apiKey
            this.token = accessToken
            this.broker = brokerType
            this.client = clientId
        }

        override fun saveBrokerType(brokerType: BrokerType) {
            this.broker = brokerType
        }

        override fun getCredentials(): BrokerCredentials {
            val configured = when (broker) {
                BrokerType.NONE_STANDALONE -> false
                BrokerType.GROWW -> true
                BrokerType.ANGEL_ONE -> key.isNotBlank() || token.isNotBlank()
                BrokerType.ZERODHA -> key.isNotBlank() && token.isNotBlank()
            }
            return BrokerCredentials(
                brokerType = broker,
                apiKey = key,
                accessToken = token,
                clientId = client,
                isConfigured = configured
            )
        }

        override fun clearCredentials() {
            key = ""
            token = ""
            client = ""
            broker = BrokerType.NONE_STANDALONE
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
    fun testDefaultState_IsAutonomousAndLive() {
        val state = repository.connectionState.value
        assertEquals(ConnectionStatus.LIVE, state.status)
        assertEquals(BrokerType.NONE_STANDALONE, state.brokerType)
        assertFalse(repository.getCredentials().isConfigured)
    }

    @Test
    fun testRefreshWithoutCredentials_SucceedsAutonomously() = runBlocking {
        // Demat setup is optional: refreshing without broker credentials must succeed via public feed / fallback
        val result = repository.refreshMarketData()
        assertTrue("Refresh without broker credentials should succeed", result.isSuccess)
        assertEquals(ConnectionStatus.LIVE, repository.connectionState.value.status)
        assertNotNull(repository.latestQuote.value)
        assertNotNull(repository.latestOptionChain.value)
    }

    @Test
    fun testSaveAndClearCredentials() {
        repository.saveCredentials("test_key", "test_token", BrokerType.ZERODHA)
        assertTrue(repository.getCredentials().isConfigured)
        assertEquals("test_key", repository.getCredentials().apiKey)
        assertEquals(BrokerType.ZERODHA, repository.getCredentials().brokerType)

        repository.clearCredentials()
        assertFalse(repository.getCredentials().isConfigured)
        assertEquals(BrokerType.NONE_STANDALONE, repository.connectionState.value.brokerType)
        assertEquals(ConnectionStatus.LIVE, repository.connectionState.value.status)
    }

    @Test
    fun testAngelOneAndGrowwBrokerSelection() {
        repository.saveBrokerType(BrokerType.GROWW)
        assertEquals(BrokerType.GROWW, repository.getCredentials().brokerType)

        repository.saveCredentials("smart_key_123", "jwt_token_456", BrokerType.ANGEL_ONE, "CL1234")
        assertEquals(BrokerType.ANGEL_ONE, repository.getCredentials().brokerType)
        assertEquals("smart_key_123", repository.getCredentials().apiKey)
        assertEquals("CL1234", repository.getCredentials().clientId)
        assertTrue(repository.getCredentials().isConfigured)
    }
}
