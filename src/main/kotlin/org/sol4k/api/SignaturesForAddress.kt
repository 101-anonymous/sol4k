package org.sol4k.api

import kotlinx.serialization.json.JsonElement

data class SignaturesForAddress(
    val signature: String,
    val slot: Long,
    val err: JsonElement? = null,
    val memo: String? = null,
    val blockTime: Long? = null,
    val confirmationStatus: Commitment? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SignaturesForAddress

        if (signature != other.signature) return false
        if (slot != other.slot) return false
        if (err != other.err) return false
        if (memo != other.memo) return false
        if (blockTime != other.blockTime) return false
        if (confirmationStatus != other.confirmationStatus) return false

        return true
    }

    override fun hashCode(): Int {
        var result = signature.hashCode()
        result = 31 * result + slot.hashCode()
        result = 31 * result + (err?.hashCode() ?: 0)
        result = 31 * result + (memo?.hashCode() ?: 0)
        result = 31 * result + (blockTime?.hashCode() ?: 0)
        result = 31 * result + (confirmationStatus?.hashCode() ?: 0)
        return result
    }
}
