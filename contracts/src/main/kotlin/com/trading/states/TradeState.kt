package com.trading.states

import com.trading.contracts.TradeContract
import net.corda.core.contracts.*
import net.corda.core.flows.FlowLogicRefFactory
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import java.time.Instant

@BelongsToContract(TradeContract::class)
data class TradeState(
    val sellAmount: Double,
    val receivedAmount: Double,
    val sellCurrency: String,
    val buyCurrency: String,
    val tradeStatus: String,
    val requester: Party,
    val responder : Party? = null,
    val currencyRate: Double,
    var updateTrade: Instant = if (responder == null) Instant.now().plusSeconds(20) else Instant.now().plusSeconds(60) ,
    override val linearId: UniqueIdentifier = UniqueIdentifier()) :  LinearState ,SchedulableState {
    override val participants: List<AbstractParty> = if(responder == null) listOf(requester) else listOf(requester,responder)

    override fun nextScheduledActivity(thisStateRef: StateRef,flowLogicRefFactory: FlowLogicRefFactory): ScheduledActivity? {
        if (tradeStatus == "Completed" && responder == null){
            println("Condition 1st")
            return null
//        }else if(tradeStatus == "Completed" && responder != null){
//            println("Condition 2nd")
//            return null
        } else if(tradeStatus == "Pending" && responder == null){
            println("Condition 3rd")
            return ScheduledActivity(flowLogicRefFactory.create("com.trading.flows.CurrencyExchangeFlow", thisStateRef), updateTrade)
        }else if(tradeStatus=="OrderPlaced" && responder != null ){
            println("Condition 4th")
            return ScheduledActivity(flowLogicRefFactory.create("com.trading.flows.CurrencyTradeFlow", thisStateRef), updateTrade)
        }
        return null
    }

}

