package com.trading.flows

import co.paralleluniverse.fibers.Suspendable
import com.trading.services.ExchangeRatesOracle
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.transactions.FilteredTransaction
import net.corda.core.utilities.unwrap

@InitiatedBy(SignRates::class)
class SignHandler(val session: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        println("Receiving sign request")
        val request = session.receive<FilteredTransaction>().unwrap { it }
        println("Signing filtered transaction")
        val response = try {
            serviceHub.cordaService(ExchangeRatesOracle::class.java).sign(request)
        } catch (e: Exception) {
            throw FlowException(e)
        }
        println("Sending sign response")
        session.send(response)
    }

}