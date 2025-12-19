package com.example.proyek

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)
        val tvForgot = findViewById<TextView>(R.id.tvForgot)

        auth = FirebaseAuth.getInstance()

        if (intent.getBooleanExtra("SIGNUP_SUCCESS", false)) {
            Toast.makeText(
                this,
                "Akun berhasil dibuat, silakan login",
                Toast.LENGTH_SHORT
            ).show()
        }

        btnLogin.setOnClickListener {

            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()

            // validasi input
            when {
                email.isEmpty() -> {
                    etEmail.error = "Email tidak boleh kosong"
                    etEmail.requestFocus()
                    return@setOnClickListener
                }
                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    etEmail.error = "Email tidak valid"
                    etEmail.requestFocus()
                    return@setOnClickListener
                }
                password.isEmpty() -> {
                    etPassword.error = "Password tidak boleh kosong"
                    etPassword.requestFocus()
                    return@setOnClickListener
                }
            }

            btnLogin.isEnabled = false

            // firebase login
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    btnLogin.isEnabled = true

                    Toast.makeText(
                        this,
                        "Login berhasil",
                        Toast.LENGTH_SHORT
                    ).show()

                    // pindah ke halaman home
                    val intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                    finish() // supaya tidak bisa balik ke login
                }
                .addOnFailureListener {
                    btnLogin.isEnabled = true
                    Toast.makeText(
                        this,
                        "Email atau password salah",
                        Toast.LENGTH_SHORT
                    ).show()
                }

        }

        // lupa password
        tvForgot.setOnClickListener {
            startActivity(Intent(this, LupaPasswordActivity::class.java))
        }
    }
}
