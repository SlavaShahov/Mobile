package com.example.myapplication.ui.fragment

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import com.example.myapplication.domain.model.Player
import com.example.myapplication.domain.usecase.CalculateZodiacUseCase
import com.example.myapplication.data.local.PreferencesManager
import java.text.SimpleDateFormat
import java.util.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class RegistrationFragment : Fragment() {

    private lateinit var etFullName: EditText
    private lateinit var etBirthDate: EditText
    private lateinit var btnSelectDate: Button
    private lateinit var rgGender: RadioGroup
    private lateinit var spCourse: Spinner
    private lateinit var sbDifficulty: SeekBar
    private lateinit var tvDifficultyValue: TextView
    private lateinit var ivZodiacSign: ImageView
    private lateinit var btnRegister: Button
    private lateinit var tvResult: TextView

    private val calendar = Calendar.getInstance()
    private var selectedBirthDate: Long = 0

    private val calculateZodiacUseCase = CalculateZodiacUseCase()
    private lateinit var preferencesManager: PreferencesManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_registration, container, false)

        preferencesManager = PreferencesManager(requireContext())
        initViews(view)
        setupCourseSpinner()
        setupDifficultySeekBar()
        setupDatePicker()
        setupRegisterButton()
        loadExistingPlayer()

        return view
    }

    private fun initViews(view: View) {
        etFullName = view.findViewById(R.id.etFullName)
        etBirthDate = view.findViewById(R.id.etBirthDate)
        btnSelectDate = view.findViewById(R.id.btnSelectDate)
        rgGender = view.findViewById(R.id.rgGender)
        spCourse = view.findViewById(R.id.spCourse)
        sbDifficulty = view.findViewById(R.id.sbDifficulty)
        tvDifficultyValue = view.findViewById(R.id.tvDifficultyValue)
        ivZodiacSign = view.findViewById(R.id.ivZodiacSign)
        btnRegister = view.findViewById(R.id.btnRegister)
        tvResult = view.findViewById(R.id.tvResult)
    }

    private fun loadExistingPlayer() {
        lifecycleScope.launch {
            try {
                val player = withContext(Dispatchers.IO) {
                    preferencesManager.getPlayer()
                }
                player?.let {
                    withContext(Dispatchers.Main) {
                        etFullName.setText(it.fullName)
                        // Загружаем остальные данные игрока...
                    }
                }
            } catch (e: Exception) {
                // Игнорируем ошибки при загрузке существующего игрока
            }
        }
    }

    private fun setupDatePicker() {
        selectedBirthDate = calendar.timeInMillis
        updateDateDisplay()

        btnSelectDate.setOnClickListener {
            showDatePickerDialog()
        }

        etBirthDate.setOnClickListener {
            showDatePickerDialog()
        }
    }

    private fun showDatePickerDialog() {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(selectedYear, selectedMonth, selectedDay)
                selectedBirthDate = calendar.timeInMillis
                updateDateDisplay()
                updateZodiacSign()
            },
            year,
            month,
            day
        )

        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        val minCalendar = Calendar.getInstance()
        minCalendar.add(Calendar.YEAR, -100)
        datePickerDialog.datePicker.minDate = minCalendar.timeInMillis

        datePickerDialog.show()
    }

    private fun updateDateDisplay() {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        etBirthDate.setText(dateFormat.format(calendar.time))
    }

    private fun updateZodiacSign() {
        val zodiacSign = calculateZodiacUseCase.execute(selectedBirthDate)
        val zodiacImageRes = getZodiacImageResource(zodiacSign)
        ivZodiacSign.setImageResource(zodiacImageRes)
    }

    private fun setupCourseSpinner() {
        val courses = arrayOf("Выберите курс", "1 курс", "2 курс", "3 курс", "4 курс", "5 курс", "6 курс")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, courses)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCourse.adapter = adapter
        spCourse.setSelection(0)
    }

    private fun setupDifficultySeekBar() {
        sbDifficulty.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val level = when (progress) {
                    0 -> "Очень легкий"
                    in 1..3 -> "Легкий"
                    in 4..6 -> "Средний"
                    in 7..9 -> "Сложный"
                    10 -> "Очень сложный"
                    else -> "Средний"
                }
                tvDifficultyValue.text = "$level ($progress)"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        tvDifficultyValue.text = "Средний (${sbDifficulty.progress})"
    }

    private fun setupRegisterButton() {
        btnRegister.setOnClickListener {
            if (validateInput()) {
                registerPlayer()
            }
        }
    }

    private fun validateInput(): Boolean {
        val fullName = etFullName.text.toString().trim()
        if (fullName.isEmpty()) {
            etFullName.error = "Введите ФИО"
            etFullName.requestFocus()
            return false
        }

        if (fullName.split("\\s+".toRegex()).size < 2) {
            etFullName.error = "Введите как минимум имя и фамилию"
            etFullName.requestFocus()
            return false
        }

        if (rgGender.checkedRadioButtonId == -1) {
            Toast.makeText(requireContext(), "Выберите пол", Toast.LENGTH_SHORT).show()
            return false
        }

        if (spCourse.selectedItemPosition == 0) {
            Toast.makeText(requireContext(), "Выберите курс", Toast.LENGTH_SHORT).show()
            return false
        }

        if (selectedBirthDate == 0L) {
            Toast.makeText(requireContext(), "Выберите дату рождения", Toast.LENGTH_SHORT).show()
            return false
        }

        if (selectedBirthDate > System.currentTimeMillis()) {
            Toast.makeText(requireContext(), "Дата рождения не может быть в будущем", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun registerPlayer() {
        val fullName = etFullName.text.toString().trim()
        val gender = when (rgGender.checkedRadioButtonId) {
            R.id.rbMale -> "Мужской"
            R.id.rbFemale -> "Женский"
            else -> ""
        }
        val course = spCourse.selectedItem.toString()
        val difficulty = sbDifficulty.progress
        val birthDate = selectedBirthDate
        val zodiacSign = calculateZodiacUseCase.execute(birthDate)
        val zodiacImageRes = getZodiacImageResource(zodiacSign)

        val player = Player(fullName, gender, course, difficulty, birthDate, zodiacSign)

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    preferencesManager.savePlayer(player)
                }
                withContext(Dispatchers.Main) {
                    displayPlayerInfo(player)
                    ivZodiacSign.setImageResource(zodiacImageRes)
                    Toast.makeText(requireContext(), "Игрок зарегистрирован!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Ошибка регистрации: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getZodiacImageResource(zodiacSign: String): Int {
        return when (zodiacSign) {
            "Овен" -> R.drawable.aries
            "Телец" -> R.drawable.taurus
            "Близнецы" -> R.drawable.gemini
            "Рак" -> R.drawable.cancer
            "Лев" -> R.drawable.leo
            "Дева" -> R.drawable.virgo
            "Весы" -> R.drawable.libra
            "Скорпион" -> R.drawable.scorpio
            "Стрелец" -> R.drawable.sagittarius
            "Козерог" -> R.drawable.capricorn
            "Водолей" -> R.drawable.aquarius
            "Рыбы" -> R.drawable.pisces
            else -> android.R.drawable.ic_menu_help
        }
    }

    private fun displayPlayerInfo(player: Player) {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = player.birthDate
        val dateFormatted = "${calendar.get(Calendar.DAY_OF_MONTH)}." +
                "${calendar.get(Calendar.MONTH) + 1}.${calendar.get(Calendar.YEAR)}"

        val resultText = """
            Регистрация завершена!
            
            ФИО: ${player.fullName}
            Пол: ${player.gender}
            Курс: ${player.course}
            Уровень сложности: ${player.difficulty}/10
            Дата рождения: $dateFormatted
            Знак зодиака: ${player.zodiacSign}
        """.trimIndent()

        tvResult.text = resultText
        tvResult.visibility = View.VISIBLE
    }
}