package com.ke.addresspicker

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.annotation.MainThread
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.services.core.AMapException
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.help.Inputtips
import com.amap.api.services.help.InputtipsQuery
import com.amap.api.services.help.Tip
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.hi.dhl.binding.viewbind
import com.ke.addresspicker.databinding.AddressPickerFragmentSearchBinding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

import java.util.concurrent.TimeUnit


class SearchFragment : Fragment(), Inputtips.InputtipsListener {


    private val foregroundColorSpan: ForegroundColorSpan = ForegroundColorSpan(Color.RED)

    private lateinit var locationClient: AMapLocationClient

    private var currentSearchText = ""

    private var currentCityName = ""

    private var currentPoint: LatLonPoint? = null


    private val binding: AddressPickerFragmentSearchBinding by viewbind()

    private val textStateFlow = MutableStateFlow("")


    private val baseQuickAdapter =
        object :
            BaseQuickAdapter<Tip, BaseViewHolder>(R.layout.address_picker_item_search_address_tip) {
            override fun convert(holder: BaseViewHolder, item: Tip) {


                val name = item.name ?: ""

                val address =
                    item.district ?: ""

                if (name.contains(currentSearchText)) {
                    val spannableStringBuilder = SpannableStringBuilder(name)

                    val index = name.indexOf(currentSearchText)

                    spannableStringBuilder.setSpan(
                        foregroundColorSpan,
                        index,
                        index + currentSearchText.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    holder.setText(R.id.name, spannableStringBuilder)
                } else {
                    holder.setText(R.id.name, name)
                }

                holder.setText(R.id.address, address)
            }

        }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        binding.back.setOnClickListener {
            activity?.onBackPressed()
        }

        binding.recyclerView.adapter = baseQuickAdapter

//        binding.address.textChanges().debounce(500, TimeUnit.MILLISECONDS)
//            .skip(1)
//            .observeOn(AndroidSchedulers.mainThread())
//            .subscribe {
//                onSearchTextChanged(it)
//            }.addTo(compositeDisposable)

        binding.address.doAfterTextChanged {
            textStateFlow.value = it?.toString() ?: ""
        }

        lifecycleScope.launch {
            textStateFlow
                .debounce(1000)
                .filter {
                it.isNotEmpty()
            }.collect {
                onSearchTextChanged(it)
            }
        }

        initLocationClient()


        baseQuickAdapter.setOnItemClickListener { _, _, position ->
            val tip = baseQuickAdapter.getItem(position)
            hideKeyboard()

            (activity as? HostActivity)?.showAddressLocationView(tip)
        }
    }


    override fun onResume() {
        super.onResume()

        showSoftKeyboard(binding.address)
    }


    private fun hideKeyboard() {
        activity?.apply {
            val view = currentFocus
            if (view != null) {
                val manager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                manager.hideSoftInputFromWindow(view.windowToken, 0)
                view.clearFocus()
            }
        }
    }

    @MainThread
    private fun showSoftKeyboard(view: View) {
        view.requestFocus()
        view.postDelayed({
            val manager =
                view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
            manager?.showSoftInput(view, 0)
        }, 100)
    }

    private fun initLocationClient() {
        locationClient = AMapLocationClient(activity?.application)

        locationClient.setLocationListener {

            if (it.errorCode != 0) {
                return@setLocationListener
            }
            currentCityName = it.city
            locationClient.stopLocation()
            currentPoint = LatLonPoint(it.latitude, it.longitude)


        }

        val locationClientOption = AMapLocationClientOption().apply {
            locationMode = AMapLocationClientOption.AMapLocationMode.Battery_Saving
            isNeedAddress = true
            isOnceLocation = false
            interval = 2000 * 10
        }

        locationClient.setLocationOption(locationClientOption)
        locationClient.startLocation()
    }

    private fun onSearchTextChanged(content: CharSequence?) {
        currentSearchText = content?.toString() ?: ""
        val inputtipsQuery = InputtipsQuery(currentSearchText, currentCityName)
        inputtipsQuery.cityLimit = false
        if (currentPoint != null) {
            inputtipsQuery.location = currentPoint
        }
        val inputTips = Inputtips(activity, inputtipsQuery)
        inputTips.setInputtipsListener(this)
        inputTips.requestInputtipsAsyn()

    }


    override fun onGetInputtips(list: MutableList<Tip>?, code: Int) {
        if (code == AMapException.CODE_AMAP_SUCCESS && list != null) {
            baseQuickAdapter.setList(list.filter { it.point != null })
        } else {
            baseQuickAdapter.setList(null)
        }
    }
}