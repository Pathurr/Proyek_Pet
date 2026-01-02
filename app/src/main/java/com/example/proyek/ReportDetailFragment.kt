package com.example.proyek

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.*

class ReportDetailFragment : Fragment(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private val db = FirebaseDatabase.getInstance()

    private lateinit var reportId: String
    private lateinit var reportType: String

    private var reportLat: Double? = null
    private var reportLng: Double? = null
    private var reporterEmail: String? = null
    private var reporterPhone: String? = null

    private lateinit var btnReportFound: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_report_detail, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        reportId = requireArguments().getString("reportId")!!
        reportType = requireArguments().getString("reportType")!!

        btnReportFound = view.findViewById(R.id.btnReportFound)

        // ❌ FOUND PET → tidak perlu tombol report found
        if (reportType == "FOUND") {
            btnReportFound.visibility = View.GONE
        }

        val mapFragment =
            childFragmentManager.findFragmentById(R.id.mapFragment)
                    as SupportMapFragment
        mapFragment.getMapAsync(this)

        loadDetail(view)
    }

    private fun loadDetail(view: View) {
        val path = if (reportType == "LOST") "lost_reports" else "found_reports"

        db.reference.child(path).child(reportId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(s: DataSnapshot) {
                    if (!s.exists()) return

                    val name = s.child("animalName").getValue(String::class.java)
                    val imageUrl = s.child("photoUrl").getValue(String::class.java)
                    val type = s.child("animalType").getValue(String::class.java)
                    val color = s.child("animalColor").getValue(String::class.java)
                    val desc = s.child("description").getValue(String::class.java)
                    val locText = s.child("locationText").getValue(String::class.java)
                    val email = s.child("ownerEmail").getValue(String::class.java)
                    val phone = s.child("contact").getValue(String::class.java)

                    val lat = s.child("lat").getValue(Double::class.java)
                    val lng = s.child("lng").getValue(Double::class.java)

                    // ===== SET UI =====
                    view.findViewById<TextView>(R.id.tvName).text = name
                    view.findViewById<TextView>(R.id.tvType).text = type
                    view.findViewById<TextView>(R.id.tvColor).text = color
                    view.findViewById<TextView>(R.id.tvDescription).text = desc
                    view.findViewById<TextView>(R.id.tvLocation).text = locText
                    view.findViewById<TextView>(R.id.tvContact).text = phone
                    view.findViewById<TextView>(R.id.tvEmail).text = email

                    reporterEmail = email
                    reporterPhone = phone
                    reportLat = lat
                    reportLng = lng

                    // ===== IMAGE =====
                    val img = view.findViewById<ImageView>(R.id.imgAnimal)
                    if (!imageUrl.isNullOrEmpty()) {
                        Glide.with(this@ReportDetailFragment)
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_image_placeholder)
                            .into(img)
                    }

                    // ===== MAP =====
                    if (lat != null && lng != null && ::googleMap.isInitialized) {
                        val pos = LatLng(lat, lng)
                        googleMap.clear()
                        googleMap.addMarker(MarkerOptions().position(pos))
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 16f))
                        googleMap.uiSettings.setAllGesturesEnabled(false)
                    }

                    // ===== VIEW ON HOME MAP =====
                    view.findViewById<Button>(R.id.btnViewLocation).setOnClickListener {
                        if (reportLat != null && reportLng != null) {
                            val bundle = Bundle().apply {
                                putDouble("focus_lat", reportLat!!)
                                putDouble("focus_lng", reportLng!!)
                            }
                            findNavController().navigate(
                                R.id.action_reportDetailFragment_to_homeFragment,
                                bundle
                            )
                        }
                    }

                    // ===== REPORT FOUND (CONTACT ONLY) =====
                    btnReportFound.setOnClickListener {
                        showContactDialog()
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // ================= CONTACT =================
    private fun showContactDialog() {
        val options = arrayOf("Email", "Phone Number")

        AlertDialog.Builder(requireContext())
            .setTitle("Contact Owner")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openEmail()
                    1 -> openDial()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openEmail() {
        if (reporterEmail.isNullOrEmpty()) return

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$reporterEmail")
            putExtra(Intent.EXTRA_SUBJECT, "I found your pet")
            putExtra(Intent.EXTRA_TEXT, "Hi, I think I found your pet.")
        }
        startActivity(intent)
    }

    private fun openDial() {
        if (reporterPhone.isNullOrEmpty()) return

        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$reporterPhone")
        }
        startActivity(intent)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
    }
}
