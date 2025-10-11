package com.example.myapplication.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.data.local.entity.ScoreRecord
import java.text.SimpleDateFormat
import java.util.*

class RecordsAdapter : ListAdapter<ScoreRecord, RecordsAdapter.RecordViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_record, parent, false)
        return RecordViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        val record = getItem(position)
        holder.bind(record, position + 1)
    }

    class RecordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvPosition: TextView = itemView.findViewById(R.id.tvPosition)
        private val tvScore: TextView = itemView.findViewById(R.id.tvScore)
        private val tvSettings: TextView = itemView.findViewById(R.id.tvSettings)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvUserName: TextView = itemView.findViewById(R.id.tvUserName) // Добавь этот TextView

        fun bind(record: ScoreRecord, position: Int) {
            tvPosition.text = "#$position"
            tvScore.text = "${record.score} очков"

            // Показываем имя пользователя
            tvUserName.text = record.userName

            // Показываем все настройки
            tvSettings.text = "Скорость: ${record.gameSpeed}x | " +
                    "Тараканы: ${record.maxCockroaches} | " +
                    "Бонусы: ${record.bonusInterval}сек | " +
                    "Время: ${record.roundDuration}сек"

            val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            tvDate.text = dateFormat.format(Date(record.timestamp))
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<ScoreRecord>() {
        override fun areItemsTheSame(oldItem: ScoreRecord, newItem: ScoreRecord): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ScoreRecord, newItem: ScoreRecord): Boolean {
            return oldItem == newItem
        }
    }
}