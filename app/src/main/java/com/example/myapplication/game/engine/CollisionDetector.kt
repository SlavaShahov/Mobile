package com.example.myapplication.game.engine

import com.example.myapplication.domain.model.Insect

object CollisionDetector {

    fun checkInsectCollision(x: Float, y: Float, insect: Insect): Boolean {
        return x >= insect.x && x <= insect.x + insect.bitmap.width &&
                y >= insect.y && y <= insect.y + insect.bitmap.height
    }

    fun findCollidedInsect(x: Float, y: Float, insects: List<Insect>): Insect? {
        return insects.find { checkInsectCollision(x, y, it) }
    }
}