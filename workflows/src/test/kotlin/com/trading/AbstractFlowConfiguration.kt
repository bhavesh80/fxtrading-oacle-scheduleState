package com.trade

import com.trading.flows.QueryHandler
import com.trading.flows.SignHandler
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.testing.core.singleIdentity
import net.corda.testing.node.*
import org.junit.AfterClass
import org.junit.BeforeClass

abstract class AbstractFlowConfiguration {

    companion object {

        lateinit var mockNetwork: MockNetwork
        lateinit var initiatorNode: StartedMockNode
        lateinit var counterPartyNode: StartedMockNode

        lateinit var initiatorParty: Party
        lateinit var counterParty: Party


        object InitiatorNodeInfo {
            const val organization = "PartyA"
            const val locality = "Goa"
            const val country = "IN"
        }

        object CounterPartyNodeInfo {
            const val organization = "PartyB"
            const val locality = "Goa"
            const val country = "IN"
        }

        @JvmStatic
        @BeforeClass
        fun setup() {

            val contractsCordapp = "com.trading.contracts"
            val flowsCordapp = "com.trading.flows"

            mockNetwork = MockNetwork(
                MockNetworkParameters(
                    cordappsForAllNodes = listOf(
                        TestCordapp.findCordapp(contractsCordapp),
                        TestCordapp.findCordapp(flowsCordapp)
                    ),
                    threadPerNode = true
                )
            )

            counterPartyNode = mockNetwork.createPartyNode(
                (CordaX500Name(
                    CounterPartyNodeInfo.organization,
                    CounterPartyNodeInfo.locality,
                    CounterPartyNodeInfo.country
                ))
            )

            initiatorNode = mockNetwork.createPartyNode(
                (CordaX500Name(
                    InitiatorNodeInfo.organization,
                    InitiatorNodeInfo.locality,
                    InitiatorNodeInfo.country
                ))
            )

            counterParty = counterPartyNode.info.singleIdentity()

            initiatorParty = initiatorNode.info.singleIdentity()

            val oracleName = CordaX500Name("Oracle", "New York", "US")
            val oracle = mockNetwork.createNode(MockNodeParameters(legalName = oracleName))
            listOf(QueryHandler::class.java, SignHandler::class.java).forEach { oracle.registerInitiatedFlow(it) }

            mockNetwork.startNodes()
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            mockNetwork.stopNodes()
        }
    }

}