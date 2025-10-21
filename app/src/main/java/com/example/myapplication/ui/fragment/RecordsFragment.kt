package com.example.myapplication.ui.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.data.local.PreferencesManager
import com.example.myapplication.ui.adapter.RecordsAdapter
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class RecordsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RecordsAdapter

    private val preferencesManager: PreferencesManager by inject()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_records, container, false)

        initViews(view)
        setupRecyclerView()
        loadRecords()

        return view
    }

    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.recyclerViewRecords)
    }

    private fun setupRecyclerView() {
        adapter = RecordsAdapter()
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun loadRecords() {
        lifecycleScope.launch {
            preferencesManager.getTopScores().collect { records ->
                Log.d("RecordsFragment", "Top records loaded: ${records.size}")
                records.forEachIndexed { index, record ->
                    Log.d("RecordsFragment", "Record ${index + 1}: ${record.score} by ${record.userName}")
                }
                adapter.updateRecords(records)
            }
        }
    }
}