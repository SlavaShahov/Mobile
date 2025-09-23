package com.example.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.fragment.app.Fragment

class AuthorsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_authors, container, false)

        val listView: ListView = view.findViewById(R.id.listViewAuthors)
        val authors = listOf(
            Author("Шахов Вячеслав", R.drawable.avatar1),
        )

        val adapter = AuthorAdapter(requireContext(), authors)
        listView.adapter = adapter

        return view
    }
}

data class Author(val name: String, val avatarRes: Int)