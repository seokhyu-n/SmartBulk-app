package com.example.smartbulk

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var prefs: android.content.SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Firebase 초기화
        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        // ✅ 자동 로그인 체크 상태로 로그인했었고, Firebase 세션이 아직 살아있으면
        //    로그인 화면을 거치지 않고 바로 메인으로 이동
        if (prefs.getBoolean(KEY_AUTO_LOGIN, false) && auth.currentUser != null) {
            goToMain()
            return
        }

        // UI 요소들
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnRegister = findViewById<TextView>(R.id.btnRegister)
        val ivTogglePassword = findViewById<ImageView>(R.id.ivTogglePassword)
        val cbAutoLogin = findViewById<CheckBox>(R.id.cbAutoLogin)
        cbAutoLogin.isChecked = prefs.getBoolean(KEY_AUTO_LOGIN, false)

        // 로그인 버튼 클릭
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            when {
                email.isEmpty() || password.isEmpty() -> {
                    showToast("이메일과 비밀번호를 입력하세요")
                }
                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    showToast("올바른 이메일 형식을 입력하세요")
                }
                else -> {
                    loginUser(email, password, cbAutoLogin.isChecked)
                }
            }
        }

        // 회원가입 텍스트 클릭
        btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // 비밀번호 보기 토글
        ivTogglePassword.setOnClickListener {
            val isVisible = etPassword.inputType == (InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD or InputType.TYPE_CLASS_TEXT)
            if (isVisible) {
                etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                ivTogglePassword.setImageResource(R.drawable.ic_visibility_off)
            } else {
                etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                ivTogglePassword.setImageResource(R.drawable.ic_visibility)
            }
            etPassword.setSelection(etPassword.text.length)
        }
    }

    private fun loginUser(email: String, password: String, autoLogin: Boolean) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    prefs.edit().putBoolean(KEY_AUTO_LOGIN, autoLogin).apply()
                    showToast("로그인 성공!")
                    goToMain()
                } else {
                    showToast("로그인 실패: ${task.exception?.message}")
                }
            }
    }

    private fun goToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val PREFS_NAME = "smartbulk_prefs"
        private const val KEY_AUTO_LOGIN = "auto_login_enabled"
    }
}
