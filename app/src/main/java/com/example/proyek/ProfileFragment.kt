package com.example.proyek

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.bumptech.glide.Glide


class ProfileFragment : Fragment() {

    // launcher edit profile
    private val editProfileLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data ?: return@registerForActivityResult

                val name = data.getStringExtra("NAME")
                val email = data.getStringExtra("EMAIL")
                val avatar = data.getStringExtra("AVATAR_URI")

                view?.findViewById<TextView>(R.id.tvName)?.text = name
                view?.findViewById<TextView>(R.id.tvEmail)?.text = email

                if (avatar != null && isAdded) {
                    Glide.with(requireContext())
                        .load(avatar)
                        .circleCrop()
                        .into(requireView().findViewById(R.id.imgAvatar))
                }
                // simpan lokal
                saveProfile(name, email, avatar)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // tampilkan profile
        loadProfile(view)

        val menuEditProfile = view.findViewById<LinearLayout>(R.id.menuEditProfile)
        val menuLogout = view.findViewById<LinearLayout>(R.id.menuLogout)

        menuEditProfile.setOnClickListener {
            val intent = Intent(requireContext(), EditProfileActivity::class.java)
            editProfileLauncher.launch(intent)
        }

        menuLogout.setOnClickListener {

            // logout firebase
            FirebaseAuth.getInstance().signOut()

            // hapus data lokal
            val pref = requireContext()
                .getSharedPreferences("PROFILE_PREF", Context.MODE_PRIVATE)
            pref.edit().clear().apply()

            // balik ke halaman awal
            val intent = Intent(requireContext(), MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    // ===============================
    // LOCAL STORAGE
    // ===============================

    private fun saveProfile(name: String?, email: String?, avatar: String?) {
        val pref = requireContext()
            .getSharedPreferences("PROFILE_PREF", Context.MODE_PRIVATE)

        pref.edit()
            .putString("NAME", name)
            .putString("EMAIL", email)
            .putString("AVATAR", avatar)
            .apply()
    }

    private fun loadProfile(view: View) {
        val pref = requireContext()
            .getSharedPreferences("PROFILE_PREF", Context.MODE_PRIVATE)

        val name = pref.getString("NAME", null)
        val email = pref.getString("EMAIL", null)
        val avatar = pref.getString("AVATAR", null)

        // 1️⃣ kalau lokal ada → pakai lokal
        if (!name.isNullOrEmpty() && !email.isNullOrEmpty()) {

            view.findViewById<TextView>(R.id.tvName).text = name
            view.findViewById<TextView>(R.id.tvEmail).text = email

            if (!avatar.isNullOrEmpty()) {
                Glide.with(view)
                    .load(avatar)
                    .circleCrop()
                    .into(view.findViewById(R.id.imgAvatar))
            }
            return
        }
        // 2️⃣ kalau belum ada → fetch dari firebase
        fetchProfileFromFirebase(view)
    }

    // ===============================
    // FIREBASE
    // ===============================

    private fun fetchProfileFromFirebase(view: View) {

        val user = FirebaseAuth.getInstance().currentUser ?: return
        val uid = user.uid

        FirebaseDatabase.getInstance()
            .getReference("users")
            .child(uid)
            .get()
            .addOnSuccessListener { snapshot ->

                if (!snapshot.exists()) return@addOnSuccessListener

                val name = snapshot.child("name").value?.toString()
                val email = snapshot.child("email").value?.toString()
                val avatar = snapshot.child("avatar").value?.toString()

                view.findViewById<TextView>(R.id.tvName).text = name
                view.findViewById<TextView>(R.id.tvEmail).text = email

                if (!avatar.isNullOrEmpty()) {
                    Glide.with(view)
                        .load(avatar)
                        .circleCrop()
                        .into(view.findViewById(R.id.imgAvatar))
                }
                // simpan ke lokal supaya next buka cepat
                saveProfile(name, email, avatar)
            }
    }
}
