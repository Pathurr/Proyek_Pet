package com.example.proyek

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
//melayu
class HomeFragment : Fragment(), OnMapReadyCallback {

    private lateinit var btnAdd: ImageButton
    private lateinit var googleMap: GoogleMap

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnAdd = view.findViewById(R.id.btnAdd)

        val mapFragment =
            childFragmentManager.findFragmentById(R.id.mapFragment)
                    as SupportMapFragment
        mapFragment.getMapAsync(this)

        btnAdd.setOnClickListener {
            showAddPopup(view)
        }
    }

    // ================= GOOGLE MAP =================

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        val startLocation = LatLng(-6.2, 106.816666)

        googleMap.moveCamera(
            CameraUpdateFactory.newLatLngZoom(startLocation, 15f)
        )

        googleMap.addMarker(
            MarkerOptions().position(startLocation).title("Lokasi Awal")
        )

        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isMapToolbarEnabled = false
    }

    // ================= POPUP =================

    private fun showAddPopup(parentView: View) {
        val popupView = LayoutInflater.from(requireContext())
            .inflate(R.layout.popup_report, null)

        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popupWindow.isOutsideTouchable = true

        popupWindow.showAtLocation(parentView, Gravity.CENTER, 0, 0)

        popupView.findViewById<LinearLayout>(R.id.optionLost).setOnClickListener {
            popupWindow.dismiss()
            findNavController()
                .navigate(R.id.action_home_to_reportLostFragment)
        }

        popupView.findViewById<LinearLayout>(R.id.optionFound).setOnClickListener {
            popupWindow.dismiss()
            findNavController()
                .navigate(R.id.action_home_to_reportFoundFragment)
        }
    }
}
