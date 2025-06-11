package com.sherazsadiq.healthify.presentation.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.sherazsadiq.healthify.databinding.ActivityLoginBinding
import com.sherazsadiq.healthify.presentation.viewmodel.AuthViewModel
import com.sherazsadiq.healthify.utils.ResultState
import kotlin.getValue


class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val authViewModel by viewModels<AuthViewModel>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.loginButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            authViewModel.login(email, password)
        }

        binding.signUpButton.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        observeAuth()
    }

    private fun observeAuth() {
        authViewModel.authState.observe(this) { state ->
            when (state) {
                is ResultState.Loading -> {
                    binding.loginProgressBar.visibility = android.view.View.VISIBLE
                }
                is ResultState.Success -> {
                    binding.loginProgressBar.visibility = android.view.View.GONE

                    // Save credentials
                    val sharedPref = getSharedPreferences("SynotePrefs", MODE_PRIVATE)
                    sharedPref.edit().apply {
                        putString("email", binding.emailEditText.text.toString().trim())
                        putString("password", binding.passwordEditText.text.toString().trim())
                        putBoolean("isLoggedIn", true)
                        apply()
                    }

                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                }

                is ResultState.Error -> {
                    binding.loginProgressBar.visibility = android.view.View.GONE
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
                else -> Unit
            }
        }
    }
}
