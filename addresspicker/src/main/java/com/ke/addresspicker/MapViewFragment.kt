package com.ke.addresspicker

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.LocationSource
import com.amap.api.maps.model.*
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.geocoder.GeocodeResult
import com.amap.api.services.geocoder.GeocodeSearch
import com.amap.api.services.geocoder.RegeocodeQuery
import com.amap.api.services.geocoder.RegeocodeResult
import com.hi.dhl.binding.viewbind
import com.ke.addresspicker.databinding.AddressPickerFragmentMapViewBinding


internal class MapViewFragment : Fragment(), LocationSource, GeocodeSearch.OnGeocodeSearchListener,
    AMapLocationListener, AMap.OnCameraChangeListener {


    private val binding:AddressPickerFragmentMapViewBinding by viewbind()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
       return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.apply {
            title = "设置地址"
            inflateMenu(R.menu.search)
            setNavigationIcon(R.drawable.baseline_clear_black_24dp)
            setNavigationOnClickListener {
                activity?.onBackPressed()
            }
            setOnMenuItemClickListener { menuItem ->
                if (menuItem.itemId == R.id.search) {
                    (activity as? HostActivity)?.showSearchView()
                }
//                Log.d("TAG","menu click ${menuItem.title}")
                return@setOnMenuItemClickListener true
            }
        }

        initMapView(savedInstanceState)

        binding.done.setOnClickListener {
            activity?.apply {
                if (currentLatLng != null) {
                    val intent = Intent()
                    intent.putExtra(HostActivity.key_latitude, currentLatLng!!.latitude)
                    intent.putExtra(HostActivity.key_longitude, currentLatLng!!.longitude)
                    intent.putExtra(HostActivity.key_address, binding.address.text.toString())
                    setResult(Activity.RESULT_OK, intent)
                    finish()

                }
            }
        }
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        binding.mapView.onSaveInstanceState(outState)
    }


    override fun onResume() {
        super.onResume()

        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()

        binding.mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()

        locationClient?.stopLocation()
        locationClient?.onDestroy()

        binding.mapView.onDestroy()


    }

    private lateinit var mGeocodeSearch: GeocodeSearch
    private lateinit var mAMap: AMap
    private var locationClient: AMapLocationClient? = null
    private var location: LatLng? = null
    private var currentLatLng: LatLng? = null
    private var mGPSMarker: Marker? = null
    private var onLocationChangedListener: LocationSource.OnLocationChangedListener? = null
    private var cityName: String? = null


    private fun initMapView(bundle: Bundle?) {

        binding.mapView.onCreate(bundle)
        mGeocodeSearch = GeocodeSearch(activity)
        mAMap = binding.mapView.map

        mAMap.setLocationSource(this)

        mGeocodeSearch.setOnGeocodeSearchListener(this)


        setupLocationStyle()

        mAMap.moveCamera(CameraUpdateFactory.zoomTo(18f)) //缩放比例


        //设置amap的属性
        val settings = mAMap.uiSettings
        mAMap.isMyLocationEnabled = true// 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false

        settings.isZoomControlsEnabled = false
        settings.isRotateGesturesEnabled = false

    }

    override fun activate(onLocationChangedListener: LocationSource.OnLocationChangedListener?) {
        this.onLocationChangedListener = onLocationChangedListener

        if (locationClient != null) {
            return
        }
        //初始化定位
        locationClient = AMapLocationClient(activity?.application)
        //设置定位回调监听
        locationClient?.setLocationListener(this)

        //初始化定位参数
        val locationClientOption = AMapLocationClientOption()
        //设置定位模式为高精度模式，Battery_Saving为低功耗模式，Device_Sensors是仅设备模式
        locationClientOption.locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
        //设置是否返回地址信息（默认返回地址信息）
        locationClientOption.isNeedAddress = true
        //设置是否只定位一次,默认为false
        locationClientOption.isOnceLocation = false
        //设置是否强制刷新WIFI，默认为强制刷新
//        locationClientOption.setWifiActiveScan(true);
        //设置是否允许模拟位置,默认为false，不允许模拟位置
        locationClientOption.isMockEnable = false
        //设置定位间隔,单位毫秒,默认为2000ms
        locationClientOption.interval = (2000 * 10).toLong()
        //给定位客户端对象设置定位参数
        locationClient?.setLocationOption(locationClientOption)
        //启动定位
        locationClient?.startLocation()
    }

    override fun deactivate() {
        onLocationChangedListener = null

        locationClient?.stopLocation()
        locationClient?.onDestroy()
    }

    override fun onRegeocodeSearched(result: RegeocodeResult?, code: Int) {

        if (code == 1000) {
            if (result != null && result.regeocodeAddress != null
                && result.regeocodeAddress.formatAddress != null
            ) {

                val addressName = result.regeocodeAddress.formatAddress
                    .replace(result.regeocodeAddress.province, "")
                    .replace(result.regeocodeAddress.city, "")


                val lat = result.regeocodeQuery.point.latitude
                val lon = result.regeocodeQuery.point.longitude

                this.location = LatLng(lat, lon)

                binding.address.text = addressName



                binding.done.isEnabled = true
                if (currentLatLng != null) {
                    setMarket(currentLatLng!!)
                }


            }
        }
    }

    private fun setMarket(latLng: LatLng) {
        mGPSMarker?.remove()


        val width = binding.mapView.width / 2

        val height = binding.mapView.height / 2

        val markOptions = MarkerOptions()
        markOptions.draggable(true)//设置Marker可拖动
        markOptions.icon(
            BitmapDescriptorFactory.fromBitmap(
                BitmapFactory.decodeResource(
                    resources,
                    R.mipmap.map_location_red
                )
            )
        ).anchor(0.5f, 0.7f)
        //设置一个角标
        mGPSMarker = mAMap.addMarker(markOptions)
        //设置marker在屏幕的像素坐标
        mGPSMarker?.position = latLng
        //        mGPSMarker.setTitle(title);
        //        mGPSMarker.setSnippet(content);
        //设置像素坐标
        mGPSMarker?.setPositionByPixels(width, height)
        binding.mapView.invalidate()
    }


    override fun onGeocodeSearched(p0: GeocodeResult?, p1: Int) {
    }

    private fun setupLocationStyle() {

        val myLocationStyle = MyLocationStyle()
        myLocationStyle.showMyLocation(false)
        mAMap.myLocationStyle = myLocationStyle
    }

    override fun onLocationChanged(aMapLocation: AMapLocation?) {
        if (onLocationChangedListener != null && aMapLocation != null) {
            if (aMapLocation.errorCode == 0) {
                onLocationChangedListener?.onLocationChanged(aMapLocation)// 显示系统箭头
                binding.address.text = aMapLocation.address

                //定位成功后 提交按钮才可以点击
                binding.done.isEnabled = true

                location = LatLng(aMapLocation.latitude, aMapLocation.longitude)

                cityName = aMapLocation.city


                val la = LatLng(aMapLocation.latitude, aMapLocation.longitude)

                setMarket(la)

                mAMap.moveCamera(CameraUpdateFactory.newLatLngZoom(la, 18f))

                //定位成功后 在开启，防止出现定位在北京的情况。
                mAMap.setOnCameraChangeListener(this)



                locationClient?.stopLocation()
            }
        }
    }

    override fun onCameraChange(p0: CameraPosition?) {
    }

    override fun onCameraChangeFinish(cameraPosition: CameraPosition) {
        val latLng = cameraPosition.target

        loadAddressDetail(latLng)
    }

    /**
     * 根据经纬度得到地址
     */
    private fun loadAddressDetail(latLonPoint: LatLng) {
        // 第一个参数表示一个Latlng，第二参数表示范围多少米，第三个参数表示是火系坐标系还是GPS原生坐标系
        val query = RegeocodeQuery(
            LatLonPoint(latLonPoint.latitude, latLonPoint.longitude),
            500f,
            GeocodeSearch.AMAP
        )
        currentLatLng = latLonPoint
        mGeocodeSearch.getFromLocationAsyn(query)// 设置同步逆地理编码请求
    }
}