package com.example.scanerformulario

import android.app.ProgressDialog
import android.content.ContentValues.TAG
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth

class Login : AppCompatActivity() {
    lateinit var correoInicioSesion: EditText
    lateinit var passwordInicioSesion: EditText
    lateinit var registrarse: TextView
    lateinit var btnInicioSesion: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        correoInicioSesion = findViewById(R.id.correoIniciarSesion)
        passwordInicioSesion = findViewById(R.id.contrasenaIniciarSesion)
        registrarse = findViewById(R.id.textviewRegistrarse)
        btnInicioSesion = findViewById(R.id.buttonIniciarSesion)

       

        btnInicioSesion.setOnClickListener {
            val correoText = correoInicioSesion.text.toString().trim()
            val passwordText = passwordInicioSesion.text.toString().trim()
            if(correoText.isEmpty()){
                Toast.makeText(this, "Debes agregar un correo", android.widget.Toast.LENGTH_SHORT).show()
            }else if(passwordText.isEmpty()){
                Toast.makeText(this, "Debes agregar una contraseña", android.widget.Toast.LENGTH_SHORT).show()
            }else{
                iniciarSesion(correoText, passwordText)
            }
        }

        registrarse.setOnClickListener{
            startActivity(Intent(this@Login, Registrarse::class.java))
        }

    }

    private fun iniciarSesion(correo: String, password: String) {
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Iniciando sesión...")
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        progressDialog.setCancelable(false)
        progressDialog.show()

        val auth = FirebaseAuth.getInstance()
        auth.signInWithEmailAndPassword(correo, password)
            .addOnCompleteListener { signInTask ->
                if (signInTask.isSuccessful) {
                    // El inicio de sesión fue exitoso
                    Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()
                    progressDialog.dismiss()

                    val currentUser = auth.currentUser
                    val userId = currentUser?.uid

                    // Continuar con la lógica de tu aplicación después del inicio de sesión exitoso
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("userId", userId)

                    // Iniciar la actividad principal
                    startActivity(intent)

                    finish()

                } else {
                    // Hubo un error en el inicio de sesión
                    Log.e(TAG, "Error al iniciar sesión", signInTask.exception)
                    Toast.makeText(this, "Error al iniciar sesión. Verifica tu correo y contraseña.", Toast.LENGTH_SHORT).show()
                    progressDialog.dismiss()
                }
            }
    }



}