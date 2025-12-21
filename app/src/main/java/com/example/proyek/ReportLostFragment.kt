package com.example.proyek

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.util.*

class ReportLostFragment : Fragment() {

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
    private lateinit var mapView: MapView

    // ================= MAP =================
    private var marker: Marker? = null

    // ================= FIREBASE =================
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    // ================= IMAGE =================
    private var selectedImageUri: Uri? = null
    private var animalPhotoUrl: String? = null
    private var mapImageUrl: String? = null
    private var isUploading = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_report_lost, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 🔥 CLOUDINARY INIT (1 FILE, AMAN)
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
        mapView = view.findViewById(R.id.mapViewLost)

        Configuration.getInstance().load(
            requireContext(),
            requireContext().getSharedPreferences("osm_pref", Context.MODE_PRIVATE)
        )

        setupSpinner()
        setupMap()

        btnPickPhoto.setOnClickListener { pickImage() }
        btnSave.setOnClickListener { saveReport() }
    }

    // ================= CLOUDINARY =================
    private fun ensureCloudinary() {
        try {
            MediaManager.get()
        } catch (e: IllegalStateException) {
            MediaManager.init(
                requireContext().applicationContext,
                mapOf(
                    "cloud_name" to "dwbkytgil"
                )
            )
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

    // ================= MAP =================
    private fun setupMap() {
        mapView.setMultiTouchControls(true)
        mapView.setBuiltInZoomControls(false)

        val start = GeoPoint(-6.2, 106.816666)
        mapView.controller.setZoom(15.0)
        mapView.controller.setCenter(start)

        marker = Marker(mapView).apply {
            position = start
            isDraggable = true
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                override fun onMarkerDragEnd(marker: Marker) {
                    updateLocation(marker.position)
                }
                override fun onMarkerDrag(marker: Marker) {}
                override fun onMarkerDragStart(marker: Marker) {}
            })
        }

        mapView.overlays.add(marker)
        updateLocation(start)

        mapView.setOnTouchListener { v, _ ->
            v.parent.requestDisallowInterceptTouchEvent(true)
            false
        }
    }

    private fun updateLocation(point: GeoPoint) {
        try {
            val geo = Geocoder(requireContext(), Locale.getDefault())
            val list = geo.getFromLocation(point.latitude, point.longitude, 1)
            tvLocation.text = list?.firstOrNull()?.getAddressLine(0)
                ?: "Lat: %.5f, Lon: %.5f".format(point.latitude, point.longitude)
        } catch (e: Exception) {
            tvLocation.text = "Lat: %.5f, Lon: %.5f".format(point.latitude, point.longitude)
        }
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
            captureMapAndUpload {
                saveToFirebase()
            }
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

    private fun captureMapAndUpload(onDone: () -> Unit) {
        mapView.post {
            val bitmap = Bitmap.createBitmap(
                mapView.width,
                mapView.height,
                Bitmap.Config.ARGB_8888
            )

            val canvas = android.graphics.Canvas(bitmap)
            mapView.draw(canvas)

            val file = File(
                requireContext().cacheDir,
                "map_${System.currentTimeMillis()}.jpg"
            )

            file.outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)
            }

            MediaManager.get().upload(Uri.fromFile(file))
                .unsigned("android_unsigned")
                .option("folder", "lost_pet_maps")
                .callback(object : UploadCallback {
                    override fun onSuccess(id: String?, data: Map<*, *>) {
                        mapImageUrl = data["secure_url"] as String
                        onDone()
                    }
                    override fun onError(id: String?, error: com.cloudinary.android.callback.ErrorInfo?) {
                        isUploading = false
                        Toast.makeText(context, "Upload map gagal", Toast.LENGTH_SHORT).show()
                    }
                    override fun onStart(id: String?) {}
                    override fun onProgress(id: String?, bytes: Long, total: Long) {}
                    override fun onReschedule(id: String?, error: com.cloudinary.android.callback.ErrorInfo?) {}
                }).dispatch()
        }
    }

    private fun saveToFirebase() {
        val user = auth.currentUser ?: return
        val point = marker!!.position
        val key = database.reference.child("lost_reports").push().key!!

        val data = mapOf(
            "id" to key,
            "animalName" to etAnimalName.text.toString(),
            "animalType" to spinnerAnimalType.selectedItem.toString(),
            "animalColor" to etAnimalColor.text.toString(),
            "description" to etDescription.text.toString(),
            "locationText" to tvLocation.text.toString(),
            "lat" to point.latitude,
            "lng" to point.longitude,
            "contact" to etContact.text.toString(),
            "photoUrl" to animalPhotoUrl,
            "mapImageUrl" to mapImageUrl,
            "ownerUid" to user.uid,
            "ownerEmail" to user.email,
            "createdAt" to System.currentTimeMillis()
        )

        database.reference.child("lost_reports").child(key).setValue(data)
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

    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { super.onPause(); mapView.onPause() }
    override fun onDestroy() { super.onDestroy(); mapView.onDetach() }
}
