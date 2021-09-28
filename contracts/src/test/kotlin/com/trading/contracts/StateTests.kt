package com.trading.contracts

import com.trading.states.TradeState
import net.corda.core.identity.Party
import org.junit.Test
import kotlin.test.assertEquals

class StateTests {
    @Test
    fun hasFieldOfCorrectType() {
        // Does the field exist?
        TradeState::class.java.getDeclaredField("sellValue")
        TradeState::class.java.getDeclaredField("buyValue")
        TradeState::class.java.getDeclaredField("sellCurrency")
        TradeState::class.java.getDeclaredField("buyCurrency")
        TradeState::class.java.getDeclaredField("tradeStatus")
        TradeState::class.java.getDeclaredField("tradeInitiator")
        TradeState::class.java.getDeclaredField("tradeReceiver")

        // Is the field of the correct type?
        assertEquals(TradeState::class.java.getDeclaredField("sellValue").type, Int::class.java)
        assertEquals(TradeState::class.java.getDeclaredField("buyValue").type, Int::class.java)
        assertEquals(TradeState::class.java.getDeclaredField("sellCurrency").type, String()::class.java)
        assertEquals(TradeState::class.java.getDeclaredField("buyCurrency").type, String()::class.java)
        assertEquals(TradeState::class.java.getDeclaredField("tradeStatus").type, String()::class.java)
        assertEquals(TradeState::class.java.getDeclaredField("tradeInitiator").type, Party::class.java)
        assertEquals(TradeState::class.java.getDeclaredField("tradeReceiver").type, Party::class.java)
    }
}