package com.ke.addresspicker

import androidx.fragment.app.FragmentManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class KeAddressPicker(private val fragmentManager: FragmentManager) {



    suspend fun pickAddress(): AddressPickResult {

        val fragment = getFragment()
        return suspendCoroutine {
            fragment.resultHandler = { result ->
               it.resume(result)
            }
            fragment.start()
        }
    }

    private fun getFragment(): DelegateFragment {
        var fragment = fragmentManager.findFragmentByTag(TAG) as? DelegateFragment

        if (fragment == null) {
            fragment = DelegateFragment()
            fragmentManager.beginTransaction().add(fragment, TAG).commitNow()
        }

        return fragment
    }


    companion object {
        private val TAG = DelegateFragment::class.java.name
    }
}