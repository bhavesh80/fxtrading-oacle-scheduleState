package com.trading.flows

import co.paralleluniverse.fibers.Suspendable
import com.sun.istack.NotNull
import com.trading.contracts.TradeContract
import com.trading.states.TradeState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateRef
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.util.function.Predicate

@InitiatingFlow
@SchedulableFlow
class CurrencyTradeFlow(private val stateRef: StateRef) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        println("Trade timeout - Setting status : Cancelled")


        println("Obtaining notary reference")
        val notary = serviceHub.networkMapCache.notaryIdentities.single()
        val oracleName = CordaX500Name("Oracle", "New York", "US")

        println("Obtaining Oracle reference")
        val oracle = serviceHub.networkMapCache.getNodeByLegalName(oracleName)?.legalIdentities?.first()
            ?: throw IllegalArgumentException("Requested oracle $oracleName not found on network.")


        val input = serviceHub.toStateAndRef<TradeState>(stateRef)
        val stateData = input.state.data
        val currencyPair = stateData.sellCurrency +"/"+ stateData.buyCurrency

        val sellAmount = stateData.sellAmount

        println("Querying for Currency Pair - $currencyPair Rates")
        //query oracle for currency rate
        val currencyRates = subFlow(QueryRates(oracle, currencyPair))
        val convertedCurrency = "%.2f".format( sellAmount* currencyRates)

        println("$currencyPair Rate is : $currencyRates")
        println("Given Currency Pair $currencyPair -  Currency Rates - $currencyRates - ConvertCurrency/Receiving Amount $convertedCurrency")

        val responder: Party? = stateData.responder

        val output = TradeState(
            stateData.sellAmount,
            stateData.receivedAmount,
            stateData.sellCurrency,
            stateData.buyCurrency,
            "Cancelled",
            stateData.requester,
            stateData.responder,
            stateData.currencyRate
        )
        println("Setting state as Cancelled")

        println("Collecting keys - oracle , own, responder ")

        val cmdRequiredSigners = listOf(oracle.owningKey, ourIdentity.owningKey , responder!!.owningKey)


        val txCommand = Command(TradeContract.Commands.CreateTradeWithOracle(currencyPair, currencyRates), cmdRequiredSigners)


        val txBuilder = TransactionBuilder(notary)
            .addInputState(input)
            .addOutputState(output, TradeContract.ID)
            .addCommand(txCommand)


        println("Verify if transaction is valid")
        txBuilder.verify(serviceHub)

        println("Sign the transaction")
        val ptx = serviceHub.signInitialTransaction(txBuilder)

        println("preparing data for sending to oracle and signing")
        val ftx = ptx.buildFilteredTransaction(Predicate {
            when (it) {
                is Command<*> -> oracle.owningKey in it.signers && it.value is TradeContract.Commands.CreateTradeWithOracle
                else -> false
            }
        })

        println("Oracle verify data and collect signature")
        val oracleSignature = subFlow(SignRates(oracle, ftx))

        val selfAndOracleSignedTransaction = ptx.withAdditionalSignature(oracleSignature)

//        send transaction to other party and receive it back with their signature
        val otherPartySession = initiateFlow(responder)
        val signedTransaction = subFlow(CollectSignaturesFlow(selfAndOracleSignedTransaction, setOf(otherPartySession),  listOf(ourIdentity.owningKey)))

        println("Transaction complete")
        return subFlow(FinalityFlow(signedTransaction, (otherPartySession)))

    }
}

@InitiatedBy(CurrencyTradeFlow::class)
class CurrencyTradeFlowResponder(private val counterpartySession: FlowSession) : FlowLogic<SignedTransaction?>() {
    @Suspendable
    @Throws(FlowException::class)
    override fun call(): SignedTransaction? {
        subFlow(object : SignTransactionFlow(counterpartySession) {
            @Throws(FlowException::class)
            override fun checkTransaction(@NotNull stx: SignedTransaction) {
                val state = stx.tx.outputs.iterator().next().data as TradeState
                println("accept transaction with linear ${state.linearId}")

            }
        })
        return subFlow(ReceiveFinalityFlow(counterpartySession))
    }
}