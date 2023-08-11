package com.example.scanerformulario

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ExpandableListView

class ayuda : AppCompatActivity() {
    lateinit var expandableListView: ExpandableListView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ayuda)

        expandableListView = findViewById(R.id.expandableListView)

        val preguntasRespuestas = mutableListOf<PreguntaRespuesta>(
            PreguntaRespuesta("¿Cómo creo un formulario nuevo?", "Puedes crear un formulario nuevo dando clic en el botón que dice \"seleccionar foto\" o en \"Tomar foto\"."),
            PreguntaRespuesta("¿Cómo debe ser la foto?", "Es preferible que la foto se centre en la tabla del formulario y que el texto no se vea borroso para que la detección de texto sea más precisa. "),
            PreguntaRespuesta("¿Cómo puedo añadir más tablas a un formulario?", "Para agregar más tablas a un formulario debes dar clic en el formulario y una vez adentro puedes seleccionar una foto o tomarla. "),
            PreguntaRespuesta("¿Cómo puedo eliminar un formulario?","Para eliminar un formulario debes mantener presionado el formulario que deseas eliminar, luego aparecerá un menú con la opción \"Eliminar\"."),
            PreguntaRespuesta("¿Cómo puedo eliminar una hoja de un formulario?","Para eliminar una hoja debes mantener presionada la hoja que deseas eliminar, luego aparecerá un menú con la opción \"Eliminar\"."),
            PreguntaRespuesta("¿Cómo puedo renombrar un formulario?", "Para renombrar un formulario debes mantener presionado el formulario que deseas renombrar, luego aparecerá un menú con la opción \"Cambiar nombre\"."),
            PreguntaRespuesta("¿Cómo puedo renombrar una hoja de un formulario?", "Para renombrar una hoja debes mantener presionada la hoja que deseas renombrar, luego aparecerá un menú con la opción \"Cambiar nombre\"."),
            PreguntaRespuesta("¿Cómo puedo descargar un formulario? ","Para descargar un formulario debes mantener presionado el formulario que deseas descargar, luego aparecerá un menú con la opción \"Descargar\", finalmente se generará un archivo CSV y se guardará en la carpeta de descargas de tu dispositivo."),
            PreguntaRespuesta("¿Cómo puedo descargar una hoja de un formulario?","Para descargar una hoja de un formulario debes mantener presionada la hoja que deseas descargar, luego aparecerá un menú con la opción \"Descargar\", finalmente se generará un archivo CSV y se guardará en la carpeta de descargas de tu dispositivo."),
            PreguntaRespuesta("¿Cómo puedo cerrar mi sesión?","Para cerrar tu sesión debes dar clic en el botón \"Datos personales\". Ahí verás un botón que dice \"Salir\", haz clic en él y tu sesión se cerrará."),
            PreguntaRespuesta("¿Cómo puedo editar mis datos? ","Para editar tus datos debes dar clic en el botón \"Datos personales\". Ahí verás un botón que dice \"Editar\", haz clic en él y podrás cambiar tu nombre, apellido, correo o contraseña. Los datos se guardarán cuando des clic en el botón \"GUARDAR\" o si deseas cancelar solo haz clic en el botón \"CANCELAR\"."),
            PreguntaRespuesta("¿Cómo puedo generar un reporte de entradas de un formulario? ", "Para generar un reporte de entradas de un formulario debes mantener presionado el formulario que deseas descargar, luego aparecerá un menú con la opción \"Generar reportes\", finalmente se generará un archivo CSV de los profesores que sí firmaron y otro de los que no, los cuales se guardarán en la carpeta de descargas de tu dispositivo."),
            PreguntaRespuesta("¿Cómo puedo generar un reporte de entradas de una hoja de un formulario?","Para generar un reporte de entradas de una hoja de un formulario debes mantener presionada la hoja que deseas descargar, luego aparecerá un menú con la opción \"Generar reportes\", finalmente se generará un archivo CSV de los profesores que sí firmaron y otro de los que no, los cuales se guardarán en la carpeta de descargas de tu dispositivo.")

            // Agrega más preguntas y respuestas según sea necesario
        )

        // Crea un adaptador personalizado para el ExpandableListView
        val adapter = PreguntaRespuestaAdapter(this, preguntasRespuestas)

        // Asigna el adaptador al ExpandableListView
        expandableListView.setAdapter(adapter)
    }
}

data class PreguntaRespuesta(val pregunta: String, val respuesta: String)
