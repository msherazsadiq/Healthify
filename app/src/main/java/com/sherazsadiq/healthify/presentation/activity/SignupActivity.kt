package com.sherazsadiq.healthify.presentation.activity

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.sherazsadiq.healthify.R
import com.sherazsadiq.healthify.databinding.ActivitySignupBinding
import com.sherazsadiq.healthify.presentation.viewmodel.SignupViewModel
import com.sherazsadiq.healthify.utils.ResultState

class SignupActivity : AppCompatActivity() {
    private lateinit var binding : ActivitySignupBinding
    private lateinit var signupViewModel: SignupViewModel

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.signinButton.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }


        // Initialize ViewModel
        signupViewModel = ViewModelProvider(this).get(SignupViewModel::class.java)

        // Observe the sign-up result
        signupViewModel.signupResult.observe(this, { result ->
            when (result) {
                is ResultState.Loading -> {
                    // Show loading state (e.g., progress bar)
                    binding.progressBar.visibility = android.view.View.VISIBLE
                }
                is ResultState.Success -> {
                    // Sign up successful
                    binding.progressBar.visibility = android.view.View.GONE
                    Toast.makeText(this, "Sign Up Successful!", Toast.LENGTH_SHORT).show()
                    finish() // Close the activity or redirect to login
                }
                is ResultState.Error -> {
                    // Show error message
                    binding.progressBar.visibility = android.view.View.GONE
                    Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
                }
            }
        })

        // Set up sign-up button click listener
        binding.signupButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()
            val confirmPassword = binding.confirmPasswordEditText.text.toString().trim()

            // Validate the input fields
            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check if passwords match
            if (password != confirmPassword) {
                binding.errorMessage.visibility = android.view.View.VISIBLE
                binding.errorMessage.text = "Passwords do not match"
            } else {
                binding.errorMessage.visibility = android.view.View.GONE
                // Proceed with sign-up using ViewModel
                signupViewModel.signup(email, password)
            }
        }
    }

}