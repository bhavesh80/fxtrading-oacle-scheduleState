package com.trading.flows

import com.trade.AbstractFlowConfiguration
import com.trading.states.TradeState
import net.corda.core.node.services.Vault.StateStatus
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.testing.core.singleIdentity
import org.junit.Test
import kotlin.test.assertEquals
class TwoPartyFlowTests : AbstractFlowConfiguration() {

    @Test
    fun `accept trade state between 2 parties`() {

        val counterPartyName = counterParty.name.organisation
        println("CounterParty $counterPartyName")

        val twoPartyFlow = TwoPartyTradeFlow(100.0, "USD", "INR",counterParty)


        initiatorNode.startFlow(twoPartyFlow).toCompletableFuture()

        mockNetwork.waitQuiescent()

        val queryCriteria = QueryCriteria.VaultQueryCriteria().withStatus(StateStatus.UNCONSUMED)

        var stateA =
            initiatorNode.services.vaultService.queryBy(
                TradeState::class.java,
                queryCriteria
            ).states.first { it.state.data.tradeStatus == "OrderPlaced" }.state.data
        var stateB =
            counterPartyNode.services.vaultService.queryBy(
                TradeState::class.java,
                queryCriteria
            ).states.first { it.state.data.tradeStatus == "OrderPlaced" }.state.data

        assertEquals(stateA, stateB, "Same state should be available in both nodes")

        val tradeDecision = TradeDecision(
            stateB.linearId.toString(),
            "Approved"
        )

        counterPartyNode.startFlow(tradeDecision)

        mockNetwork.waitQuiescent()

        val stateASuccess =
            initiatorNode.services.vaultService.queryBy(
                TradeState::class.java,
                queryCriteria
            ).states.first { it.state.data.tradeStatus == "Approved" }.state.data
        val stateBSuccess =
            counterPartyNode.services.vaultService.queryBy(
                TradeState::class.java,
                queryCriteria
            ).states.first { it.state.data.tradeStatus == "Approved" }.state.data

        assertEquals(stateASuccess, stateBSuccess, "Same state should be available in both nodes")

        assertEquals( stateASuccess.linearId,stateBSuccess.linearId)
    }
}