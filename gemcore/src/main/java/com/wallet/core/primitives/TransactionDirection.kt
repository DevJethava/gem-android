/**
 * Generated by typeshare 1.13.2
 */

package com.wallet.core.primitives

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
enum class TransactionDirection(val string: String) {
	@SerialName("self")
	SelfTransfer("self"),
	@SerialName("outgoing")
	Outgoing("outgoing"),
	@SerialName("incoming")
	Incoming("incoming"),
}

