package ru.skillbranch.kotlinexample

import android.annotation.SuppressLint
import androidx.annotation.VisibleForTesting
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*

class User private constructor(
    val firstName: String,
    val lastName: String?,
    email: String? = null,
    rawPhone: String? = null,
    meta: Map<String, Any>? = null
) {
    val userInfo: String

    private val fullName: String
        @SuppressLint("DefaultLocale")
        get() = listOfNotNull(firstName, lastName)
            .joinToString { " " }
            .capitalize()

    private val initials: String
        get() = listOfNotNull(firstName, lastName)
            .map { it.first().toUpperCase() }
            .joinToString { " " }

    private var phone: String? = null
        set(value) {
            field = value?.replace("[^+\\d]".toRegex(), "")
        }


    private var _login: String? = null
    var login: String
        get() = _login!!
        set(value) {
            _login = value.toLowerCase(Locale.getDefault())
        }

    private var salt: String? = null
    private lateinit var passwordHash: String

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    var accessCode: String? = null

    //For email
    constructor(
        firstName: String,
        lastName: String?,
        email: String,
        password: String
    ) : this(
        firstName,
        lastName,
        email = email,
        meta = mapOf("auth" to "password")
    ) {
        passwordHash = encrypt(password)
    }

    //For phone
    constructor(
        firstName: String,
        lastName: String?,
        rawPhone: String
    ) : this(
        firstName,
        lastName,
        rawPhone = rawPhone,
        meta = mapOf("auth" to "sms")
    ) {
        val code = generateAccessCode()
        passwordHash = encrypt(code)
        accessCode = code
        sendAccessCodeToUser(rawPhone, code)
    }

    init {
        check(firstName.isNotBlank()) { "First Name must not be blank" }
        check(!email.isNullOrBlank() || !rawPhone.isNullOrBlank()) { "Email or phone must not be blank" }
        phone = rawPhone
        login = email ?: phone!!
        userInfo = """
                  firstName: $firstName
                  lastName: $lastName
                  login: $login
                  fullName: $fullName
                  initials: $initials
                  email: $email
                  phone: $phone
                  meta: $meta
            """.trimIndent()
    }

    fun checkPassword(password: String) = encrypt(password) == passwordHash

    fun changePassword(oldPass: String, newPass: String) {
        if (checkPassword(oldPass)) {
            passwordHash = encrypt(newPass)
            if (!accessCode.isNullOrBlank()) accessCode = newPass
        } else {
            throw IllegalArgumentException("Wrong old password")
        }
    }

    private fun encrypt(password: String): String {
        if (salt.isNullOrBlank()) {
            salt = ByteArray(16).also { SecureRandom().nextBytes(it) }.toString()
        }
        return salt.plus(password).md5()
    }

    private fun sendAccessCodeToUser(rawPhone: String, code: String) {
        println("Send code $code to user $rawPhone")
    }

    private fun generateAccessCode(): String {
        val possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return StringBuilder().apply {
            repeat(6) {
                possible.indices.random().also {
                    append(possible[it])
                }
            }
        }.toString()
    }

    private fun String.md5(): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(toByteArray())
        val hexString = BigInteger(1, digest).toString(16)
        return hexString.padStart(32, '0')
    }

    companion object Factory {
        fun makeUser(
            fullName: String,
            email: String? = null,
            password: String? = null,
            phone: String? = null
        ): User {
            val (firstName, lastName) = fullName.fullNameToPair()

            return when {
                !phone.isNullOrBlank() -> User(firstName, lastName, phone)
                !email.isNullOrBlank() -> User(
                    firstName,
                    lastName,
                    email,
                    password
                )
                else -> throw IllegalArgumentException("Email or phone must not be blank")
            }
        }

        private fun String.fullNameToPair(): Pair<String, String?> {
            return this.split(" ")
                .filter { it.isNotBlank() }
                .run {
                    when (size) {
                        1 -> first() to null
                        2 -> first() to last()
                        else -> throw IllegalArgumentException("Incorrect fullname")
                    }
                }
        }
    }
}
