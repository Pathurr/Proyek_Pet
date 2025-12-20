package com.example.proyek

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.cloudinary.android.MediaManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.yalantis.ucrop.UCrop
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.util.*

class EditProfileActivity : AppCompatActivity() {

    // ================= IMAGE =================
    private lateinit var imgAvatar: ImageView
    private var originalImageUri: Uri? = null
    private var avatarCloudUrl: String? = null
    private var isUploading = false

    // ================= MAP =================
    private lateinit var mapView: MapView
    private var marker: Marker? = null
    private var selectedLat: Double? = null
    private var selectedLng: Double? = null
    private var mapImageUrl: String? = null

    // ================= FORM =================
    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etLocation: EditText
    private lateinit var etFullAddress: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        // OSM
        Configuration.getInstance().load(
            this,
            getSharedPreferences("osm_pref", Context.MODE_PRIVATE)
        )

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
        mapView = findViewById(R.id.mapView)

        imgAvatar.setOnClickListener { pickImage() }
        findViewById<Button>(R.id.btnSave).setOnClickListener { saveProfile() }

        setupMap()
        loadFromLocal()
        fetchFromFirebase()
    }

    // ================= MAP =================
    private fun setupMap() {
        mapView.setMultiTouchControls(true)
        val start = GeoPoint(selectedLat ?: -6.2, selectedLng ?: 106.816666)
        mapView.controller.setZoom(15.0)
        mapView.controller.setCenter(start)

        marker = Marker(mapView).apply {
            position = start
            isDraggable = true
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                override fun onMarkerDragEnd(marker: Marker) {
                    updateAddress(marker.position)
                }
                override fun onMarkerDrag(marker: Marker) {}
                override fun onMarkerDragStart(marker: Marker) {}
            })
        }
        mapView.overlays.add(marker)
        // Set initial lat/lng
        selectedLat = start.latitude
        selectedLng = start.longitude
    }

    private fun updateAddress(point: GeoPoint) {
        selectedLat = point.latitude
        selectedLng = point.longitude
        try {
            val geo = Geocoder(this, Locale.getDefault())
            val list = geo.getFromLocation(point.latitude, point.longitude, 1)
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
            }, 100
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK || data == null) return

        when (requestCode) {
            100 -> {
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
            .callback(object : com.cloudinary.android.callback.UploadCallback {
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
        mapView.isDrawingCacheEnabled = true
        val bitmap = Bitmap.createBitmap(mapView.drawingCache)
        mapView.isDrawingCacheEnabled = false

        val file = File(cacheDir, "map_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        onResult(file)
    }

    private fun uploadMapToCloudinary(file: File, onDone: () -> Unit) {
        isUploading = true
        MediaManager.get().upload(Uri.fromFile(file))
            .unsigned("android_unsigned")
            .option("folder", "user_maps")
            .callback(object : com.cloudinary.android.callback.UploadCallback {
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
            .putFloat("LAT", selectedLat?.toFloat() ?: -6.2f)
            .putFloat("LNG", selectedLng?.toFloat() ?: 106.816666f)
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
                selectedLat = it.child("lat").getValue(Double::class.java)
                selectedLng = it.child("lng").getValue(Double::class.java)
                updateMarkerPosition()
            }
    }
    private fun updateMarkerPosition() {
        val lat = selectedLat ?: -6.2
        val lng = selectedLng ?: 106.816666
        val point = GeoPoint(lat, lng)

        if (marker == null) {
            marker = Marker(mapView)
            marker!!.isDraggable = true
            marker!!.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker!!.setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                override fun onMarkerDragEnd(marker: Marker) { updateAddress(marker.position) }
                override fun onMarkerDrag(marker: Marker) {}
                override fun onMarkerDragStart(marker: Marker) {}
            })
            mapView.overlays.add(marker)
        }
        marker!!.position = point
        mapView.controller.setCenter(point)
        mapView.controller.setZoom(15.0)
    }

    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { super.onPause(); mapView.onPause() }
    override fun onDestroy() { super.onDestroy(); mapView.onDetach() }
}
