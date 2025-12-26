package com.example.proyek

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
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
import com.yalantis.ucrop.UCrop
import java.io.File
import java.util.*

class EditProfileActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        const val PICK_IMAGE_REQUEST = 100
    }

    // ================= IMAGE =================
    private lateinit var imgAvatar: ImageView
    private var originalImageUri: Uri? = null
    private var avatarCloudUrl: String? = null
    private var isUploading = false

    // ================= MAP =================
    private lateinit var googleMap: GoogleMap
    private var marker: Marker? = null
    private var selectedLat: Double = -6.2
    private var selectedLng: Double = 106.816666
    private var mapImageUrl: String? = null

    // ================= FORM =================
    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etLocation: EditText
    private lateinit var etFullAddress: EditText
    private lateinit var scrollView: ScrollView
    private lateinit var mapContainer: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        // Cloudinary init
        try {
            MediaManager.get()
        } catch (e: IllegalStateException) {
            MediaManager.init(this, mapOf("cloud_name" to "dwbkytgil"))
        }

        // View
        imgAvatar = findViewById(R.id.imgAvatarEdit)
        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etPhone = findViewById(R.id.etPhone)
        etLocation = findViewById(R.id.etLocation)
        etFullAddress = findViewById(R.id.etFUllAddress)
        scrollView = findViewById(R.id.scrollView)
        mapContainer = findViewById(R.id.mapContainer)

        imgAvatar.setOnClickListener { pickImage() }
        findViewById<Button>(R.id.btnSave).setOnClickListener { saveProfile() }

        // Setup Google Map
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // ScrollView touch handling
        mapContainer.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> scrollView.requestDisallowInterceptTouchEvent(true)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> scrollView.requestDisallowInterceptTouchEvent(false)
            }
            false
        }

        loadFromLocal()
        fetchFromFirebase()
    }

    // ================= MAP =================
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        val start = LatLng(selectedLat, selectedLng)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(start, 15f))

        marker = googleMap.addMarker(
            MarkerOptions().position(start).draggable(true)
        )

        googleMap.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
            override fun onMarkerDragEnd(m: Marker) {
                selectedLat = m.position.latitude
                selectedLng = m.position.longitude
                updateAddress(m.position)
            }
            override fun onMarkerDrag(m: Marker) {}
            override fun onMarkerDragStart(m: Marker) {}
        })
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isMapToolbarEnabled = false
    }

    private fun updateAddress(latLng: LatLng) {
        try {
            val geo = android.location.Geocoder(this, Locale.getDefault())
            val list = geo.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (!list.isNullOrEmpty()) {
                etFullAddress.setText(list[0].getAddressLine(0))
            }
        } catch (_: Exception) {}
    }

    // ================= IMAGE =================
    private fun pickImage() {
        startActivityForResult(
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "image/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }, PICK_IMAGE_REQUEST
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK || data == null) return

        when (requestCode) {
            PICK_IMAGE_REQUEST -> {
                originalImageUri = data.data
                originalImageUri?.let { startCrop(it) }
            }
            UCrop.REQUEST_CROP -> {
                val uri = UCrop.getOutput(data) ?: return
                imgAvatar.setImageURI(uri)
                originalImageUri?.let { uploadAvatarToCloudinary(it) }
            }
        }
    }

    private fun startCrop(uri: Uri) {
        val options = UCrop.Options().apply {
            setCircleDimmedLayer(true)
            setShowCropFrame(false)
            setShowCropGrid(false)
        }
        UCrop.of(uri, Uri.fromFile(File(cacheDir, "crop_${System.currentTimeMillis()}.jpg")))
            .withAspectRatio(1f, 1f)
            .withOptions(options)
            .start(this)
    }

    private fun uploadAvatarToCloudinary(uri: Uri) {
        isUploading = true
        MediaManager.get().upload(uri)
            .unsigned("android_unsigned")
            .option("folder", "avatars")
            .callback(object : UploadCallback {
                override fun onSuccess(id: String?, data: Map<*, *>) {
                    avatarCloudUrl = data["secure_url"] as String
                    isUploading = false
                }
                override fun onError(id: String?, error: com.cloudinary.android.callback.ErrorInfo?) {
                    isUploading = false
                }
                override fun onStart(id: String?) {}
                override fun onProgress(id: String?, b: Long, t: Long) {}
                override fun onReschedule(id: String?, e: com.cloudinary.android.callback.ErrorInfo?) {}
            }).dispatch()
    }

    // ================= MAP → IMAGE =================
    private fun captureMapBitmap(onResult: (File) -> Unit) {
        if (!::googleMap.isInitialized) return

        googleMap.snapshot { bitmap ->
            bitmap?.let { bmp ->
                val file = File(cacheDir, "map_${System.currentTimeMillis()}.jpg")
                file.outputStream().use { out ->
                    bmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                onResult(file)
            } ?: run {
                Toast.makeText(this, "Gagal mengambil snapshot map", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uploadMapToCloudinary(file: File, onDone: () -> Unit) {
        isUploading = true
        MediaManager.get().upload(Uri.fromFile(file))
            .unsigned("android_unsigned")
            .option("folder", "user_maps")
            .callback(object : UploadCallback {
                override fun onSuccess(id: String?, data: Map<*, *>) {
                    mapImageUrl = data["secure_url"] as String
                    isUploading = false
                    onDone()
                }
                override fun onError(id: String?, error: com.cloudinary.android.callback.ErrorInfo?) {
                    isUploading = false
                }
                override fun onStart(id: String?) {}
                override fun onProgress(id: String?, b: Long, t: Long) {}
                override fun onReschedule(id: String?, e: com.cloudinary.android.callback.ErrorInfo?) {}
            }).dispatch()
    }

    // ================= SAVE =================
    private fun saveProfile() {
        if (isUploading) {
            Toast.makeText(this, "Tunggu upload selesai", Toast.LENGTH_SHORT).show()
            return
        }

        captureMapBitmap { file ->
            uploadMapToCloudinary(file) {
                saveProfileData()
            }
        }
    }

    private fun saveProfileData() {
        val uid = FirebaseAuth.getInstance().currentUser!!.uid

        val data = mapOf(
            "name" to etName.text.toString(),
            "email" to etEmail.text.toString(),
            "phone" to etPhone.text.toString(),
            "location" to etLocation.text.toString(),
            "fullAddress" to etFullAddress.text.toString(),
            "avatar" to avatarCloudUrl,
            "location_map" to mapImageUrl,
            "lat" to selectedLat,
            "lng" to selectedLng
        )

        FirebaseDatabase.getInstance()
            .getReference("users")
            .child(uid)
            .updateChildren(data)
            .addOnSuccessListener {
                saveToLocal()
                finish()
            }
    }

    // ================= LOCAL =================
    private fun saveToLocal() {
        getSharedPreferences("PROFILE_PREF", MODE_PRIVATE).edit()
            .putString("NAME", etName.text.toString())
            .putString("EMAIL", etEmail.text.toString())
            .putString("PHONE", etPhone.text.toString())
            .putString("LOCATION", etLocation.text.toString())
            .putString("FULL_ADDRESS", etFullAddress.text.toString())
            .putString("AVATAR", avatarCloudUrl)
            .putString("MAP_IMAGE", mapImageUrl)
            .putFloat("LAT", selectedLat.toFloat())
            .putFloat("LNG", selectedLng.toFloat())
            .apply()
    }

    private fun loadFromLocal() {
        val p = getSharedPreferences("PROFILE_PREF", MODE_PRIVATE)
        etName.setText(p.getString("NAME", ""))
        etEmail.setText(p.getString("EMAIL", ""))
        etPhone.setText(p.getString("PHONE", ""))
        etLocation.setText(p.getString("LOCATION", ""))
        etFullAddress.setText(p.getString("FULL_ADDRESS", ""))
        avatarCloudUrl = p.getString("AVATAR", null)
        mapImageUrl = p.getString("MAP_IMAGE", null)
        selectedLat = p.getFloat("LAT", -6.2f).toDouble()
        selectedLng = p.getFloat("LNG", 106.816666f).toDouble()

        avatarCloudUrl?.let { Glide.with(this).load(it).circleCrop().into(imgAvatar) }
    }

    private fun fetchFromFirebase() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseDatabase.getInstance()
            .getReference("users")
            .child(uid)
            .get()
            .addOnSuccessListener {
                if (!it.exists()) return@addOnSuccessListener
                etName.setText(it.child("name").value?.toString())
                etEmail.setText(it.child("email").value?.toString())
                etPhone.setText(it.child("phone").value?.toString())
                etLocation.setText(it.child("location").value?.toString())
                etFullAddress.setText(it.child("fullAddress").value?.toString())
                avatarCloudUrl = it.child("avatar").value?.toString()
                mapImageUrl = it.child("location_map").value?.toString()
                selectedLat = it.child("lat").getValue(Double::class.java) ?: -6.2
                selectedLng = it.child("lng").getValue(Double::class.java) ?: 106.816666

                updateMarkerPosition()
            }
    }

    private fun updateMarkerPosition() {
        val point = LatLng(selectedLat, selectedLng)
        if (::googleMap.isInitialized) {
            if (marker == null) {
                marker = googleMap.addMarker(MarkerOptions().position(point).draggable(true))
            } else {
                marker!!.position = point
            }
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(point, 15f))
        }
    }
}
