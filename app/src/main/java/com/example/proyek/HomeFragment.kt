package com.example.proyek

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class HomeFragment : Fragment() {

    private lateinit var mapView: MapView
    private var marker: Marker? = null
    private lateinit var etSearch: EditText
    private lateinit var btnZoomIn: ImageButton
    private lateinit var btnZoomOut: ImageButton
    private lateinit var btnAdd: ImageButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bind views
        mapView = view.findViewById(R.id.mapViewHome)
        etSearch = view.findViewById(R.id.etSearch)
        btnZoomIn = view.findViewById(R.id.btnZoomIn)
        btnZoomOut = view.findViewById(R.id.btnZoomOut)
        btnAdd = view.findViewById(R.id.btnAdd)

        // Load OSMDroid config
        Configuration.getInstance().load(
            requireContext(),
            requireContext().getSharedPreferences("osm_pref", Context.MODE_PRIVATE)
        )

        setupMap()

        // Tombol zoom
        btnZoomIn.setOnClickListener { mapView.controller.zoomIn() }
        btnZoomOut.setOnClickListener { mapView.controller.zoomOut() }

        // Tombol add
        btnAdd.setOnClickListener {
            showAddPopup(view)
        }
    }

    private fun setupMap() {
        mapView.setMultiTouchControls(true)

        // Matikan zoom bawaan OSMDroid
        mapView.zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
        mapView.setBuiltInZoomControls(false)

        // Set center awal map
        val startPoint = GeoPoint(-6.2, 106.816666)
        mapView.controller.setZoom(15.0)
        mapView.controller.setCenter(startPoint)

        // Marker awal
        marker = Marker(mapView).apply {
            position = startPoint
            isDraggable = true
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                override fun onMarkerDragEnd(marker: Marker) {
                    val lat = marker.position.latitude
                    val lng = marker.position.longitude
                    // bisa simpan posisi baru atau update UI jika mau
                }
                override fun onMarkerDragStart(marker: Marker) {}
                override fun onMarkerDrag(marker: Marker) {}
            })
        }
        mapView.overlays.add(marker)
    }

    private fun showAddPopup(parentView: View) {
        val inflater = LayoutInflater.from(requireContext())
        val popupView = inflater.inflate(R.layout.popup_report, null)

        // PopupWindow
        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        popupWindow.isFocusable = true
        popupWindow.isOutsideTouchable = false
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // tampil di tengah fragment
        popupWindow.showAtLocation(parentView, Gravity.CENTER, 0, 0)

        // klik Lost Pet
        popupView.findViewById<LinearLayout>(R.id.optionLost).setOnClickListener {
            popupWindow.dismiss()
            findNavController().navigate(R.id.action_home_to_reportLostFragment)
        }

        // klik Found Pet
        popupView.findViewById<LinearLayout>(R.id.optionFound).setOnClickListener {
            popupWindow.dismiss()
            findNavController().navigate(R.id.action_home_to_reportFoundFragment)
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDetach()
    }
}
