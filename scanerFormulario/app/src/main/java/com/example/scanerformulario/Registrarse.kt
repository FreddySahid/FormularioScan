package com.example.scanerformulario

import android.app.ProgressDialog
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.database.FirebaseDatabase

class Registrarse : AppCompatActivity() {
    lateinit var nombre: EditText
    lateinit var correo: EditText
    lateinit var password1: EditText
    lateinit var password2: EditText
    lateinit var apellidos: EditText
    var firebaseAuth: FirebaseAuth? = null
    lateinit var registrarse: Button
    val database = FirebaseDatabase.getInstance().reference



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            FirebaseApp.initializeApp(this)

            firebaseAuth = FirebaseAuth.getInstance()
        }catch (e: FirebaseAuthException) {
            Log.e(TAG, "FirebaseAuth"+e)
        }
        setContentView(R.layout.activity_registrarse)
        nombre = findViewById(R.id.nombreRegistrarse)
        correo = findViewById(R.id.correoRegistrarse)
        password1 = findViewById(R.id.contrasenaRegistrarse)
        password2 = findViewById(R.id.contrasenaRegistrarse2)
        apellidos = findViewById(R.id.apellidosRegistrarse)




        registrarse = findViewById(R.id.buttonRegistrarse)

        registrarse.setOnClickListener {
            val nombreText = nombre.text.toString().trim()
            val apellidoText = apellidos.text.toString().trim()
            val correoText = correo.text.toString().trim()
            val passwordText = password1.text.toString().trim()
            val passwordText2 = password2.text.toString().trim()


            val banderaCorreo = validarCorreo(correoText)
            val banderaPassword = validarLongitudPassword(passwordText)



            if(nombreText.isEmpty()){
                Toast.makeText(this, "Debes agregar un nombre", Toast.LENGTH_SHORT).show()
            }else if(apellidoText.isEmpty()){
                Toast.makeText(this, "Debes agregar tus apellidos", android.widget.Toast.LENGTH_SHORT).show()
            }else if(correoText.isEmpty()){
                Toast.makeText(this, "Debes agregar un correo", android.widget.Toast.LENGTH_SHORT).show()
            }else if(banderaCorreo == false){
                Toast.makeText(this, "El correo asignado es incorrecto", android.widget.Toast.LENGTH_SHORT).show()
            }else if(banderaPassword == false){
                Toast.makeText(this, "La contraseña debe medir almenos 8 caracteres", android.widget.Toast.LENGTH_SHORT).show()
            } else if(passwordText.isEmpty()){
                Toast.makeText(this, "Debes agregar una contraseña", android.widget.Toast.LENGTH_SHORT).show()
            }else if(passwordText2.isEmpty()){
                Toast.makeText(this, "Debes confirmar tu contraseña", android.widget.Toast.LENGTH_SHORT).show()
            }else if(passwordText != passwordText2){
                Toast.makeText(this, "las contraseñas no coinciden", android.widget.Toast.LENGTH_SHORT).show()
            }else{
                guardarDatos(nombreText, apellidoText, correoText, passwordText)
            }



        }


    }

    private fun validarCorreo(correo: String): Boolean {
        val regex = "^[^@]+@[^@]+\\.[^@]+$"
        return correo.matches(Regex(regex))
    }

    private fun validarLongitudPassword(password: String): Boolean {
        val regex = "^.{8,}$"
        return password.matches(Regex(regex))
    }


    private fun guardarDatos(nombre: String, apellidos: String, correo: String, password: String) {
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Guardando datos...")
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        progressDialog.setCancelable(false)
        progressDialog.show()

        val auth = FirebaseAuth.getInstance()
        auth.fetchSignInMethodsForEmail(correo)
            .addOnCompleteListener { signInMethodsTask ->
                if (signInMethodsTask.isSuccessful) {
                    val signInMethodsResult = signInMethodsTask.result

                    if (signInMethodsResult?.signInMethods?.isNotEmpty() == true) {
                        // El correo ya está registrado en Firebase Authentication
                        Toast.makeText(this, "El correo ya está registrado", Toast.LENGTH_SHORT).show()
                        progressDialog.dismiss()
                    } else {
                        auth.createUserWithEmailAndPassword(correo, password)
                            .addOnCompleteListener { createUserTask ->
                                if (createUserTask.isSuccessful) {
                                    val user = createUserTask.result?.user

                                    if (user != null) {
                                        val usuarioId = user.uid

                                        val usuariosRef = FirebaseDatabase.getInstance().reference.child("usuarios")
                                        val datosUsuario = HashMap<String, Any>()
                                        datosUsuario["nombre"] = nombre
                                        datosUsuario["apellidos"] = apellidos

                                        usuariosRef.child(usuarioId).setValue(datosUsuario)
                                            .addOnCompleteListener { saveDataTask ->
                                                if (saveDataTask.isSuccessful) {
                                                    Toast.makeText(this, "Datos guardados correctamente", Toast.LENGTH_SHORT).show()
                                                    progressDialog.dismiss()

                                                    val currentUser = auth.currentUser
                                                    val userId = currentUser?.uid

                                                    val intent = Intent(this, MainActivity::class.java)
                                                    intent.putExtra("userId", userId)

                                                    // Iniciar la actividad principal
                                                    startActivity(intent)
                                                } else {
                                                    Log.e(TAG, "Error al guardar los datos en la base de datos", saveDataTask.exception)
                                                    Toast.makeText(this, "No se pudieron guardar sus datos", Toast.LENGTH_SHORT).show()
                                                    progressDialog.dismiss()
                                                    finish()
                                                }
                                            }
                                    }
                                } else {
                                    Log.e(TAG, "Error al crear el usuario", createUserTask.exception)
                                    Toast.makeText(this, "No se pudo crear el usuario", Toast.LENGTH_SHORT).show()
                                    progressDialog.dismiss()
                                }
                            }
                    }
                } else {
                    Log.e(TAG, "Error al obtener los métodos de inicio de sesión para el correo", signInMethodsTask.exception)
                    Toast.makeText(this, "Error al obtener los métodos de inicio de sesión", Toast.LENGTH_SHORT).show()
                    progressDialog.dismiss()
                }
            }
    }





}