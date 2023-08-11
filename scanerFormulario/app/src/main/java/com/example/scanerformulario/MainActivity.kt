package com.example.scanerformulario

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import java.text.SimpleDateFormat
import java.util.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import android.content.Context
import android.view.LayoutInflater
import android.widget.*
import androidx.core.content.PackageManagerCompat.LOG_TAG
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import org.opencv.core.Rect
import org.opencv.core.Point
import androidx.appcompat.widget.SearchView
import com.opencsv.CSVWriter
import kotlinx.coroutines.*
import java.io.*
import java.nio.charset.StandardCharsets


class MainActivity : AppCompatActivity() {


    lateinit var camara: ImageButton
    lateinit var select: ImageButton
    lateinit var count: ImageButton
    lateinit var help: ImageButton


    lateinit var bitmap: Bitmap
    var SELECT_CODE = 100
    var CAMERA_CODE = 101
    lateinit var mat: Mat
    private lateinit var imageUri: Uri
    lateinit var photoFile: File
    lateinit var path: String
    private lateinit var recyclerView: RecyclerView
    private lateinit var proyectoAdapter: ProyectoAdapter
    private lateinit var userId: String







    data class Proyecto(val id: String = "", val nombre: String = "")



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (OpenCVLoader.initDebug()) {
            Log.d("LOADED", "success")
        } else {
            Log.d("LOADED", "error")
        }
        //
        //Log.d("TAG", "El userID es "+userId)
        getPermission()
        camara = findViewById(R.id.camara)
        select = findViewById(R.id.select)
        count = findViewById(R.id.dataUser)
        help = findViewById(R.id.dataUserhelp)




        userId = intent.getStringExtra("userId") ?: ""

        //Log.d("TAG", "El id del usuario es "+userId)

        // Inicializar el RecyclerView y su adaptador
        recyclerView = findViewById(R.id.recyclerViewProyectos)
        proyectoAdapter = ProyectoAdapter(this@MainActivity, userId)


        proyectoAdapter.setOnItemClickListener { proyecto ->
            val intent = Intent(this@MainActivity, HojasActivity::class.java)
            intent.putExtra("proyectoId", proyecto.id)
            startActivity(intent)
        }



