package com.example.proyek

import android.app.Activity
import android.content.Intent
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.UploadCallback
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.*

class ReportFoundFragment : Fragment(), OnMapReadyCallback {

    companion object {
        const val PICK_IMAGE_REQUEST = 101
    }

    // UI
    private lateinit var etAnimalName: EditText
    private lateinit var etAnimalType: Spinner
    private lateinit var etAnimalColor: EditText
    private lateinit var etDescription: EditText
    private lateinit var etContact: EditText
    private lateinit var tvLocation: TextView
    private lateinit var ivPhoto: ImageView
    private lateinit var btnPickPhoto: Button
    private lateinit var btnSave: Button

    // MAP
    private lateinit var googleMap: GoogleMap
    private var marker: Marker? = null

    // IMAGE
    private var selectedImageUri: Uri? = null
    private var photoUrl: String? = null
    private var isUploading = false

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_report_found, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ensureCloudinary()

        etAnimalName = view.findViewById(R.id.etAnimalName)
        etAnimalType = view.findViewById(R.id.spinnerAnimalType)
        etAnimalColor = view.findViewById(R.id.etAnimalColor)
        etDescription = view.findViewById(R.id.etDescription)
        etContact = view.findViewById(R.id.etContact)
        tvLocation = view.findViewById(R.id.tvLocation)
        ivPhoto = view.findViewById(R.id.ivAnimalPhoto)
        btnPickPhoto = view.findViewById(R.id.btnPickPhoto)
        btnSave = view.findViewById(R.id.btnSaveReport)

        setupSpinner()

        val mapFragment =
            childFragmentManager.findFragmentById(R.id.mapFragmentFound) as SupportMapFragment
        mapFragment.getMapAsync(this)

        btnPickPhoto.setOnClickListener { pickImage() }
        btnSave.setOnClickListener { saveReport() }

        val scrollView = view.findViewById<ScrollView>(R.id.scrollView)
        val touchInterceptor = view.findViewById<View>(R.id.mapTouchInterceptor)

        touchInterceptor.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN,
                MotionEvent.ACTION_MOVE -> {
                    scrollView.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    scrollView.requestDisallowInterceptTouchEvent(false)
                }
            }
            false
        }

    }

    // ================= MAP =================
    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        val start = LatLng(-6.2, 106.816666)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(start, 15f))

        marker = googleMap.addMarker(
            MarkerOptions().position(start).draggable(true)
        )

        updateLocation(start)

        googleMap.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
            override fun onMarkerDragEnd(marker: Marker) {
                updateLocation(marker.position)
            }
            override fun onMarkerDrag(marker: Marker) {}
            override fun onMarkerDragStart(marker: Marker) {}
        })

        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isMapToolbarEnabled = false
    }

    private fun updateLocation(latLng: LatLng) {
        try {
            val geo = Geocoder(requireContext(), Locale.getDefault())
            val list = geo.getFromLocation(latLng.latitude, latLng.longitude, 1)
            tvLocation.text = list?.firstOrNull()?.getAddressLine(0)
                ?: "Lat: %.5f, Lng: %.5f".format(latLng.latitude, latLng.longitude)
        } catch (e: Exception) {
            tvLocation.text =
                "Lat: %.5f, Lng: %.5f".format(latLng.latitude, latLng.longitude)
        }
    }

    // ================= SPINNER =================
    private fun setupSpinner() {
        val items = listOf("Anjing", "Kucing", "Burung", "Hamster", "Lainnya")
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            items
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        etAnimalType.adapter = adapter
    }

    // ================= IMAGE =================
    private fun pickImage() {
        startActivityForResult(
            Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" },
            PICK_IMAGE_REQUEST
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data
            ivPhoto.setImageURI(selectedImageUri)
        }
    }

    // ================= SAVE =================
    private fun saveReport() {
        if (isUploading) return
        isUploading = true

        uploadPhoto {
            saveToFirebase()
        }
    }

    private fun uploadPhoto(onDone: () -> Unit) {
        if (selectedImageUri == null) {
            onDone()
            return
        }

        MediaManager.get().upload(selectedImageUri)
            .unsigned("android_unsigned")
            .option("folder", "found_pets")
            .callback(object : UploadCallback {
                override fun onSuccess(id: String?, data: Map<*, *>) {
                    photoUrl = data["secure_url"] as String
                    onDone()
                }

                override fun onError(id: String?, error: com.cloudinary.android.callback.ErrorInfo?) {
                    isUploading = false
                }

                override fun onStart(id: String?) {}
                override fun onProgress(id: String?, bytes: Long, total: Long) {}
                override fun onReschedule(id: String?, error: com.cloudinary.android.callback.ErrorInfo?) {}
            }).dispatch()
    }

    private fun saveToFirebase() {
        val pos = marker!!.position
        val key = database.reference.child("found_reports").push().key!!
        val nameInput = etAnimalName.text.toString().trim()

        val data = mapOf(
            "id" to key,
            "animalName" to if (nameInput.isEmpty()) null else nameInput,
            "animalType" to etAnimalType.selectedItem.toString(),
            "animalColor" to etAnimalColor.text.toString(),
            "description" to etDescription.text.toString(),
            "locationText" to tvLocation.text.toString(),
            "lat" to pos.latitude,
            "lng" to pos.longitude,
            "contact" to etContact.text.toString(),
            "photoUrl" to photoUrl,
            "foundByUid" to auth.currentUser?.uid,
            "createdAt" to System.currentTimeMillis()
        )

        database.reference.child("found_reports")
            .child(key)
            .setValue(data)
            .addOnSuccessListener {
                isUploading = false
                Toast.makeText(context, "Laporan ditemukan tersimpan", Toast.LENGTH_LONG).show()
            }
    }

    private fun ensureCloudinary() {
        try {
            MediaManager.get()
        } catch (e: IllegalStateException) {
            MediaManager.init(
                requireContext().applicationContext,
                mapOf("cloud_name" to "dwbkytgil")
            )
        }
    }
}
