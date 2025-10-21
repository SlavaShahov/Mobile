package com.example.myapplication.ui.activity

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.R
import com.example.myapplication.data.local.PreferencesManager
import com.example.myapplication.domain.model.Player
import com.example.myapplication.domain.usecase.CalculateZodiacUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import java.text.SimpleDateFormat
import java.util.*

class LoginActivity : AppCompatActivity() {

    private lateinit var btnLogin: Button
    private lateinit var btnRegister: Button
    private lateinit var etUsername: EditText
    private lateinit var etLoginPassword: EditText
    private lateinit var layoutLogin: LinearLayout
    private lateinit var layoutRegister: LinearLayout
    private lateinit var btnBackToLogin: Button
    private lateinit var btnConfirmRegister: Button

    // Registration fields
    private lateinit var etRegFullName: EditText
    private lateinit var etBirthDate: EditText
    private lateinit var btnSelectDate: Button
    private lateinit var rgGender: RadioGroup
    private lateinit var spCourse: Spinner
    private lateinit var sbDifficulty: SeekBar
    private lateinit var tvDifficultyValue: TextView
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText

    private val calendar = Calendar.getInstance()
    private var selectedBirthDate: Long = 0

    private val preferencesManager: PreferencesManager by inject()
    private val calculateZodiacUseCase = CalculateZodiacUseCase()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        initViews()
        setupLoginScreen()
    }

    private fun initViews() {
        btnLogin = findViewById(R.id.btnLogin)
        btnRegister = findViewById(R.id.btnRegister)
        etUsername = findViewById(R.id.etUsername)
        etLoginPassword = findViewById(R.id.etLoginPassword)
        layoutLogin = findViewById(R.id.layoutLogin)
        layoutRegister = findViewById(R.id.layoutRegister)
        btnBackToLogin = findViewById(R.id.btnBackToLogin)
        btnConfirmRegister = findViewById(R.id.btnConfirmRegister)

        etRegFullName = findViewById(R.id.etRegFullName)
        etBirthDate = findViewById(R.id.etBirthDate)
        btnSelectDate = findViewById(R.id.btnSelectDate)
        rgGender = findViewById(R.id.rgGender)
        spCourse = findViewById(R.id.spCourse)
        sbDifficulty = findViewById(R.id.sbDifficulty)
        tvDifficultyValue = findViewById(R.id.tvDifficultyValue)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)

        Log.d("LoginActivity", "btnConfirmRegister: $btnConfirmRegister")
        Log.d("LoginActivity", "btnRegister: $btnRegister")
    }

    private fun setupLoginScreen() {
        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etLoginPassword.text.toString()

            if (username.isEmpty()) {
                etUsername.error = "Введите имя пользователя"
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                etLoginPassword.error = "Введите пароль"
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    val success = withContext(Dispatchers.IO) {
                        preferencesManager.loginUser(username, password)
                    }
                    if (success) {
                        startMainActivity()
                    } else {
                        Toast.makeText(
                            this@LoginActivity,
                            "Неверное имя пользователя или пароль",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        this@LoginActivity,
                        "Ошибка входа: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        btnRegister.setOnClickListener {
            Log.d("LoginActivity", "Register button clicked")
            showRegistrationScreen()
        }

        setupRegistrationScreen()
    }

    private fun showRegistrationScreen() {
        Log.d("LoginActivity", "Showing registration screen")
        layoutLogin.visibility = View.GONE
        layoutRegister.visibility = View.VISIBLE
    }

    private fun showLoginScreen() {
        layoutRegister.visibility = View.GONE
        layoutLogin.visibility = View.VISIBLE
    }

    private fun setupRegistrationScreen() {
        Log.d("LoginActivity", "Setting up registration screen")

        val courses = arrayOf("1 курс", "2 курс", "3 курс", "4 курс", "5 курс", "6 курс")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, courses)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCourse.adapter = adapter

        setupDatePicker()
        setupDifficultySeekBar()

        btnBackToLogin.setOnClickListener {
            Log.d("LoginActivity", "Back to login clicked")
            showLoginScreen()
        }

        btnConfirmRegister.setOnClickListener {
            Log.d("LoginActivity", "Confirm register clicked")
            registerUser()
        }
    }

    private fun setupDatePicker() {
        selectedBirthDate = System.currentTimeMillis()
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
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(selectedYear, selectedMonth, selectedDay)
                selectedBirthDate = calendar.timeInMillis
                updateDateDisplay()
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
        etBirthDate.setText(dateFormat.format(Date(selectedBirthDate)))
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

    private fun validateRegistration(): Boolean {
        val fullName = etRegFullName.text.toString().trim()
        val password = etPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()

        if (fullName.isEmpty()) {
            etRegFullName.error = "Введите ФИО"
            return false
        }

        if (fullName.split("\\s+".toRegex()).size < 2) {
            etRegFullName.error = "Введите как минимум имя и фамилию"
            return false
        }

        if (rgGender.checkedRadioButtonId == -1) {
            Toast.makeText(this, "Выберите пол", Toast.LENGTH_SHORT).show()
            return false
        }

        if (password.length < 4) {
            etPassword.error = "Пароль должен быть не менее 4 символов"
            return false
        }

        if (password != confirmPassword) {
            etConfirmPassword.error = "Пароли не совпадают"
            return false
        }

        if (selectedBirthDate == 0L) {
            Toast.makeText(this, "Выберите дату рождения", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun registerUser() {
        if (!validateRegistration()) {
            return
        }

        val fullName = etRegFullName.text.toString().trim()
        val password = etPassword.text.toString()
        val gender = when (rgGender.checkedRadioButtonId) {
            R.id.rbMale -> "Мужской"
            R.id.rbFemale -> "Женский"
            else -> ""
        }
        val course = spCourse.selectedItem.toString()
        val difficulty = sbDifficulty.progress
        val birthDate = selectedBirthDate
        val zodiacSign = calculateZodiacUseCase.execute(birthDate)

        val player = Player(fullName, gender, course, difficulty, birthDate, zodiacSign)

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    preferencesManager.savePlayer(player, password)
                }

                Toast.makeText(
                    this@LoginActivity,
                    "Регистрация успешна!",
                    Toast.LENGTH_SHORT
                ).show()
                startMainActivity()

            } catch (e: Exception) {
                Toast.makeText(
                    this@LoginActivity,
                    "Ошибка регистрации: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                Log.e("LoginActivity", "Registration error", e)
            }
        }
    }

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}