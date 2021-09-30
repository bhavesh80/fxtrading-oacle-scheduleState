package com.trading.flows

import com.trading.states.TradeState
import net.corda.client.rpc.notUsed
import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.getOrThrow
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.singleIdentity
import net.corda.testing.node.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class TradeFlowTests() {
    private val mockNet = MockNetwork(
        MockNetworkParameters( cordappsForAllNodes = listOf(
                TestCordapp.findCordapp("com.trading.flows"),
                TestCordapp.findCordapp("com.trading.contracts"))

            )
        )
    private lateinit var a: StartedMockNode
    private lateinit var b: StartedMockNode

    @Before
    fun setUp() {
        a = mockNet.createNode()
        b = mockNet.createNode()

        val oracleName = CordaX500Name("Oracle", "New York", "US")
        val oracle = mockNet.createNode(MockNodeParameters(legalName = oracleName))
        listOf(QueryHandler::class.java, SignHandler::class.java).forEach { oracle.registerInitiatedFlow(it) }

        mockNet.runNetwork()
    }

    @After
    fun tearDown() {
        mockNet.stopNodes()
    }

//    @Test
//    fun `Temp test for oracle`() {
//
//        val tradeData = TradeInitiator(100.0, "USD", "INR")
//        val flow = a.startFlow(tradeData)
//        mockNet.runNetwork()
//
//        val result = flow.getOrThrow().tx.outputsOfType<TradeState>().single()
//
//        assertEquals(73.81, result.currencyRate)
//        assertEquals("Pending", result.tradeStatus)
//        //        val prime100 = 541
////        assertEquals(prime100, result.nthPrime)
//    }

    @Test
    fun `Single party test for oracle`() {

        val tradeData = SinglePartyExchangeCurrency(100.0, "USD", "INR")
        val flow = a.startFlow(tradeData)
        mockNet.runNetwork()

        val result = flow.getOrThrow().tx.outputsOfType<TradeState>().single()

        assertEquals(73.81, result.currencyRate)
        assertEquals("Pending", result.tradeStatus)
        //        val prime100 = 541
//        assertEquals(prime100, result.nthPrime)
    }

        @Test
        fun `schedule currency rate and buyCurrency update`() {
            val tradeData = SinglePartyExchangeCurrency(100.0, "USD", "INR")
            val flow = a.startFlow(tradeData)

            val sleepTime: Long = 25000
            Thread.sleep(sleepTime)

            val recordedTxs = a.transaction {
                val (recordedTxs, futureTxs) = a.services.validatedTransactions.track()
                futureTxs.notUsed()
                recordedTxs
            }

            val totalExpectedTransactions = 1
            assertEquals(totalExpectedTransactions, recordedTxs.size)


        }

    @Test
    fun `Two party test for oracle`() {

        val tradeData = TwoPartyTradeFlow(100.0, "USD", "INR",b.info.singleIdentity())
        val flow = a.startFlow(tradeData)
        mockNet.runNetwork()

        val result = flow.getOrThrow().tx.outputsOfType<TradeState>().single()

        assertEquals(73.81, result.currencyRate)
        assertEquals("Pending", result.tradeStatus)
        //        val prime100 = 541
//        assertEquals(prime100, result.nthPrime)
    }
    }
