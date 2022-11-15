package io.hydok.biometric

import android.app.Activity
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings.ACTION_BIOMETRIC_ENROLL
import android.provider.Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import io.hydok.biometric.databinding.ActivityMainBinding
import java.util.concurrent.Executor

class BiometricActivity : AppCompatActivity() {
    private val TAG = "BiometricActivity"

    private val binding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }


    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this@BiometricActivity, executor, authenticationCallback)
        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("생체 인증")
            .setSubtitle("인증해주세요.")
            .setNegativeButtonText("취소")
            .build()



        binding.authBtn.setOnClickListener {
            authenticateToEncrypt()
        }
    }

    private val authenticationCallback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            super.onAuthenticationError(errorCode, errString)
            Log.d(TAG, "onAuthenticationError - code: $errorCode, - errStr: $errString")
            Toast.makeText(this@BiometricActivity, errString, Toast.LENGTH_SHORT).show()
        }

        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            super.onAuthenticationSucceeded(result)
            Log.d(TAG, "onAuthenticationSucceeded")
            Toast.makeText(this@BiometricActivity, "인증 완료", Toast.LENGTH_SHORT).show()
        }

        override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()
            Log.d(TAG, "onAuthenticationFailed")
        }
    }


    /**
     * 생체 인식 인증을 사용할 수 있는지 확인
     * */
    private fun authenticateToEncrypt() = with(binding) {
        val textStatus: String
        val biometricManager = BiometricManager.from(this@BiometricActivity)

        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {

            //생체 인증 가능
            BiometricManager.BIOMETRIC_SUCCESS -> {
                textStatus = "App can authenticate using biometrics."
                biometricPrompt.authenticate(promptInfo) //인증 실행
            }

            //기기에서 생체 인증을 지원하지 않는 경우
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> textStatus =
                "No biometric features available on this device."

            //현재 생체 인증을 사용할 수 없는 경우
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> textStatus =
                "Biometric features are currently unavailable."

            //생체 인식 정보가 등록되어 있지 않은 경우
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                textStatus = "Prompts the user to create credentials that your app accepts."

                val dialogBuilder = AlertDialog.Builder(this@BiometricActivity)
                dialogBuilder
                    .setMessage("지문 등록이 필요합니다. 지문등록 설정화면으로 이동하시겠습니까?")
                    .setPositiveButton("확인") { _, _ -> goBiometricSettings() }
                    .setNegativeButton("취소") { dialog, _ -> dialog.cancel() }
                dialogBuilder.show()
            }

            //기타 실패
            else -> textStatus = "Fail Biometric facility"

        }
        Log.d(TAG, textStatus)
    }

    /**
     * 지문 등록 화면으로 이동
     */
    private fun goBiometricSettings() {
        val enrollIntent = Intent(ACTION_BIOMETRIC_ENROLL).apply {
            putExtra(
                EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                BIOMETRIC_STRONG or DEVICE_CREDENTIAL
            )
        }
        loginLauncher.launch(enrollIntent)
    }
    private val loginLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            if (result.resultCode == Activity.RESULT_OK) {
                authenticateToEncrypt()  //생체 인증 가능 여부 실행
            } else {
                //none
            }
        }
}