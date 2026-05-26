package com.example.smartbulk

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Firebase 초기화
        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()

        // View 참조 - XML 내 ID와 일치시킴
        val etEmail = findViewById<EditText>(R.id.emailEditText)
        val etPassword = findViewById<EditText>(R.id.passwordEditText)
        val etName = findViewById<EditText>(R.id.nameEditText)
        val etAge = findViewById<EditText>(R.id.ageEditText)
        val etHeight = findViewById<EditText>(R.id.heightEditText)
        val etWeight = findViewById<EditText>(R.id.weightEditText)
        val spinnerGoal = findViewById<Spinner>(R.id.spinnerGoal)
        val btnRegister = findViewById<Button>(R.id.registerButton)

        // 목표 설정
        val goalList = listOf("체중 감량", "근육 증가", "건강 유지")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, goalList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerGoal.adapter = adapter

        // 버튼 클릭 리스너
        btnRegister.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val name = etName.text.toString().trim()
            val age = etAge.text.toString().trim().toIntOrNull()
            val height = etHeight.text.toString().trim().toIntOrNull()
            val weight = etWeight.text.toString().trim().toIntOrNull()
            val goal = spinnerGoal.selectedItem.toString()

            // 유효성 검사
            when {
                email.isEmpty() || password.isEmpty() -> {
                    showToast("이메일과 비밀번호를 입력하세요")
                }
                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    showToast("올바른 이메일 형식을 입력하세요")
                }
                password.length < 6 -> {
                    showToast("비밀번호는 최소 6자리 이상이어야 합니다")
                }
                name.isEmpty() || age == null || height == null || weight == null -> {
                    showToast("이름, 나이, 키, 몸무게를 모두 정확히 입력하세요")
                }
                else -> {
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val uid = auth.currentUser?.uid ?: return@addOnCompleteListener
                                val userMap = mapOf(
                                    "name" to name,
                                    "age" to age,
                                    "height" to height,
                                    "weight" to weight,
                                    "goal" to goal
                                )

                                FirebaseDatabase.getInstance().getReference("users")
                                    .child(uid)
                                    .setValue(userMap)
                                    .addOnSuccessListener {
                                        showToast("회원가입 성공! 로그인 화면으로 이동합니다.")
                                        startActivity(Intent(this, LoginActivity::class.java))
                                        finish()
                                    }
                                    .addOnFailureListener {
                                        showToast("회원가입은 성공했지만 정보 저장에 실패했습니다.")
                                    }
                            } else {
                                showToast("회원가입 실패: ${task.exception?.message}")
                            }
                        }
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