        recyclerView.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 2)
            adapter = proyectoAdapter
        }

        // Obtener los proyectos del usuario y actualizar el adaptador
        obtenerProyectosUsuario(userId) { proyectos ->
            proyectoAdapter.actualizarProyectos(proyectos)
        }

        recyclerView.setOnCreateContextMenuListener(this)
        registerForContextMenu(recyclerView)

        val searchView: SearchView = findViewById(R.id.searchView)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                // Realizar búsqueda cuando se envía el formulario del SearchView (opcional)
                searchProjects(query)
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                // Realizar búsqueda a medida que el texto cambia
                searchProjects(newText)
                return true
            }
        })



        select.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, SELECT_CODE)
        }
        camara.setOnClickListener {
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { intent ->
                if (intent.resolveActivity(packageManager) != null) {
                    val imageFile: File? = try {
                        createImageFile()
                    } catch (e: IOException) {
                        throw RuntimeException(e)
                    }
                    imageFile?.let { file ->
                        imageUri = FileProvider.getUriForFile(this, "$packageName.provider", file)
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
                        grantUriPermission(packageName, imageUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        startActivityForResult(intent, CAMERA_CODE)
                    }
                }
            }
        }

        count.setOnClickListener {
            val intent = Intent(this@MainActivity, CambiarDatosUsuario::class.java)
            intent.putExtra("userId", userId)
            this.startActivity(intent)

        }

        help.setOnClickListener {
            val intent= Intent(this@MainActivity, ayuda::class.java)
            this.startActivity(intent)
        }

        val database = FirebaseDatabase.getInstance()
        val reference = database.getReference("proyectos")
        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                //Log.d("TAG", "No hubo ningun error")
                obtenerProyectosUsuario(userId) { proyectos ->
                    proyectoAdapter.actualizarProyectos(proyectos)
                    proyectoAdapter.notifyDataSetChanged()
                }

            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.d("TAG","Hubo un error: "+databaseError)
                showToast("Hubo un error a encontrar los cambios generados.", this@MainActivity)
            }
        }

        reference.addValueEventListener(valueEventListener)


    }

    private fun actualizarProyectos(context: Context, idProyecto: String) {
        obtenerProyectosUsuario(idProyecto) { proyectos ->
            proyectoAdapter.actualizarProyectos(proyectos)
            proyectoAdapter.notifyDataSetChanged()
        }
    }
    fun showToast(mensaje: String, context: Context){
        Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show()
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SELECT_CODE && data != null) {
            try {
                val inputStream: InputStream? = contentResolver.openInputStream(data.data!!)
                val options = BitmapFactory.Options()
                options.inSampleSize = 4 // Ajusta la calidad de la imagen según tus necesidades
                val bitmap: Bitmap = BitmapFactory.decodeStream(inputStream, null, options)!!
                processImageAndSaveAsCSV(bitmap)

            } catch (e: IOException) {
                Log.e("TAG", "Error al leer el archivo ", e)
            }
        }
        if (requestCode == CAMERA_CODE && resultCode == Activity.RESULT_OK) {
            try {
                val inputStream: InputStream? = contentResolver.openInputStream(imageUri)
                if (inputStream != null && isImageFile(imageUri)) {
                    val options = BitmapFactory.Options()
                    options.inSampleSize = 4 // aquí puedes ajustar la calidad de la imagen
                    val bitmap: Bitmap = BitmapFactory.decodeStream(inputStream, null, options)!!
                    processImageAndSaveAsCSV(bitmap)
                } else {
                    Toast.makeText(this, "El archivo seleccionado debe ser una imagen.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isImageFile(uri: Uri): Boolean {
        val mimeType = contentResolver.getType(uri)
        return mimeType?.startsWith("image") == true
    }
    // Clase para almacenar los datos de cada celda
    data class CellData(val x: Int, val y: Int, val bitmap: Bitmap, val result: String)
    private fun processImageAndSaveAsCSV(bitmap: Bitmap) {
        try {
            val mat = Mat()
            Utils.bitmapToMat(bitmap, mat)
            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY)

            // Umbralización adaptativa para resaltar los bordes de la hoja
            val thresh = Mat()
            Imgproc.adaptiveThreshold(
                mat, thresh,
                255.0, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY_INV, 15, 2.0
            )

            // Detección de contornos
            val contours: MutableList<MatOfPoint> = ArrayList()
            val hierarchy = Mat()
            Imgproc.findContours(thresh, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE)

            var largestContour: MatOfPoint? = null
            var maxArea = 0.0

            for (contour in contours) {
                val area = Imgproc.contourArea(contour)
                if (area > maxArea) {
                    maxArea = area
                    largestContour = contour
                }
            }



            val color = Scalar(0.0, 0.0, 0.0) // Color rojo
            val colorPunto = Scalar(255.0, 0.0, 0.0)
            var contador = 1 // Contador para el orden de las celdas
            val areaThreshold = 1020

            // Ordenar los contornos por coordenada x del rectángulo delimitador
            contours.sortBy { Imgproc.boundingRect(it).x }

            val results: ArrayList<CellData> = ArrayList() // Array para almacenar los resultados

            try {
                runBlocking {
                    for (i in contours.indices) {
                        val contourArea = Imgproc.contourArea(contours[i])

                        if (contourArea > areaThreshold && contourArea < (mat.width() * mat.height()) - 100) {
                            val epsilon = 0.1 * Imgproc.arcLength(MatOfPoint2f(*contours[i].toArray()), true)
                            val approxCurve = MatOfPoint2f()
                            Imgproc.approxPolyDP(MatOfPoint2f(*contours[i].toArray()), approxCurve, epsilon, true)

                            // Encontrar el rectángulo delimitador de la celda
                            val rect = Imgproc.boundingRect(contours[i])
                            // Dibujar un rectángulo alrededor de la celda

                            // Recortar el área del contorno y guardarlo en un nuevo Bitmap
                            val croppedBitmap = Bitmap.createBitmap(bitmap, rect.x, rect.y, rect.width, rect.height)

                            // Detectar el texto en el nuevo bitmap utilizando una corrutina y esperar a que se complete
                            if (contourArea<maxArea) {
                                val deferredText = async(Dispatchers.Default) {

                                    detectTextFromImage(croppedBitmap)

                                }

                                val text = deferredText.await()

                                //Log.d(TAG, "El texto es $text")

                                val cellData = CellData(rect.x,rect.y, croppedBitmap, text)
                                results.add(cellData) // Agregar los datos de la celda al array
                            }
                        }
                    }
                }

                // Guardar los resultados en un archivo CSV
                saveResultsToDictionary(results, "foldercsv", "archivo")





            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("Scanerformulario", "Error en la detección de texto.")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("Scanerformulario", "Error en el procesamiento de la imagen")
            showToast("Error al procesar la imagen.", this@MainActivity)
        }
    }
    fun eliminarProyecto(context: Context, idProyecto: String, nombreProyecto: String, userId: String){
        val database = FirebaseDatabase.getInstance()
        val reference = database.getReference("proyectos").child(idProyecto)

        val builder = AlertDialog.Builder(context)
        builder.setTitle("Eliminar archivo")
        builder.setMessage("Se eliminara archivo "+nombreProyecto+".")
        builder.setPositiveButton("Aceptar") { dialog, _ ->
            reference.removeValue().addOnSuccessListener {
                // La eliminación se realizó con éxito
                Log.d("TAG", "Proyecto eliminado exitosamente.")
                showToast("El proyecto se eliminó exitosamente.", context)
            }
                .addOnFailureListener { exception ->
                    // Ocurrió un error al eliminar el proyecto
                    Log.e("TAG", "Error al eliminar el proyecto: ${exception.message}.")
                    showToast("Error al eliminar el proyecto.", context)
                }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()

    }
    private interface DialogCallback2 {
        fun onDialogSuccess(nombreProyecto: String)
        fun onDialogFailure()
    }
    private fun mostrarDialogo2(callback: DialogCallback2, context: Context) {
        val builder = AlertDialog.Builder(context)

        builder.setTitle("Datos del proyecto")

        val view = LayoutInflater.from(context).inflate(R.layout.nombre_proyecto, null)
        builder.setView(view)

        val editTextHoja = view.findViewById<EditText>(R.id.nombre_proyect_2)

        builder.setPositiveButton("Aceptar") { dialog, _ ->
            val hoja = editTextHoja.text.toString().trim()

            if (hoja.isNotEmpty()) {
                callback.onDialogSuccess(hoja)
            } else {
                callback.onDialogFailure()
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
            callback.onDialogFailure()
        }

        val dialog = builder.create()
        dialog.show()
    }

    fun descargarProyecto(context: Context, proyectoId: String, usuarioId: String, proyectoNombre: String) {
        val database = FirebaseDatabase.getInstance()

        val hojasRef = database.getReference("proyectos").child(proyectoId).child("hojas")



        hojasRef.addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val csvFilename = "${proyectoNombre}.csv"
                val csvFile = File(downloadsFolder, csvFilename)



                try {
                    val writer = FileWriter(csvFile)

                    var cont = 5
                    try {
                        for (hojaSnapshot in dataSnapshot.children) {
                            val hojaData = hojaSnapshot.value as? List<List<Any>>

                            if (hojaData != null) {
                                val numColumns = hojaData[0].size

                                for (columnIndex in 0 until numColumns) {
                                    for (rowData in hojaData) {
                                        if (columnIndex < rowData.size) {
                                            val columnValue = rowData[columnIndex]
                                            writer.write(columnValue.toString())
                                            writer.write(",")
                                        } else {
                                            writer.write(",")
                                        }
                                    }
                                    writer.write("\n")
                                }
                            } else {
                                Log.d("TAG", "Hoja: ${hojaSnapshot.key} no contiene datos válidos.")
                                showToast(
                                    "La hoja: ${hojaSnapshot.key} no contiene datos validos.",
                                    context
                                )
                            }
                            cont += 1
                        }
                    }catch (e: Exception) {
                        showToast("No se pueden procesar todos los datos.", context)
                    }

                    writer.close()
                    Log.d("TAG", "Archivo CSV guardado exitosamente en descargas.")
                    showToast("Archivo ${proyectoNombre} guardado exitosamente.", context)
                } catch (e: IOException) {
                    e.printStackTrace()
                    Log.e("TAG", "Error al guardar el archivo CSV")
                    showToast("Error al descargar ${proyectoNombre}.", context)
                }

                Log.d("TAG", "Hemos terminado")
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("TAG", "Error al obtener las hojas del proyecto: ${databaseError.message}")
            }
        })
    }

    fun generarReportesHojas(context: Context, idProyecto: String, usuarioId: String, proyectoNombre: String) {
        val database = FirebaseDatabase.getInstance()
        val hojasRef = database.getReference("proyectos").child(idProyecto).child("hojas")

        hojasRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val downloadsFolder =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

                // Crear archivos CSV para las listas con "Sin texto" y sin "Sin texto"
                val csvFilenameWithSinTexto = "${proyectoNombre}_reporte_Sin_Firma.csv"
                val csvFileWithSinTexto = File(downloadsFolder, csvFilenameWithSinTexto)

                val csvFilenameWithoutSinTexto = "${proyectoNombre}_reporte_Con_Firma.csv"
                val csvFileWithoutSinTexto = File(downloadsFolder, csvFilenameWithoutSinTexto)

                try {
                    val writerWithSinTexto = FileWriter(csvFileWithSinTexto)
                    val writerWithoutSinTexto = FileWriter(csvFileWithoutSinTexto)

                    val listaDeListas: MutableList<MutableList<String>> = mutableListOf()

                    try {

                        for (hojaSnapshot in dataSnapshot.children) {
                            val hojaData = hojaSnapshot.value as? List<List<Any>>
                            if (hojaData != null) {
                                val numColumns = hojaData[0].size
                                for (columnIndex in 0 until numColumns) {
                                    val listaDatos: MutableList<String> = mutableListOf()
                                    for (rowData in hojaData) {
                                        if (columnIndex < rowData.size) {
                                            val columnValue = rowData[columnIndex]
                                            listaDatos.add(columnValue.toString())
                                        } else {
                                            listaDatos.add("")
                                        }
                                    }
                                    listaDeListas.add(listaDatos)
                                }
                            } else {
                                Log.d("TAG", "Hoja: ${hojaSnapshot.key} no contiene datos válidos.")
                                showToast(
                                    "La hoja: ${hojaSnapshot.key} no contiene datos válidos.",
                                    context
                                )
                            }
                        }
                    }catch (e: Exception) {
                        showToast("No se pudieron procesar todos los datos", context)
                    }

                    // Escribir en los archivos CSV correspondientes
                    try {
                        for (listaDatos in listaDeListas) {
                            val hasSinTexto =
                                listaDatos[5] == "Firma" && listaDatos[6] == "Firma"
                            if (!hasSinTexto && !listaDatos[0].startsWith("NRC")) {
                                writerWithSinTexto.write(listaDatos.joinToString(","))
                                writerWithSinTexto.write("\n")
                            } else if (!listaDatos[0].startsWith("NRC")) {
                                writerWithoutSinTexto.write(listaDatos.joinToString(","))
                                writerWithoutSinTexto.write("\n")
                            }
                        }
                    }catch (e: Exception){
                        showToast("No se pudieron escribir todos los datos", context)
                    }

                    writerWithSinTexto.close()
                    writerWithoutSinTexto.close()

                    Log.d("TAG", "Archivo CSV con 'Sin texto' guardado exitosamente en ${csvFileWithSinTexto.absolutePath}")
                    Log.d("TAG", "Archivo CSV sin 'Sin texto' guardado exitosamente en ${csvFileWithoutSinTexto.absolutePath}")
                    showToast("Archivos CSV guardados exitosamente en descargas.", context)
                } catch (e: IOException) {
                    e.printStackTrace()
                    Log.e("TAG", "Error al guardar el archivo CSV")
                    showToast("Error al descargar ${proyectoNombre}.", context)
                }

                Log.d("TAG", "Hemos terminado")
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("TAG", "Error al obtener las hojas del proyecto: ${databaseError.message}")
            }
        })
    }

    fun cambiarnombre(context: Context, idProyecto: String, usuarioid: String) {
        val database = FirebaseDatabase.getInstance()
        val proyectosRef = database.getReference("proyectos")

        // Obtener la referencia del proyecto
        val proyectoRef = proyectosRef.child(idProyecto)

        // Obtener la referencia de los proyectos del usuario
        val proyectosUsuarioRef = proyectosRef.orderByChild("idUsuario").equalTo(usuarioid)

        // Obtener el nombre actual del proyecto
        proyectoRef.child("nombreProyecto").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val nombreProyectoActual = dataSnapshot.getValue(String::class.java)

                // Verificar si el nuevo nombre del proyecto ya existe para el usuarioid
                proyectosUsuarioRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val proyectosUsuario = dataSnapshot.children.mapNotNull { it.child("nombreProyecto").getValue(String::class.java) }

                        var nuevoNombre = nombreProyectoActual ?: ""
                        var numero = 1

                        while (proyectosUsuario.contains(nuevoNombre)) {
                            nuevoNombre = "$nombreProyectoActual $numero"
                            numero++
                        }

                        val dialogCallback = object : MainActivity.DialogCallback2 {
                            override fun onDialogSuccess(nombreProyectoNuevo: String) {
                                // Verificar si el nombre ya existe en los proyectos del usuario
                                var nombreProyectoFinal = nombreProyectoNuevo
                                var bandera = false
                                while (proyectosUsuario.contains(nombreProyectoFinal)) {
                                    nombreProyectoFinal = "$nombreProyectoNuevo $numero"
                                    numero++
                                    bandera = true
                                }

                                if(bandera){
                                    showToast("Ese nombre ya existe, así que se actualizó a ${nombreProyectoFinal}.", context)
                                }

                                // Actualizar el nombre del proyecto en la base de datos
                                proyectoRef.child("nombreProyecto").setValue(nombreProyectoFinal)
                                    .addOnSuccessListener {
                                        // El cambio de nombre se realizó con éxito
                                        Log.d("TAG", "Nombre de proyecto cambiado exitosamente")
                                    }
                                    .addOnFailureListener { exception ->
                                        // Ocurrió un error al cambiar el nombre del proyecto
                                        Log.e("TAG", "Error al cambiar el nombre del proyecto: ${exception.message}")
                                    }
                            }

                            override fun onDialogFailure() {
                                val builder = AlertDialog.Builder(context)
                                builder.setTitle("Datos incompletos")
                                builder.setMessage("Los datos ingresados son nulos. El proyecto no se actualizará.")
                                builder.setPositiveButton("Aceptar") { dialog, _ ->
                                    dialog.dismiss()
                                }
                                val dialog = builder.create()
                                dialog.show()
                            }
                        }

                        mostrarDialogo2(dialogCallback, context)
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Log.d("TAG", "Error al obtener los proyectos del usuario: ${databaseError.message}.")
                    }
                })
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.d("TAG", "Error al obtener el nombre actual del proyecto: ${databaseError.message}")
            }
        })
    }

    private interface DialogCallback {
        fun onDialogSuccess(nombreProyecto: String, nombreHoja: String)
        fun onDialogFailure()
    }
    private fun mostrarDialogo(callback: DialogCallback) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Datos del proyecto")

        val view = layoutInflater.inflate(R.layout.layout_dialogo_nombre, null)
        builder.setView(view)

        val editTextHoja = view.findViewById<EditText>(R.id.nombre_hoja)
        val editTextProyecto = view.findViewById<EditText>(R.id.nombre_formulario)

        builder.setPositiveButton("Aceptar") { dialog, _ ->
            val hoja = editTextHoja.text.toString().trim()
            val proyecto = editTextProyecto.text.toString().trim()

            if (hoja.isNotEmpty() && proyecto.isNotEmpty()) {
                callback.onDialogSuccess(proyecto, hoja)
            } else {
                callback.onDialogFailure()
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
            callback.onDialogFailure()
        }

        val dialog = builder.create()
        dialog.show()
    }
    private fun saveResultsToDictionary(results: List<CellData>, folderName: String, fileName: String): Map<Int, List<String>> {
        val columnData: MutableMap<Int, MutableList<CellData>> = mutableMapOf()
        var previousX = -1

        for (result in results) {
            Log.d("TAG", "result es: $result")
            val columnIndex = if (previousX == -1 || result.x - previousX > 40) {
                // Crear una nueva columna si no hay una previa o si la diferencia es mayor a 40
                columnData.size
            } else {
                // Utilizar la columna previa si la diferencia es menor o igual a 40
                columnData.size - 1
            }

            if (!columnData.containsKey(columnIndex)) {
                columnData[columnIndex] = mutableListOf()
            }
            columnData[columnIndex]?.add(result)

            previousX = result.x
        }

        // Ordenar los elementos de cada fila de menor a mayor según result.y
        val sortedColumnData: MutableMap<Int, List<String>> = mutableMapOf()
        for ((columnIndex, columnResults) in columnData) {
            sortedColumnData[columnIndex] = columnResults.sortedBy { result ->
                result.y
            }.map { it.result }
        }

        // Obtener el primer resultado de la primera lista
        val firstResult = sortedColumnData[0]?.firstOrNull()

        // Crear una lista aparte con el primer resultado
        val firstResultList = if (firstResult != null) {
            listOf(firstResult)
        } else {
            emptyList()
        }

        // Eliminar el primer resultado de la primera lista
        sortedColumnData[0] = sortedColumnData[0]?.drop(1) ?: emptyList()

        Log.d("TAG", "Diccionario: $sortedColumnData")
        val userId = intent.getStringExtra("userId")

        val dialogCallback = object : DialogCallback {
            override fun onDialogSuccess(nombreProyecto: String, nombreHoja: String) {
                crearNuevoProyecto(nombreProyecto, userId.toString(), nombreHoja, sortedColumnData)
            }

            override fun onDialogFailure() {
                val builder = AlertDialog.Builder(this@MainActivity)
                builder.setTitle("Datos incompletos")
                builder.setMessage("Los datos ingresados son nulos. El proyecto no se guardará.")
                builder.setPositiveButton("Aceptar") { dialog, _ ->
                    dialog.dismiss()
                }
                val dialog = builder.create()
                dialog.show()
            }
        }

        mostrarDialogo(dialogCallback)

        return sortedColumnData
    }

    fun crearNuevoProyecto(nombreProyecto: String, idUsuario: String, nombreHoja: String, json: MutableMap<Int, List<String>> = mutableMapOf()) {
        val database = FirebaseDatabase.getInstance()
        val proyectosRef = database.getReference("proyectos")

        // Obtener la referencia de los proyectos del usuario
        val proyectosUsuarioRef = proyectosRef.orderByChild("idUsuario").equalTo(idUsuario)

        // Verificar si el nuevo nombre del proyecto ya existe para el idUsuario
        proyectosUsuarioRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val proyectosUsuario = dataSnapshot.children.mapNotNull { it.child("nombreProyecto").getValue(String::class.java) }

                var nuevoNombreProyecto = nombreProyecto
                var numero = 1

                while (proyectosUsuario.contains(nuevoNombreProyecto)) {
                    nuevoNombreProyecto = "$nombreProyecto $numero"
                    numero++
                }

                // Crear un nuevo objeto para el proyecto
                val nuevoProyecto = proyectosRef.push()
                val convertedJson = json.mapKeys { it.key.toString() }

                // Crear un mapa con los datos del proyecto
                val datosHoja = hashMapOf(
                    nombreHoja to convertedJson
                )

                val datosProyecto = hashMapOf(
                    "nombreProyecto" to nuevoNombreProyecto,
                    "idUsuario" to idUsuario,
                    "hojas" to datosHoja
                )

                // Guardar los datos del proyecto en la base de datos
                nuevoProyecto.setValue(datosProyecto)
                    .addOnSuccessListener {
                        // El proyecto se guardó correctamente
                        println("Nuevo proyecto creado correctamente")
                        obtenerProyectosUsuario(userId) { proyectos ->
                            proyectoAdapter.actualizarProyectos(proyectos)
                        }
                    }
                    .addOnFailureListener { e ->
                        // Ocurrió un error al guardar el proyecto
                        println("Error al crear el proyecto: ${e.message}")
                    }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.d("TAG", "Error al obtener los proyectos del usuario: ${databaseError.message}.")
            }
        })
    }

    private fun detectTextFromImage(bitmap: Bitmap): String {
        val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        val originalWidth = bitmap.width
        val originalHeight = bitmap.height

        var resizedBitmap = bitmap



        // Verificar el tamaño del bitmap y redimensionarlo si es necesario
        if (originalWidth < 32 || originalHeight < 32) {
            val newWidth = maxOf(originalWidth, 32)
            val newHeight = maxOf(originalHeight, 32)
            resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        }

        val inputImage = InputImage.fromBitmap(resizedBitmap, 0)

        val visionText = Tasks.await(textRecognizer.process(inputImage))

        val stringBuilder = StringBuilder()

        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                stringBuilder.append(line.text)
            }
        }

        val result = stringBuilder.toString()

        if (result.isNullOrEmpty()) {
            // Si no hay texto ni trazos significativos, devuelve "Sin texto"
            val result = detectSignatureOrDoodle(bitmap)

            //val threshold = 30

            //val result = detectContrast(bitmap, threshold)
            return result


        }

        return result
    }

    private fun detectSignatureOrDoodle(bitmap: Bitmap): String {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        // Convertir a escala de grises
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY)

        // Aplicar umbralización adaptativa con un umbral negativo para aumentar la sensibilidad
        val thresh = Mat()
        Imgproc.adaptiveThreshold(
            mat,
            thresh,
            255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY_INV,
            15,
            10.0
        )

        // Detección de contornos
        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(
            thresh,
            contours,
            hierarchy,
            Imgproc.RETR_EXTERNAL,
            Imgproc.CHAIN_APPROX_SIMPLE
        )

        for (contour in contours) {
            // Calcular el área del contorno
            val area = Imgproc.contourArea(contour)

            // Ajustar un rectángulo alrededor del contorno
            val rect = Imgproc.boundingRect(contour)

            // Verificar si el área y la relación de aspecto cumplen con los criterios de una firma o un garabato
            val aspectRatio = rect.width.toDouble() / rect.height.toDouble()
            val aspectRatioThreshold = 0.9
            val areaThreshold = 500.0

            if (area > areaThreshold && aspectRatio > aspectRatioThreshold) {
                return "Firma"
            }
        }

        return "Sin texto"
    }
    private fun obtenerProyectosUsuario(userId: String, callback: (List<Proyecto>) -> Unit) {
        Log.d("TAG", "Antes de actualizar proyectos en obtenerProyectosUsuario")
        val database = FirebaseDatabase.getInstance()
        val reference = database.getReference("proyectos")

        reference.orderByChild("idUsuario").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val proyectos: MutableList<Proyecto> = mutableListOf()

                    for (proyectoSnapshot in snapshot.children) {
                        val key = proyectoSnapshot.key
                        val nombre = proyectoSnapshot.child("nombreProyecto").getValue(String::class.java)
                        if (key != null && nombre != null) {
                            val proyecto = Proyecto(key, nombre)
                            proyectos.add(proyecto)
                        }
                    }
                    Log.d("TAG", "Antes de actualizar proyectos en obtenerProyectosUsuario callback")
                    callback(proyectos)
                    Log.d("TAG", "Después de actualizar proyectos en obtenerProyectosUsuario callback")

                    // Actualizar los proyectos en el adaptador
                    proyectoAdapter.actualizarProyectos(proyectos)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error al obtener los proyectos: ${error.message}")
                }
            })
        Log.d("TAG", "Después de actualizar proyectos en obtenerProyectosUsuario finalizar")
    }
    private fun obtenerProyectosUsuario2(userId: String, callback: (List<Proyecto>) -> Unit) {
        val database = FirebaseDatabase.getInstance()
        val reference = database.getReference("proyectos")

        reference.orderByChild("idUsuario").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val proyectos: MutableList<Proyecto> = mutableListOf()

                    for (proyectoSnapshot in snapshot.children) {
                        val key = proyectoSnapshot.key
                        val nombre = proyectoSnapshot.child("nombreProyecto").getValue(String::class.java)
                        if (key != null && nombre != null) {
                            val proyecto = Proyecto(key, nombre)
                            proyectos.add(proyecto)
                        }
                    }

                    callback(proyectos)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error al obtener los proyectos: ${error.message}")
                }
            })
    }
    private fun searchProjects(query: String) {
        // Obtén el ID del usuario actual
        obtenerProyectosUsuario2(userId) { proyectos ->
            val proyectosFiltrados = proyectos.filter { proyecto ->
                proyecto.nombre.contains(query, ignoreCase = true)
            }
            proyectoAdapter.actualizarProyectos(proyectosFiltrados)
        }
    }
    private fun getPermission() {
        val permission = Manifest.permission.CAMERA
        val grant = PackageManager.PERMISSION_GRANTED
        if (checkSelfPermission(permission) != grant) {
            requestPermissions(arrayOf(permission, Manifest.permission.MANAGE_EXTERNAL_STORAGE), 102)
        }
    }
    @Throws(IOException::class)
    private fun createImageFile(): File? {
        var imageFile: File? = null

        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        imageFile = File.createTempFile(imageFileName, ".jpg", storageDir)
        return imageFile
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 102 && grantResults.isNotEmpty()) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                getPermission()
            }
        }
    }

}