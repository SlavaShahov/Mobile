package com.example.myapplication

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.*

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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_registration, container, false)

        initViews(view)
        setupCourseSpinner()
        setupDifficultySeekBar()
        setupDatePicker()
        setupRegisterButton()

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

    private fun setupDatePicker() {
        // Устанавливаем текущую дату по умолчанию
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

                // Автоматически обновляем знак зодиака при выборе даты
                updateZodiacSign()
            },
            year,
            month,
            day
        )

        // Устанавливаем максимальную дату - сегодня
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()

        // Устанавливаем минимальную дату - 100 лет назад
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
        val zodiacSign = calculateZodiacSign(selectedBirthDate)
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
        // сразу показать начальное значение
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

        // простая проверка на фамилию + имя (минимум 2 слова)
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

        // дата не может быть в будущем
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

        val zodiacSign = calculateZodiacSign(birthDate)
        val zodiacImageRes = getZodiacImageResource(zodiacSign)

        val player = Player(fullName, gender, course, difficulty, birthDate, zodiacSign)

        // Отображаем результат
        displayPlayerInfo(player)
        ivZodiacSign.setImageResource(zodiacImageRes)
    }

    private fun calculateZodiacSign(birthDate: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = birthDate

        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.get(Calendar.MONTH) + 1

        return when (month) {
            1 -> if (day <= 20) "Козерог" else "Водолей"
            2 -> if (day <= 18) "Водолей" else "Рыбы"
            3 -> if (day <= 20) "Рыбы" else "Овен"
            4 -> if (day <= 20) "Овен" else "Телец"
            5 -> if (day <= 21) "Телец" else "Близнецы"
            6 -> if (day <= 21) "Близнецы" else "Рак"
            7 -> if (day <= 22) "Рак" else "Лев"
            8 -> if (day <= 23) "Лев" else "Дева"
            9 -> if (day <= 23) "Дева" else "Весы"
            10 -> if (day <= 23) "Весы" else "Скорпион"
            11 -> if (day <= 22) "Скорпион" else "Стрелец"
            12 -> if (day <= 21) "Стрелец" else "Козерог"
            else -> "Неизвестно"
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