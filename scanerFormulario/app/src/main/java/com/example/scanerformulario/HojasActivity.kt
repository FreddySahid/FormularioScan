package com.example.scanerformulario

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.text.InputType
import android.util.Log
import android.view.*
import android.widget.*
import android.widget.AdapterView.AdapterContextMenuInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.digitalink.Ink
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.imgproc.Imgproc.*
import java.io.*
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*



class HojasActivity : AppCompatActivity() , View.OnCreateContextMenuListener{
    lateinit var camara: ImageButton
    lateinit var select: ImageButton


    lateinit var bitmap: Bitmap
    var SELECT_CODE = 100
    var CAMERA_CODE = 101
    lateinit var mat: Mat
    private lateinit var imageUri: Uri
    lateinit var photoFile: File
    lateinit var path: String
    private lateinit var hojaAdapter: HojaAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var ProyectoId: String
    private lateinit var proyectoNombre: String
    private lateinit var hojas: List<Hoja>
    private var selectedHoja: Hoja? = null
    lateinit var textViewNombreFormulario: TextView

    data class Hoja( val nombre: String = "")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hojas)

        if (OpenCVLoader.initDebug()) {
            Log.d("LOADED", "success")
        } else {
            Log.d("LOADED", "error")
        }

        getPermission()
        camara = findViewById(R.id.camaraHoja)
        select = findViewById(R.id.selectHoja)


        ProyectoId = intent.getStringExtra("proyecto_id") ?: ""
        proyectoNombre = intent.getStringExtra("proyecto_nombre") ?: ""

        textViewNombreFormulario = findViewById(R.id.nombreFormulario)
        textViewNombreFormulario.setText(proyectoNombre)



        recyclerView = findViewById(R.id.recyclerViewHojas)
        hojaAdapter = HojaAdapter(this@HojasActivity, ProyectoId)

        recyclerView.apply {
            layoutManager = GridLayoutManager(this@HojasActivity, 2)
            adapter = hojaAdapter
        }

        recyclerView.setOnCreateContextMenuListener(this)
        registerForContextMenu(recyclerView)


        // Obtener los proyectos del usuario y actualizar el adaptador
        obtenerHojasProyecto(ProyectoId) { hoja ->
            hojaAdapter.actualizarProyectos(hoja)
        }

        val database = FirebaseDatabase.getInstance()
        val reference = database.getReference("proyectos").child(ProyectoId)
        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                Log.d("TAG", "No hubo ningun error")
                obtenerHojasProyecto(ProyectoId) { hoja ->
                    hojaAdapter.actualizarProyectos(hoja)
                }

            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.d("TAG","Hubo un error: "+databaseError)
                showToast("Hubo un error a encontrar los cambios generados", this@HojasActivity)
            }
        }

        reference.addValueEventListener(valueEventListener)

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

    }

    private fun isImageFile(uri: Uri): Boolean {
        val mimeType = contentResolver.getType(uri)
        return mimeType?.startsWith("image") == true
    }

    fun showToast(mensaje: String, context: Context){
        val durationInMillis: Long = 200000 // Duración de 5 segundos en milisegundos

        val toast = Toast.makeText(context, mensaje, Toast.LENGTH_SHORT)

        // Mostrar el Toast con la duración personalizada
        toast.show()

        // Retrasar la cancelación del Toast para lograr la duración personalizada
        Handler().postDelayed({ toast.cancel() }, durationInMillis)
    }

    fun obtenerHojasProyecto(proyectoId: String, callback: (List<HojasActivity.Hoja>) -> Unit) {
        val database = FirebaseDatabase.getInstance()
        val reference = database.getReference("proyectos").child(proyectoId).child("hojas")

        reference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val hojas: MutableList<HojasActivity.Hoja> = mutableListOf()

                for (hojaSnapshot in snapshot.children) {
                    val hojaId = hojaSnapshot.key
                    if (hojaId != null) {
                        val hoja = HojasActivity.Hoja(hojaId)
                        hojas.add(hoja)
                    }
                }

                callback(hojas)

                // Actualizar las hojas en el adaptador
                hojaAdapter.actualizarProyectos(hojas)

            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(ContentValues.TAG, "Error al obtener las hojas: ${error.message}")
            }
        })
    }

    fun eliminarHoja(context: Context, nombreHoja: String, proyectoId: String) {

        val database = FirebaseDatabase.getInstance()
        val reference = database.getReference("proyectos").child(proyectoId).child("hojas")

        val builder = AlertDialog.Builder(context)
        builder.setTitle("eliminar archivo")
        builder.setMessage("Se eliminara archivo "+nombreHoja)


        builder.setPositiveButton("Aceptar") { dialog, _ ->
            reference.child(nombreHoja).removeValue()
            dialog.dismiss()
            showToast("Archivo eliminado correctamente", context)
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
            showToast("Se cancelo la eliminación del archivo", context)
        }

        val dialog = builder.create()
        dialog.show()

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
                Log.e("TAG", "Error al leer el archivo", e)
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
                    Toast.makeText(this, "Archivo seleccionado debe ser una imagen", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show()
            }
        }
    }

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

            val results: ArrayList<MainActivity.CellData> = ArrayList() // Array para almacenar los resultados

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

                                val cellData =
                                    MainActivity.CellData(rect.x, rect.y, croppedBitmap, text)
                                results.add(cellData) // Agregar los datos de la celda al array
                            }
                        }
                    }
                }

                // Guardar los resultados en un archivo CSV
                saveResultsToDictionary(results, "foldercsv", "archivo")



                Log.d("Scanerformulario", "Proceso completado")
                for (result in results) {
                    println("Resultado: ${result.result}, Posición X: ${result.x}")
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("Scanerformulario", "Error en la detección de texto")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("Scanerformulario", "Error en el procesamiento de la imagen")
        }
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
        cvtColor(mat, mat, COLOR_RGB2GRAY)

        // Aplicar umbralización adaptativa con un umbral negativo para aumentar la sensibilidad
        val thresh = Mat()
        adaptiveThreshold(mat, thresh, 255.0, ADAPTIVE_THRESH_GAUSSIAN_C, THRESH_BINARY_INV, 15, 10.0)

        // Detección de contornos
        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
        findContours(thresh, contours, hierarchy, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE)

        for (contour in contours) {
            // Calcular el área del contorno
            val area = contourArea(contour)

            // Ajustar un rectángulo alrededor del contorno
            val rect = boundingRect(contour)

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

    private fun detectContrast(bitmap: Bitmap, threshold: Int): String {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        var maxContrast = 0
        var minContrast = 255

        for (pixel in pixels) {
            val grayscale = Color.red(pixel)

            maxContrast = maxOf(maxContrast, grayscale)
            minContrast = minOf(minContrast, grayscale)
        }

        val contrastPercentage = ((maxContrast - minContrast).toFloat() / 255.0f) * 100.0f

        return if (contrastPercentage > threshold) {
            "Firma"
        } else {
            "Sin texto"
        }
    }

    private interface DialogCallback {
        fun onDialogSuccess(nombreHoja: String)
        fun onDialogFailure()
    }

    private fun mostrarDialogo(callback: DialogCallback, context: Context) {
        val builder = AlertDialog.Builder(context)

        builder.setTitle("Datos del proyecto")

        val view = LayoutInflater.from(context).inflate(R.layout.nombre_hoja, null)
        builder.setView(view)

        val editTextHoja = view.findViewById<EditText>(R.id.nombre_hoja_2)

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

    fun renombrar(context: Context, nombreHojaAnterio: String, proyectoId: String) {

        val database = FirebaseDatabase.getInstance()
        val reference = database.getReference("proyectos").child(proyectoId).child("hojas")

        val hojaAnteriorRef = reference.child(nombreHojaAnterio)
        val dialogCallback = object : DialogCallback {
            override fun onDialogSuccess(nombreHoja: String) {

                // Obtener todas las hojas actuales de la base de datos
                reference.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val hojasActuales = dataSnapshot.children.map { it.key ?: "" }

                        var nuevoNombre = nombreHoja
                        var numero = 1
                        var bandera = false

                        // Verificar si el nombre ya existe y agregar un número si es necesario
                        while (hojasActuales.contains(nuevoNombre)) {
                            nuevoNombre = "$nombreHoja $numero"
                            numero++
                            bandera = true
                        }

                        // Renombrar y guardar en la base de datos
                        hojaAnteriorRef.removeValue()
                        reference.child(nuevoNombre).setValue(nombreHoja)
                        if (bandera){
                            showToast("El nombre $nombreHoja ya existe, así que fue actualizado por $nuevoNombre", context)
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Log.d("TAG", "Error al obtener las hojas actuales: ${databaseError.message}")
                    }
                })

            }

            override fun onDialogFailure() {
                val builder = AlertDialog.Builder(context)
                builder.setTitle("Datos incompletos")
                builder.setMessage("Los datos ingresados son nulos. El archivo no se actualizará.")
                builder.setPositiveButton("Aceptar") { dialog, _ ->
                    dialog.dismiss()
                }
                val dialog = builder.create()
                dialog.show()
            }
        }

        mostrarDialogo(dialogCallback, context)
    }

    private fun saveResultsToDictionary(results: List<MainActivity.CellData>, folderName: String, fileName: String): Map<Int, List<String>> {
        val columnData: MutableMap<Int, MutableList<MainActivity.CellData>> = mutableMapOf()
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


        val dialogCallback = object : DialogCallback {
            override fun onDialogSuccess( nombreHoja: String) {
                crearNuevoProyecto( ProyectoId, nombreHoja, sortedColumnData)
            }

            override fun onDialogFailure() {
                val builder = AlertDialog.Builder(this@HojasActivity)
                builder.setTitle("Datos incompletos")
                builder.setMessage("Los datos ingresados son nulos. El proyecto no se guardará.")
                builder.setPositiveButton("Aceptar") { dialog, _ ->
                    dialog.dismiss()
                }
                val dialog = builder.create()
                dialog.show()
            }
        }

        mostrarDialogo(dialogCallback, this)

        return sortedColumnData
    }

    fun descargarHoja(context: Context, identificadorHoja: String, proyectoId: String) {
        val database = FirebaseDatabase.getInstance()
        val reference = database.getReference("proyectos").child(proyectoId).child("hojas").child(identificadorHoja)

        reference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val hoja = dataSnapshot.value

                val downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val csvFilename = identificadorHoja + ".csv"
                val csvFile = File(downloadsFolder, csvFilename)

                try {
                    val writer = FileWriter(csvFile)

                    // Obtener el número de filas y columnas
                    val numRows = (hoja as? List<*>)?.size ?: 0
                    val numColumns = (hoja as? List<*>)?.firstOrNull()?.let { it as? List<*> }?.size ?: 0

                    // Escribir los datos en el archivo CSV en forma de columnas
                    for (columnIndex in 0 until numColumns) {
                        for (rowIndex in 0 until numRows) {
                            val row = (hoja as? List<*>)?.get(rowIndex)
                            val rowData = if (row is List<*> && columnIndex < row.size) row[columnIndex]?.toString() ?: "" else ""

                            writer.write(rowData)
                            writer.write(",")
                        }
                        writer.write("\n")
                    }

                    writer.close()
                    showToast("Archivo CSV guardado exitosamente en descargas.", context)

                    Log.d("TAG", "Archivo CSV guardado exitosamente en ${csvFile.absolutePath}")
                } catch (e: IOException) {
                    e.printStackTrace()
                    Log.e("TAG", "Error al guardar el archivo CSV")
                    showToast("Error al guardar el archivo CSV", context)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Manejar el error, si es necesario
                Log.d("TAG", "Error al conectar a la base de datos "+databaseError )
                showToast("No se puedimos descargar su archivo, intentelo más tarde", context)

            }
        })
    }

    fun generarReporte(context: Context, identificadorHoja: String, proyectoId: String) {
        val database = FirebaseDatabase.getInstance()
        val reference = database.getReference("proyectos").child(proyectoId).child("hojas").child(identificadorHoja)

        reference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val hoja = dataSnapshot.value

                val downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

                // Crear archivos CSV para las listas con "Sin texto" y sin "Sin texto"
                val csvFilenameWithSinTexto = identificadorHoja + "_reporte_Sin_Firma.csv"
                val csvFileWithSinTexto = File(downloadsFolder, csvFilenameWithSinTexto)

                val csvFilenameWithoutSinTexto = identificadorHoja + "_reporte_Con_Firma.csv"
                val csvFileWithoutSinTexto = File(downloadsFolder, csvFilenameWithoutSinTexto)

                try {

                    val outputStream2 = FileOutputStream(csvFileWithSinTexto)
                    val writerWithSinTexto = OutputStreamWriter(outputStream2, StandardCharsets.UTF_8)

                    val outputStream = FileOutputStream(csvFileWithoutSinTexto)
                    val writerWithoutSinTexto = OutputStreamWriter(outputStream, StandardCharsets.UTF_8)

                    // Obtener el número de filas y columnas
                    val numRows = (hoja as? List<*>)?.size ?: 0
                    val numColumns = (hoja as? List<*>)?.firstOrNull()?.let { it as? List<*> }?.size ?: 0

                    // Escribir los datos en los archivos CSV correspondientes
                    val columnData: MutableMap<Int, MutableList<String>> = mutableMapOf()

                    for (columnIndex in 0 until numColumns) {
                        val columnList = mutableListOf<String>()

                        for (rowIndex in 0 until numRows) {
                            val row = (hoja as? List<*>)?.get(rowIndex)
                            val rowData = if (row is List<*> && columnIndex < row.size) row[columnIndex]?.toString() ?: "" else ""

                            columnList.add(rowData)
                        }

                        // Verificar si la columna 5 o 6 contiene "Sin texto" y escribir en el archivo correspondiente
                        val hasSinTexto = columnList[5] == "Sin texto" || columnList[6] == "Sin texto"

                        if (hasSinTexto) {
                            writerWithSinTexto.write(columnList.joinToString(","))
                            writerWithSinTexto.write("\n")
                        } else {
                            writerWithoutSinTexto.write(columnList.joinToString(","))
                            writerWithoutSinTexto.write("\n")
                        }

                        columnData[columnIndex] = columnList
                    }

                    writerWithSinTexto.close()
                    writerWithoutSinTexto.close()

                    showToast("Archivos CSV guardados exitosamente en descargas.", context)

                    Log.d("TAG", "Archivo CSV con 'Sin texto' guardado exitosamente en ${csvFileWithSinTexto.absolutePath}")
                    Log.d("TAG", "Archivo CSV sin 'Sin texto' guardado exitosamente en ${csvFileWithoutSinTexto.absolutePath}")
                    Log.d("TAG", "Diccionario: $columnData")
                } catch (e: IOException) {
                    e.printStackTrace()
                    Log.e("TAG", "Error al guardar los archivos CSV")
                    showToast("Error al guardar los archivos CSV", context)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Manejar el error, si es necesario
                Log.d("TAG", "Error al conectar a la base de datos " + databaseError)
                showToast("No se pudimos descargar su archivo, intentelo más tarde", context)
            }
        })
    }

    fun crearNuevoProyecto(ProyectoId: String, nombreHoja: String, json: MutableMap<Int, List<String>> = mutableMapOf()) {
        val database = FirebaseDatabase.getInstance()
        val proyectosRef = database.getReference("proyectos")

        val proyectoRef = proyectosRef.child(ProyectoId)
        val hojasRef = proyectoRef.child("hojas")

        // Verificar si el nombre de la hoja ya existe y agregar un número si es necesario
        hojasRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val hojasActuales = dataSnapshot.children.map { it.key ?: "" }

                var nuevoNombre = nombreHoja
                var numero = 1
                var bandera = false

                while (hojasActuales.contains(nuevoNombre)) {
                    nuevoNombre = "$nombreHoja $numero"
                    numero++
                    bandera = true
                }

                if (bandera) {
                    showToast("El nombre $nombreHoja ya existe, así que fue actualizado por $nuevoNombre", this@HojasActivity )
                }

                // Crear un nuevo mapa con los datos de la hoja
                val datosHoja = hashMapOf(
                    nuevoNombre to json.mapKeys { it.key.toString() }
                )

                // Convertir datosHoja a Map<String, Any>
                val datosHojaAny: Map<String, Any> = datosHoja

                // Actualizar los datos de las hojas en el proyecto existente
                hojasRef.updateChildren(datosHojaAny)
                    .addOnSuccessListener {
                        // Los datos de la hoja se agregaron correctamente
                        println("Datos de la hoja agregados correctamente al proyecto")

                        obtenerHojasProyecto(ProyectoId) { hoja ->
                            hojaAdapter.actualizarProyectos(hoja)
                        }
                    }
                    .addOnFailureListener { e ->
                        // Ocurrió un error al agregar los datos de la hoja al proyecto
                        println("Error al agregar los datos de la hoja al proyecto: ${e.message}")
                    }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                println("Error al obtener las hojas actuales del proyecto: ${databaseError.message}")
            }
        })
    }


    //Permisos
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