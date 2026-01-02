package com.example.proyek

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class SearchFragment : Fragment() {

    private lateinit var etSearch: EditText
    private lateinit var rvSearch: RecyclerView
    private lateinit var adapter: PetReportAdapter

    private val allReports = mutableListOf<PetReport>()
    private val database = FirebaseDatabase.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_search, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etSearch = view.findViewById(R.id.etSearch)
        rvSearch = view.findViewById(R.id.rvSearch)

        // Recycler
        rvSearch.layoutManager = LinearLayoutManager(requireContext())
        adapter = PetReportAdapter(mutableListOf()) { report ->
            val bundle = Bundle().apply {
                putString("reportId", report.id)
                putString("reportType", report.type.name)
            }
            findNavController().navigate(
                R.id.action_searchFragment_to_reportDetailFragment,
                bundle
            )
        }
        rvSearch.adapter = adapter

        loadAllReports()

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applySearch(s.toString())
            }
        })
    }

    // ================= FIREBASE =================
    private fun loadAllReports() {
        allReports.clear()
        loadLostReports()
        loadFoundReports()
    }

    private fun loadLostReports() {
        database.reference.child("lost_reports")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.forEach {
                        allReports.add(
                            PetReport(
                                id = it.key ?: "",
                                type = ReportType.LOST,
                                animalName = it.child("animalName").getValue(String::class.java),
                                animalType = it.child("animalType").getValue(String::class.java) ?: "",
                                animalColor = it.child("animalColor").getValue(String::class.java) ?: "",
                                ownerEmail = it.child("ownerEmail").getValue(String::class.java),
                                photoUrl = it.child("photoUrl").getValue(String::class.java)
                            )
                        )
                    }
                    adapter.updateData(allReports)
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun loadFoundReports() {
        database.reference.child("found_reports")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.forEach {
                        allReports.add(
                            PetReport(
                                id = it.key ?: "",
                                type = ReportType.FOUND,
                                animalName = it.child("animalName").getValue(String::class.java),
                                animalType = it.child("animalType").getValue(String::class.java) ?: "",
                                animalColor = it.child("animalColor").getValue(String::class.java) ?: "",
                                ownerEmail = null,
                                photoUrl = it.child("photoUrl").getValue(String::class.java)
                            )
                        )
                    }
                    adapter.updateData(allReports)
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // ================= SEARCH =================
    private fun applySearch(keyword: String) {
        val key = keyword.lowercase()

        val filtered = allReports.filter {
            it.animalName?.lowercase()?.contains(key) == true ||
                    it.animalType.lowercase().contains(key)
        }

        adapter.updateData(filtered)
    }
}
