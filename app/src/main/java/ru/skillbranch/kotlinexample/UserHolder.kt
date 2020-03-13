package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting
import kotlin.math.log

object UserHolder {
    private val map = mutableMapOf<String, User>()

    fun registerUser(
        fullName: String,
        email: String,
        password: String
    ): User = User.makeUser(fullName, email = email, password = password).also {
        saveNewUser(it, "email")
    }

    fun loginUser(login: String, password: String): String? {
        val result = if (map[login.trim()] == null) {
            map[getMsisdn(login)]
        } else {
            map[login.trim()]
        }
        return result?.let {
            if (it.checkPassword(password)) it.userInfo
            else null
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun clearHolder() {
        map.clear()
    }

    fun registerUserByPhone(fullName: String, phone: String): User {
        return User.makeUser(fullName, phone = phone).also {
            saveNewUser(it, "phone")
        }
    }

    private fun getMsisdn(phone: String): String {
        return "+" + phone.filter { it.isDigit() }
    }

    private fun saveNewUser(user: User, method: String) {
        with(user) {
            map[login]?.let {
                throw IllegalArgumentException("A user with this $method already exists")
            }
            map[login] = this
        }
    }

    fun requestAccessCode(rawPhone: String) {
        map[getMsisdn(rawPhone)]?.changeAccessCode(rawPhone)
    }
}
