package com.trading.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.identity.Party
import net.corda.core.utilities.unwrap


@InitiatingFlow
class QueryRates(val oracle: Party, val currencyPair: String) : FlowLogic<Double>() {
    @Suspendable override fun call(): Double = initiateFlow(oracle).sendAndReceive<Double>(currencyPair).unwrap { it }
}