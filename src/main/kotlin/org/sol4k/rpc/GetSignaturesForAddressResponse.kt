package org.sol4k.rpc

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import org.sol4k.api.Commitment

@Serializable
internal data class GetSignaturesForAddressResponse(
    val signature: String,
    val slot: Long,
    val err: JsonElement? = null,
    val memo: String? = null,
    val blockTime: Long? = null,
    val confirmationStatus: Commitment? = null,
)
