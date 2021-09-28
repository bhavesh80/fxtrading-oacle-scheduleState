package com.trading.states

import com.trading.contracts.TradeContract
import net.corda.core.contracts.*
import net.corda.core.flows.FlowLogicRefFactory
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit

@BelongsToContract(TradeContract::class)
data class TradeState(
    val sellAmount: Double,
    val receivedAmount: Double,
    val sellCurrency: String,
    val buyCurrency: String,
    val tradeStatus: String,
    val requester: Party,
    val currencyRate: Double,
    var updateTrade: Instant = Instant.now().plusSeconds(30),
    override val linearId: UniqueIdentifier = UniqueIdentifier()) :  LinearState ,SchedulableState {

    override val participants: List<AbstractParty> = listOf(requester)
    override fun nextScheduledActivity(thisStateRef: StateRef,flowLogicRefFactory: FlowLogicRefFactory): ScheduledActivity? {
        if (tradeStatus == "Completed"){
            return null
        }
        return ScheduledActivity(flowLogicRefFactory.create("com.trading.flows.CurrencyExchangeFlow", thisStateRef), updateTrade)
    }

}

