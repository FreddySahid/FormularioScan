package com.example.scanerformulario

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class CambiarDatosUsuario : AppCompatActivity() {

    private lateinit var userId: String
    lateinit var editTextNombre: EditText
    lateinit var editTextApellido: EditText
    lateinit var editTextCorreo: EditText
    lateinit var editTextPassword: EditText
    lateinit var textViewNombre: TextView
    lateinit var textViewCorreo: TextView
    lateinit var textViewApellido: TextView
    lateinit var buttonEdit: ImageButton
    lateinit var buttonSalir: ImageButton
    lateinit var buttonGuardar: Button
    lateinit var layoutSalir: LinearLayout
    lateinit var layoutEdit: LinearLayout
    lateinit var layoutGC: LinearLayout
    lateinit var textViewPassword: TextView
    lateinit var toolbarEdit: Toolbar
    lateinit var buttonCancelar: Button
    lateinit var salir : TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cambiar_datos_usuario)
        userId = intent.getStringExtra("userId") ?: ""
        Log.d("TAG","EL ID ES "+userId )



        buttonGuardar = findViewById(R.id.buttonGuardar)
        toolbarEdit = findViewById(R.id.toolbarSalirEditar)
        layoutGC = findViewById(R.id.layoutGuardarCancelar)
        salir = findViewById(R.id.log_out_salir)



        textViewPassword = findViewById(R.id.password_text)

        layoutEdit = findViewById(R.id.layoutedit)
        layoutSalir = findViewById(R.id.layoutLogout)
        editTextNombre = findViewById(R.id.nombre_editar)
        editTextApellido = findViewById(R.id.apellido_editar)
        editTextCorreo = findViewById(R.id.correo_editar)
        editTextPassword = findViewById(R.id.contrasena_editar)
        buttonSalir = findViewById(R.id.log_out)

        textViewNombre = findViewById(R.id.nombre_text)
        textViewApellido = findViewById(R.id.apellido_text)
        textViewCorreo = findViewById(R.id.correo_text)

        buttonEdit = findViewById(R.id.edit_data_user)
        buttonCancelar = findViewById(R.id.buttonCancelar)

        buttonEdit.setOnClickListener {
            ocultarTextView()

        }

        buttonGuardar.setOnClickListener{

            ModificarDatos(userId)

        }
        buttonSalir.setOnClickListener{
            cerrarSesionYBloquearRetroceso()
        }
        buttonCancelar.setOnClickListener {
            cancelarCambioDatos()
        }
        obtenerDatosUsuario(userId)

        val database = FirebaseDatabase.getInstance()
        val reference = database.getReference("usuarios").child(userId)
        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                Log.d("TAG", "No hubo ningun error")
                obtenerDatosUsuario(userId)


            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.d("TAG","Hubo un error: "+databaseError)
                showToast("Hubo un error a encontrar los cambios generados")
            }
        }



    }
    fun cancelarCambioDatos(){
        verTextview()
        editTextPassword.setText("")
        editTextApellido.setText("")
        editTextNombre.setText("")
        editTextCorreo.setText("")

        verTextview()
    }
    fun ocultarTextView(){
        textViewNombre.visibility = View.GONE
        textViewApellido.visibility = View.GONE
        textViewCorreo.visibility = View.GONE
        layoutSalir.visibility = View.GONE
        layoutEdit.visibility = View.GONE
        textViewPassword.visibility = View.GONE
        toolbarEdit.visibility = View.GONE

        layoutGC.visibility = View.VISIBLE
        editTextNombre.visibility = View.VISIBLE
        editTextApellido.visibility = View.VISIBLE
        editTextCorreo.visibility = View.VISIBLE
        editTextPassword.visibility = View.VISIBLE
        obtenerDatosUsuarioEditText(userId)
    }
    fun verTextview(){
        textViewNombre.visibility = View.VISIBLE
        textViewApellido.visibility = View.VISIBLE
        textViewCorreo.visibility = View.VISIBLE
        textViewPassword.visibility = View.VISIBLE
        layoutEdit.visibility = View.VISIBLE
        layoutSalir.visibility = View.VISIBLE
        toolbarEdit.visibility = View.VISIBLE

        editTextNombre.visibility = View.GONE
        editTextApellido.visibility = View.GONE
        editTextCorreo.visibility = View.GONE
        editTextPassword.visibility = View.GONE
        layoutGC.visibility = View.GONE

    }

    private fun obtenerDatosUsuarioEditText(userId: String){
        try{
            val auth = FirebaseAuth.getInstance()
            val user = auth.currentUser

            user?.let {
                val correo = it.email
                // Haz lo que necesites con el correo obtenido, como actualizar la interfaz de usuario
                editTextCorreo.setText(correo)
            }

            val database = FirebaseDatabase.getInstance()
            val reference = database.getReference("usuarios").child(userId)
            reference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Verifica si el usuario existe
                    if (snapshot.exists()) {
                        // Obtén los valores de nombre y apellidos del snapshot
                        val nombre = snapshot.child("nombre").getValue(String::class.java)
                        val apellidos = snapshot.child("apellidos").getValue(String::class.java)

                        editTextNombre.setText( nombre)
                        editTextApellido.setText(apellidos)

                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Manejo de errores en caso de que la lectura sea cancelada
                    showToast("No se pudieron obtener todos los datos, intentelo más tarde")
                    Log.e("TAG", "Error: " + error)

                }
            })
        }catch (e: Exception) {
            Log.d("TAG", "Error: "+e)
            showToast("Intentelo más tarde")

        }

    }

    private fun obtenerDatosUsuario(userId: String) {

        try{
            val auth = FirebaseAuth.getInstance()
            val user = auth.currentUser

            user?.let {
                val correo = it.email
                // Haz lo que necesites con el correo obtenido, como actualizar la interfaz de usuario
                textViewCorreo.text= "Correo: "+correo
            }

            val database = FirebaseDatabase.getInstance()
            val reference = database.getReference("usuarios").child(userId)
            reference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Verifica si el usuario existe
                    if (snapshot.exists()) {
                        // Obtén los valores de nombre y apellidos del snapshot
                        val nombre = snapshot.child("nombre").getValue(String::class.java)
                        val apellidos = snapshot.child("apellidos").getValue(String::class.java)

                        textViewNombre.text = "Nombre: " + nombre
                        textViewApellido.text = "Apellido: " + apellidos
                        editTextCorreo.setText("")
                        editTextNombre.setText("")
                        editTextPassword.setText("")
                        editTextApellido.setText("")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Manejo de errores en caso de que la lectura sea cancelada
                    showToast("No se pudieron obtener todos los datos, intentelo más tarde")
                    Log.e("TAG", "Error: " + error)

                }
            })
        }catch (e: Exception) {
            Log.d("TAG", "Error: "+e)
            showToast("Intentelo más tarde")

        }

    }
    fun showToast(mensaje: String){
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
    }
    fun ModificarDatos(userId: String){

        val correoText = editTextCorreo.text.toString().trim()
        val passwordText = editTextPassword.text.toString().trim()
        val nombreText = editTextNombre.text.toString().trim()
        val apellidoText = editTextApellido.text.toString().trim()




        val banderaCorreo = validarCorreo(correoText)
        val banderaPassword = validarLongitudPassword(passwordText)



        if(passwordText.isEmpty()){
            if(nombreText.isEmpty()){
              showToast("El campo nombre no puede quedar vacio")
            }else if(apellidoText.isEmpty()){
                showToast("El campo apellido no puede quedar vacio")
            }else if(correoText.isEmpty()){
                showToast("El campo correo no puede quedar vacio")
            }else if(banderaCorreo == false){
                showToast(("El correo no es valido"))
            }else{
                ModificarCorreo(correoText)
                modificarNombre(nombreText)
                modificarApellido(apellidoText)
                verTextview()
            }
        }else {
            if(nombreText.isEmpty()){
                showToast("El campo nombre no puede estar vacio")
            }else if(apellidoText.isEmpty()){
                showToast("El campo apellido no puede estar vacio")
            }else if(correoText.isEmpty()){
                showToast("El campo correo no puede estar vacio")
            }else if(banderaCorreo == false) {
                showToast("El correo no es valido")
            }else if(banderaPassword == false) {
                showToast("La contraseña debe medir almenos 8 caracteres")
            }else{
                ModificarCorreo(correoText)
                modificarNombre(nombreText)
                modificarApellido(apellidoText)
                ModificarPassword(passwordText) { flag ->
                    // Aquí puedes utilizar el valor de flag según el resultado del cambio de contraseña
                    if (flag) {
                        // El cambio de contraseña fue exitoso
                        showToast("Datos guardados correctamente")
                    } else {
                        // Ocurrió un error al cambiar la contraseña
                        showToast("Datos no guardados correctamente")
                    }
                }
                verTextview()
            }

        }
    }

    fun modificarApellido(apellido: String){
        val database = FirebaseDatabase.getInstance()
        val reference = database.getReference("usuarios").child(userId).child("apellidos")
        reference.setValue(apellido).addOnSuccessListener {
            Log.d("TAG", "Apellido modificado exitosamente")
        }.addOnFailureListener{exception ->
            Log.d("TAG", "Error al tratar de cambiar el apellido: ${exception.message}")

        }

    }
    fun modificarNombre(nombre: String){
        val database = FirebaseDatabase.getInstance()
        val reference = database.getReference("usuarios").child(userId).child("nombre")
        reference.setValue(nombre).addOnSuccessListener {
            Log.d("TAG", "Nombre modificado exitosamente")
        }.addOnFailureListener{exception ->
            Log.d("TAG", "Error al tratar de cambiar el nombre: ${exception.message}")

        }

    }
    fun ModificarCorreo(correoText: String){

        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Guardando datos...")
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        progressDialog.setCancelable(false)
        progressDialog.show()

        user?.updateEmail(correoText)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // El cambio de correo electrónico fue exitoso
                    Log.d("TAG", "Correo electrónico actualizado correctamente")
                    progressDialog.dismiss()
                    showToast("Los datos modificados")
                } else {
                    // Ocurrió un error al cambiar el correo electrónico
                    Log.e("TAG", "Error al actualizar el correo electrónico", task.exception)
                    progressDialog.dismiss()
                    showToast("No se pudieron guardar los cambios, intentelo más tarde")

                }
            }

    }
    fun ModificarPassword(password: String, callback: (Boolean) -> Unit)  {


        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Guardando datos...")
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        progressDialog.setCancelable(false)
        progressDialog.show()
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        // Reemplaza con la nueva contraseña que deseas establecer

        user?.updatePassword(password)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // El cambio de contraseña fue exitoso
                    Log.d("TAG", "Contraseña actualizada correctamente")
                    progressDialog.dismiss()

                    callback(true)


                } else {
                    // Ocurrió un error al cambiar la contraseña
                    Log.e("TAG", "Error al actualizar la contraseña", task.exception)
                    progressDialog.dismiss()
                    callback(false)
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

    private fun cerrarSesionYBloquearRetroceso() {
        // Cerrar sesión de Firebase

        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        auth.signOut()

        // Limpiar pila de actividades y abrir actividad de inicio de sesión
        val intent = Intent(this, Login::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

}