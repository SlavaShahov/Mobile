package com.example.myapplication.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.example.myapplication.R
import com.example.myapplication.domain.model.Author

class AuthorAdapter(context: Context, private val authors: List<Author>) :
    ArrayAdapter<Author>(context, R.layout.item_author, authors) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        val holder: ViewHolder

        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.item_author, parent, false)
            holder = ViewHolder()
            holder.avatar = view.findViewById(R.id.ivAvatar)
            holder.name = view.findViewById(R.id.tvName)
            view.tag = holder
        } else {
            holder = view.tag as ViewHolder
        }

        val author = authors[position]
        holder.avatar?.setImageResource(author.avatarRes)
        holder.name?.text = author.name

        return view!!
    }

    private class ViewHolder {
        var avatar: ImageView? = null
        var name: TextView? = null
    }
}