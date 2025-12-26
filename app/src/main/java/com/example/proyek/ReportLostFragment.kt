package com.example.proyek

import android.app.Activity
import android.content.Intent
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
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

class ReportLostFragment : Fragment(), OnMapReadyCallback {

    companion object {
        const val PICK_IMAGE_REQUEST = 100
    }

    // ================= UI =================
    private lateinit var etAnimalName: EditText
    private lateinit var ivAnimalPhoto: ImageView
    private lateinit var btnPickPhoto: Button
    private lateinit var spinnerAnimalType: Spinner
    private lateinit var etAnimalColor: EditText
    private lateinit var etDescription: EditText
    private lateinit var tvLocation: TextView
    private lateinit var etContact: EditText
    private lateinit var btnSave: Button

    // ================= MAP =================
    private lateinit var googleMap: GoogleMap
    private var marker: Marker? = null

    // ================= FIREBASE =================
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    // ================= IMAGE =================
    private var selectedImageUri: Uri? = null
    private var animalPhotoUrl: String? = null
    private var isUploading = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_report_lost, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ensureCloudinary()

        etAnimalName = view.findViewById(R.id.etAnimalName)
        ivAnimalPhoto = view.findViewById(R.id.ivAnimalPhoto)
        btnPickPhoto = view.findViewById(R.id.btnPickPhoto)
        spinnerAnimalType = view.findViewById(R.id.spinnerAnimalType)
        etAnimalColor = view.findViewById(R.id.etAnimalColor)
        etDescription = view.findViewById(R.id.etDescription)
        tvLocation = view.findViewById(R.id.tvLocation)
        etContact = view.findViewById(R.id.etContact)
        btnSave = view.findViewById(R.id.btnSaveReport)

        setupSpinner()

        val mapFragment = childFragmentManager
            .findFragmentById(R.id.mapFragmentLost) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val scrollView = view.findViewById<ScrollView>(R.id.scrollView)
        scrollView?.requestDisallowInterceptTouchEvent(true)

        //val scrollView = view.parent as ScrollView
        val touchInterceptor = view.findViewById<View>(R.id.mapTouchInterceptor)

        touchInterceptor.setOnTouchListener { v, event ->
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

        val btnZoomIn = view.findViewById<ImageButton>(R.id.btnZoomIn)
        val btnZoomOut = view.findViewById<ImageButton>(R.id.btnZoomOut)
        btnZoomIn.setOnClickListener {
            googleMap.animateCamera(CameraUpdateFactory.zoomIn())
        }

        btnZoomOut.setOnClickListener {
            googleMap.animateCamera(CameraUpdateFactory.zoomOut())
        }


        btnPickPhoto.setOnClickListener { pickImage() }
        btnSave.setOnClickListener { saveReport() }
    }

    // ================= MAP =================
    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        val start = LatLng(-6.2, 106.816666)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(start, 15f))

        marker = googleMap.addMarker(
            MarkerOptions()
                .position(start)
                .draggable(true)
        )

        updateLocation(start)

        googleMap.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
            override fun onMarkerDragEnd(marker: Marker) {
                updateLocation(marker.position)
            }
            override fun onMarkerDrag(marker: Marker) {}
            override fun onMarkerDragStart(marker: Marker) {}
        })

        googleMap.uiSettings.isZoomControlsEnabled = false

        view?.findViewById<ImageButton>(R.id.btnZoomIn)?.setOnClickListener {
            googleMap.animateCamera(CameraUpdateFactory.zoomIn())
        }
        view?.findViewById<ImageButton>(R.id.btnZoomOut)?.setOnClickListener {
            googleMap.animateCamera(CameraUpdateFactory.zoomOut())
        }
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
        spinnerAnimalType.adapter = adapter
    }

    // ================= IMAGE =================
    private fun pickImage() {
        startActivityForResult(
            Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" },
            PICK_IMAGE_REQUEST
        )
    }

    // ================= SAVE =================
    private fun saveReport() {
        val user = auth.currentUser ?: return

        if (etAnimalName.text.isEmpty()) {
            etAnimalName.error = "Wajib diisi"
            return
        }

        if (isUploading) return
        isUploading = true

        uploadAnimalPhoto {
            saveToFirebase()
        }
    }

    private fun uploadAnimalPhoto(onDone: () -> Unit) {
        if (selectedImageUri == null) {
            onDone()
            return
        }

        MediaManager.get().upload(selectedImageUri)
            .unsigned("android_unsigned")
            .option("folder", "lost_pets")
            .callback(object : UploadCallback {
                override fun onSuccess(id: String?, data: Map<*, *>) {
                    animalPhotoUrl = data["secure_url"] as String
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
        val user = auth.currentUser ?: return
        val pos = marker!!.position
        val key = database.reference.child("lost_reports").push().key!!

        val data = mapOf(
            "id" to key,
            "animalName" to etAnimalName.text.toString(),
            "animalType" to spinnerAnimalType.selectedItem.toString(),
            "animalColor" to etAnimalColor.text.toString(),
            "description" to etDescription.text.toString(),
            "locationText" to tvLocation.text.toString(),
            "lat" to pos.latitude,
            "lng" to pos.longitude,
            "contact" to etContact.text.toString(),
            "photoUrl" to animalPhotoUrl,
            "ownerUid" to user.uid,
            "ownerEmail" to user.email,
            "createdAt" to System.currentTimeMillis()
        )

        database.reference.child("lost_reports")
            .child(key)
            .setValue(data)
            .addOnSuccessListener {
                isUploading = false
                Toast.makeText(context, "Report tersimpan", Toast.LENGTH_LONG).show()
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data
            ivAnimalPhoto.setImageURI(selectedImageUri)
        }
    }

    // ================= CLOUDINARY =================
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
