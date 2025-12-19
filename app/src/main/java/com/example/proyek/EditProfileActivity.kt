package com.example.proyek

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class EditProfileActivity : AppCompatActivity() {

    private lateinit var imgAvatar: ImageView
    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        imgAvatar = findViewById(R.id.imgAvatarEdit)

        val etName = findViewById<EditText>(R.id.etName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val btnSave = findViewById<Button>(R.id.btnSave)

        // klik avatar → pilih foto
        imgAvatar.setOnClickListener {
            pickImage()
        }

        // SAVE → kirim data balik ke ProfileFragment
        btnSave.setOnClickListener {

            val name = etName.text.toString()
            val email = etEmail.text.toString()

            val resultIntent = Intent().apply {
                putExtra("NAME", name)
                putExtra("EMAIL", email)
                putExtra("AVATAR_URI", selectedImageUri?.toString())
            }

            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }

    private fun pickImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, 100)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 100 && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data
            imgAvatar.setImageURI(selectedImageUri)
        }
    }
}
