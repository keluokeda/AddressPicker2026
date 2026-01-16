package com.ke.addresspicker

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


sealed class AddressPickResult : Parcelable {

    @Parcelize
    data class Success(
        val latitude: Double,
        val longitude: Double,
        val address: String
    ) : AddressPickResult()

    @Parcelize
    object NoPermission : AddressPickResult()

    @Parcelize
    object Cancel : AddressPickResult()
}

