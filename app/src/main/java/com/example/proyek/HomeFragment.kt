package com.example.proyek

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.firebase.database.*

class HomeFragment : Fragment(), OnMapReadyCallback {

    // ===== MAP =====
    private var googleMap: GoogleMap? = null
    private var focusLat: Double? = null
    private var focusLng: Double? = null

    // ===== BUTTON =====
    private lateinit var btnZoomIn: ImageButton
    private lateinit var btnZoomOut: ImageButton
    private lateinit var btnAdd: ImageButton
    private lateinit var btnSearch: ImageButton
    private lateinit var btnFilter: ImageButton
    private lateinit var btnOpenGoogleMaps: Button

    // ===== SEARCH =====
    private lateinit var searchOverlay: LinearLayout
    private lateinit var etSearch: EditText
    private lateinit var rvReports: RecyclerView

    // ===== DATA =====
    private lateinit var adapter: PetReportAdapter
    private val allReports = mutableListOf<PetReport>()
    private val database = FirebaseDatabase.getInstance()

    // ===== FILTER STATE =====
    private var filterType: ReportType? = null
    private var filterAnimalType: String? = null
    private var filterColor: String? = null
    private var filterAnimalName: String? = null

    private lateinit var btnResetMap: ImageButton


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ===== TERIMA KOORDINAT DARI DETAIL =====
        arguments?.let {
            if (it.containsKey("focus_lat")) {
                focusLat = it.getDouble("focus_lat")
                focusLng = it.getDouble("focus_lng")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ===== INIT VIEW =====
        btnZoomIn = view.findViewById(R.id.btnZoomIn)
        btnZoomOut = view.findViewById(R.id.btnZoomOut)
        btnAdd = view.findViewById(R.id.btnAdd)
        btnSearch = view.findViewById(R.id.btnSearch)
        btnFilter = view.findViewById(R.id.btnFilter)
        btnOpenGoogleMaps = view.findViewById(R.id.btnOpenGoogleMaps)

        searchOverlay = view.findViewById(R.id.searchOverlay)
        etSearch = view.findViewById(R.id.etSearchAnimal)
        rvReports = view.findViewById(R.id.rvSearchResults)
        btnResetMap = view.findViewById(R.id.btnResetMap)


        // ===== RECYCLER =====
        rvReports.layoutManager = LinearLayoutManager(requireContext())
        adapter = PetReportAdapter(mutableListOf()) { report ->
            val bundle = Bundle().apply {
                putString("reportId", report.id)
                putString("reportType", report.type.name)
            }
            findNavController().navigate(
                R.id.action_home_to_reportDetailFragment,
                bundle
            )
        }
        rvReports.adapter = adapter

        // ===== MAP =====
        val mapFragment =
            childFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // ===== LOAD DATA =====
        loadAllReports()

        // ===== BUTTON =====
        btnAdd.setOnClickListener { showAddPopup(view) }

        btnSearch.setOnClickListener {
            searchOverlay.visibility = View.VISIBLE
            etSearch.requestFocus()
        }

        btnFilter.setOnClickListener {
            showFilterDialog()
        }

        // ===== SEARCH INPUT =====
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilter()
            }
        })

        // ===== TOP SEARCH BAR =====
        view.findViewById<EditText>(R.id.etSearch).apply {
            isFocusable = false
            setOnClickListener {
                searchOverlay.visibility = View.VISIBLE
                etSearch.requestFocus()
            }
        }

        btnResetMap.setOnClickListener {
            resetMapToDefault()
        }

    }

    // ================= MAP =================
    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        btnZoomIn.setOnClickListener {
            googleMap?.animateCamera(CameraUpdateFactory.zoomIn())
        }

        btnZoomOut.setOnClickListener {
            googleMap?.animateCamera(CameraUpdateFactory.zoomOut())
        }

        // ===== JIKA ADA FOCUS =====
        if (focusLat != null && focusLng != null) {
            val pos = LatLng(focusLat!!, focusLng!!)

            map.clear()
            map.addMarker(
                MarkerOptions()
                    .position(pos)
                    .title("Lokasi Laporan")
            )

            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(pos, 16f)
            )

            showViewInGoogleMapsButton(pos)

            btnResetMap.visibility = View.VISIBLE

        } else {
            // DEFAULT MAP
            val start = LatLng(-6.2, 106.816666)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(start, 13f))
        }
    }

    // ================= VIEW IN GOOGLE MAPS =================
    private fun showViewInGoogleMapsButton(pos: LatLng) {
        btnOpenGoogleMaps.visibility = View.VISIBLE

        btnOpenGoogleMaps.setOnClickListener {
            val uri = Uri.parse(
                "geo:${pos.latitude},${pos.longitude}?q=${pos.latitude},${pos.longitude}"
            )
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("com.google.android.apps.maps")
            startActivity(intent)
        }
    }

    // ================= FIREBASE =================
    private fun loadAllReports() {
        allReports.clear()
        adapter.updateData(emptyList())
        loadLostReports()
        loadFoundReports()
    }

    private fun loadLostReports() {
        database.reference.child("lost_reports")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.forEach {
                        allReports.add(
                            PetReport(
                                id = it.key ?: "",
                                type = ReportType.LOST,
                                animalName = it.child("animalName").getValue(String::class.java),
                                animalType = it.child("animalType").getValue(String::class.java) ?: "",
                                animalColor = it.child("animalColor").getValue(String::class.java) ?: "",
                                ownerEmail = it.child("ownerEmail").getValue(String::class.java),
                                photoUrl = it.child("photoUrl").getValue(String::class.java)
                            )
                        )
                    }
                    applyFilter()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun loadFoundReports() {
        database.reference.child("found_reports")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.forEach {
                        allReports.add(
                            PetReport(
                                id = it.key ?: "",
                                type = ReportType.FOUND,
                                animalName = it.child("animalName").getValue(String::class.java),
                                animalType = it.child("animalType").getValue(String::class.java) ?: "",
                                animalColor = it.child("animalColor").getValue(String::class.java) ?: "",
                                ownerEmail = null,
                                photoUrl = it.child("photoUrl").getValue(String::class.java)
                            )
                        )
                    }
                    applyFilter()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // ================= FILTER =================
    private fun applyFilter() {
        val keyword = etSearch.text.toString().lowercase()

        val filtered = allReports.filter { r ->
            val matchKeyword =
                keyword.isBlank() ||
                        r.animalName?.lowercase()?.contains(keyword) == true ||
                        r.animalType.lowercase().contains(keyword)

            val matchType = filterType == null || r.type == filterType
            val matchAnimal = filterAnimalType == null || r.animalType == filterAnimalType
            val matchColor = filterColor == null || r.animalColor == filterColor
            val matchName = filterAnimalName == null ||
                    r.animalName?.contains(filterAnimalName!!, true) == true

            matchKeyword && matchType && matchAnimal && matchColor && matchName
        }

        adapter.updateData(filtered)
    }

    // ================= FILTER DIALOG =================
    private fun showFilterDialog() {
        val v = layoutInflater.inflate(R.layout.dialog_filter, null)

        val spinnerType = v.findViewById<Spinner>(R.id.spinnerType)
        val spinnerAnimal = v.findViewById<Spinner>(R.id.spinnerAnimal)
        val spinnerColor = v.findViewById<Spinner>(R.id.spinnerColor)
        val etName = v.findViewById<EditText>(R.id.etAnimalName)

        fun setupSpinner(spinner: Spinner, data: List<String>) {
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                data
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }

        setupSpinner(spinnerType, listOf("Semua", "Lost", "Found"))
        setupSpinner(spinnerAnimal, listOf("Semua", "Anjing", "Kucing", "Burung", "Lainnya"))
        setupSpinner(spinnerColor, listOf("Semua", "Hitam", "Putih", "Coklat", "Belang"))

        AlertDialog.Builder(requireContext())
            .setTitle("Filter")
            .setView(v)
            .setPositiveButton("Terapkan") { _, _ ->
                filterType = when (spinnerType.selectedItem.toString()) {
                    "Lost" -> ReportType.LOST
                    "Found" -> ReportType.FOUND
                    else -> null
                }

                filterAnimalType =
                    spinnerAnimal.selectedItem.toString().takeIf { it != "Semua" }

                filterColor =
                    spinnerColor.selectedItem.toString().takeIf { it != "Semua" }

                filterAnimalName =
                    etName.text.toString().takeIf { it.isNotBlank() }

                searchOverlay.visibility = View.VISIBLE
                applyFilter()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    // ================= POPUP =================
    private fun showAddPopup(parentView: View) {
        val popupView = layoutInflater.inflate(R.layout.popup_report, null)
        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popupWindow.showAtLocation(parentView, Gravity.CENTER, 0, 0)

        popupView.findViewById<LinearLayout>(R.id.optionLost).setOnClickListener {
            popupWindow.dismiss()
            findNavController().navigate(R.id.action_home_to_reportLostFragment)
        }

    }
    private fun resetMapToDefault() {
        val map = googleMap ?: return

        // hapus focus
        focusLat = null
        focusLng = null

        // bersihkan map
        map.clear()

        // posisi default Home
        val defaultPos = LatLng(-6.2, 106.816666)
        map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(defaultPos, 13f)
        )

        // sembunyikan tombol tambahan
        btnOpenGoogleMaps.visibility = View.GONE
        btnResetMap.visibility = View.GONE
    }

}
