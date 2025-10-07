package com.example.myapplication.domain.model

enum class InsectType { REGULAR, FAST, RARE, BONUS, PENALTY }

data class Insect(
    val type: InsectType,
    var x: Float,
    var y: Float,
    var speedX: Float,
    var speedY: Float,
    val bitmap: android.graphics.Bitmap,
    var health: Int = 1,
    val maxHealth: Int = 1
) {
    fun update(deltaTime: Float) {
        x += speedX * deltaTime
        y += speedY * deltaTime
    }
}