/**
 * Generated by typeshare 1.13.2
 */

package com.wallet.core.blockchain.solana.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class SolanaBlockhash (
	val blockhash: String
)

@Serializable
data class SolanaBlockhashResult (
	val value: SolanaBlockhash
)

