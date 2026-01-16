package com.ke.addresspicker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.services.help.Tip
import com.hi.dhl.binding.viewbind
import com.ke.addresspicker.databinding.AddressPickerFragmentAddressLocationBinding


private const val ARGUMENT_TIP = "ARGUMENT_TIP"


class AddressLocationFragment : Fragment() {



    private val binding:AddressPickerFragmentAddressLocationBinding by viewbind()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
       return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        binding.mapView.onCreate(savedInstanceState)

        val tip = arguments?.getParcelable<Tip>(ARGUMENT_TIP) ?: return

        binding.toolbar.apply {
            title = "设置地址"
            inflateMenu(R.menu.done)
            setNavigationIcon(R.drawable.baseline_clear_black_24dp)
            setNavigationOnClickListener {
                activity?.onBackPressed()
            }
            setOnMenuItemClickListener { menuItem ->
                if (menuItem.itemId == R.id.done) {
//                    (activity as? HostActivity)?.showSearchView()
                    val intent = Intent()
                    intent.putExtra(HostActivity.key_latitude, tip.point.latitude)
                    intent.putExtra(HostActivity.key_longitude, tip.point.longitude)
                    intent.putExtra(HostActivity.key_address, tip.name)
                    activity?.setResult(Activity.RESULT_OK, intent)
                    activity?.finish()

                }
//                Log.d("TAG","menu click ${menuItem.title}")
                return@setOnMenuItemClickListener true
            }
        }

        binding.name.text = tip.name
        binding.address.text = if (tip.address.isNullOrEmpty()) tip.district else tip.address

        drawMap(tip)
    }



    /**
     * 开始绘制地图
     */
    private fun drawMap(tip: Tip) {
        val location = tip.point
        binding.mapView.map.apply {
            clear()
            val latLng = LatLng(location.latitude, location.longitude)
            moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f))
            addMarker(MarkerOptions().apply {
                this.position(latLng)
                this.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_location_on_grey_400_24dp))
            })
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        binding.mapView.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView.onSaveInstanceState(outState)
    }


    companion object {

        @JvmStatic
        fun newInstance(tip: Tip) =
            AddressLocationFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARGUMENT_TIP, tip)
                }
            }
    }
}