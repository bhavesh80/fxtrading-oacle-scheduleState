package com.trading.flows

import co.paralleluniverse.fibers.Suspendable
import com.trading.services.ExchangeRatesOracle
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.utilities.unwrap


@InitiatedBy(QueryRates::class)
class QueryHandler(val session: FlowSession) : FlowLogic<Unit>() {

    @Suspendable
    override fun call() {
        println("Receiving query request")
        val request = session.receive<String>().unwrap { it }

        println("Calculating Currency Rate for $request")
        val response = try {
            serviceHub.cordaService(ExchangeRatesOracle::class.java).query(request)
        } catch (e: Exception) {
            throw FlowException(e)
        }
        println("Sending query response")
        session.send(response)
    }
}