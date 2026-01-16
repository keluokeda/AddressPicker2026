package com.ke.addresspicker

import android.Manifest
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.ke.permission.KePermission
import kotlinx.coroutines.launch

internal class DelegateFragment : Fragment() {


    internal var resultHandler: ((AddressPickResult) -> Unit)? = null

    private lateinit var launcher: ActivityResultLauncher<Unit>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        launcher = registerForActivityResult(PickerAddressContacts()) {
            resultHandler?.invoke(it)
        }
    }

    fun start() {


        lifecycleScope.launch {
           if( KePermission(childFragmentManager).requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)).firstOrNull()?.granted == true){
               launcher.launch(Unit)
           }else{
               resultHandler?.invoke(AddressPickResult.NoPermission)
           }
        }


    }


}