package com.trading.services

import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.SerializationEnvironmentRule
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class RatesServiceTests {
    private val oracleIdentity = TestIdentity(CordaX500Name("Oracle", "New York", "US"))
    private val dummyServices = MockServices(listOf("com.trading.contracts"), oracleIdentity)
    private val oracle = ExchangeRatesOracle(dummyServices)
    private val PartyA = TestIdentity(CordaX500Name("PartyA", "", "GB"))
    private val PartyB = TestIdentity(CordaX500Name("PartyB", "", "GB"))

    @Rule
    @JvmField
    val testSerialization = SerializationEnvironmentRule()

    @Test
    fun `oracle returns correct Nth prime`() {
        assertEquals(73.79, oracle.query("USD/INR"))
    }

    @Test
    fun `oracle rejects invalid values of N`() {
//        assertFailsWith<IllegalArgumentException> { oracle.query("") }
//        assertFailsWith<IllegalArgumentException> { oracle.query("Dollar") }
    }
}