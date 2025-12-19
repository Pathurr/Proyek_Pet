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
import androidx.fragment.app.Fragment
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.auth.FirebaseAuth


class ProfileFragment : Fragment() {

    private val editProfileLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data ?: return@registerForActivityResult

                val name = data.getStringExtra("NAME")
                val email = data.getStringExtra("EMAIL")
                val avatarUri = data.getStringExtra("AVATAR_URI")

                view?.findViewById<TextView>(R.id.tvName)?.text = name
                view?.findViewById<TextView>(R.id.tvEmail)?.text = email

                if (avatarUri != null) {
                    view?.findViewById<ImageView>(R.id.imgAvatar)
                        ?.setImageURI(Uri.parse(avatarUri))
                }

                saveProfile(name, email, avatarUri)
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

        loadProfile(view)

        val menuEditProfile = view.findViewById<LinearLayout>(R.id.menuEditProfile)

        val menuLogout = view.findViewById<LinearLayout>(R.id.menuLogout)

        menuLogout.setOnClickListener {

            // 1. logout firebase
            FirebaseAuth.getInstance().signOut()

            // 2. hapus data profile lokal
            val pref = requireContext()
                .getSharedPreferences("PROFILE_PREF", Context.MODE_PRIVATE)
            pref.edit().clear().apply()

            // 3. balik ke MainActivity (page awal)
            val intent = Intent(requireContext(), MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        menuEditProfile.setOnClickListener {
            val intent = Intent(requireContext(), EditProfileActivity::class.java)
            editProfileLauncher.launch(intent) // ✅ PENTING
        }
    }

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

        val name = pref.getString("NAME", "Your Name")
        val email = pref.getString("EMAIL", "your@email.com")
        val avatar = pref.getString("AVATAR", null)

        view.findViewById<TextView>(R.id.tvName).text = name
        view.findViewById<TextView>(R.id.tvEmail).text = email

        if (avatar != null) {
            view.findViewById<ImageView>(R.id.imgAvatar)
                .setImageURI(Uri.parse(avatar))
        }
    }
}
