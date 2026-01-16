package com.ke.addresspicker

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract

internal class PickerAddressContacts : ActivityResultContract<Unit, AddressPickResult>() {
    override fun createIntent(context: Context, input: Unit): Intent {
        return Intent(context, HostActivity::class.java)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): AddressPickResult {
        return if (resultCode == Activity.RESULT_OK && intent != null) {
            val latitude = intent.getDoubleExtra(HostActivity.key_latitude, .0)
            val longitude = intent.getDoubleExtra(HostActivity.key_longitude, .0)
            val address = intent.getStringExtra(HostActivity.key_address) ?: ""
            AddressPickResult.Success(
                latitude, longitude, address
            )
        } else {
            AddressPickResult.Cancel
        }
    }
}