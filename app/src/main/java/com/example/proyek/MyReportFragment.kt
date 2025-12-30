package com.example.proyek

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MyReportFragment : Fragment() {

    private lateinit var rv: RecyclerView
    private lateinit var adapter: MyReportAdapter

    private val myReports = mutableListOf<MyReportItem>()

    private val db = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_my_report, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rv = view.findViewById(R.id.rvMyReport)
        rv.layoutManager = LinearLayoutManager(requireContext())

        adapter = MyReportAdapter(
            myReports,
            onEdit = { report ->
                goToEdit(report)
            },
            onDelete = { report ->
                confirmDelete(report)
            }
        )

        rv.adapter = adapter

        loadMyReports()
    }

    // ================= LOAD DATA =================

    private fun loadMyReports() {
        val uid = auth.currentUser?.uid ?: return

        myReports.clear()
        adapter.notifyDataSetChanged()

        loadLost(uid)
        loadFound(uid)
    }

    private fun loadLost(uid: String) {
        db.reference.child("lost_reports")
            .orderByChild("ownerUid")
            .equalTo(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.forEach { s ->
                        myReports.add(
                            MyReportItem(
                                id = s.key ?: "",
                                type = "LOST",
                                animalName = s.child("animalName").getValue(String::class.java),
                                animalType = s.child("animalType").getValue(String::class.java),
                                animalColor = s.child("animalColor").getValue(String::class.java),
                                photoUrl = s.child("photoUrl").getValue(String::class.java)
                            )
                        )
                    }
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun loadFound(uid: String) {
        db.reference.child("found_reports")
            .orderByChild("foundByUid")
            .equalTo(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.forEach { s ->
                        myReports.add(
                            MyReportItem(
                                id = s.key ?: "",
                                type = "FOUND",
                                animalName = s.child("animalName").getValue(String::class.java),
                                animalType = s.child("animalType").getValue(String::class.java),
                                animalColor = s.child("animalColor").getValue(String::class.java),
                                photoUrl = s.child("photoUrl").getValue(String::class.java)
                            )
                        )
                    }
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // ================= EDIT =================

    private fun goToEdit(report: MyReportItem) {
        val bundle = Bundle().apply {
            putBoolean("IS_EDIT", true)
            putString("REPORT_ID", report.id)
            putString("REPORT_TYPE", report.type)
        }

        if (report.type == "LOST") {
            findNavController().navigate(
                R.id.action_myReportFragment_to_reportLostFragment,
                bundle
            )
        } else {
            findNavController().navigate(
                R.id.action_myReportFragment_to_reportFoundFragment,
                bundle
            )
        }
    }

    // ================= DELETE =================

    private fun confirmDelete(report: MyReportItem) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Report")
            .setMessage("Laporan ini akan dihapus permanen")
            .setPositiveButton("Delete") { _, _ ->
                deleteReport(report)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteReport(report: MyReportItem) {
        val path = if (report.type == "LOST") "lost_reports" else "found_reports"

        db.reference.child(path)
            .child(report.id)
            .removeValue()
            .addOnSuccessListener {
                myReports.remove(report)
                adapter.notifyDataSetChanged()
            }
    }
}
