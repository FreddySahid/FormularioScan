package com.example.scanerformulario
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ProyectoAdapter(private val context: Context,  private val usuarioId: String) : RecyclerView.Adapter<ProyectoAdapter.ProyectoViewHolder>() {
    private var proyectos: MutableList<MainActivity.Proyecto> = mutableListOf()
    private var proyectosFiltrados: List<MainActivity.Proyecto> = emptyList()
    private var onItemClickListener: ((MainActivity.Proyecto) -> Unit)? = null





    private var onMenuItemClickListener: ((MenuItem, String) -> Unit)? = null
    private var selectedItem: MainActivity.Proyecto? = null

    interface ProyectoAdapterListener {
        fun onRenombrarClicked(nombreHoja: String)
    }
    private var listener: ProyectoAdapterListener? = null

    fun setListener(listener: ProyectoAdapterListener) {
        this.listener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProyectoViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_proyecto, parent, false)
        return ProyectoViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ProyectoViewHolder, position: Int) {
        val proyecto = proyectos[position]
        holder.bind(proyecto.nombre)

        holder.itemView.setOnClickListener {
            // Abrir el Activity "Hojas" y pasar el ID del proyecto
            val intent = Intent(context, HojasActivity::class.java)
            intent.putExtra("proyecto_id", proyecto.id)
            intent.putExtra("proyecto_nombre", proyecto.nombre)
            context.startActivity(intent)
        }
    }
    fun setOnItemClickListener(listener: (MainActivity.Proyecto) -> Unit) {
        onItemClickListener = listener
    }
    fun setSelectedItem(position: Int) {
        selectedItem = proyectos[position]
    }

    override fun getItemCount(): Int {
        return proyectos.size
    }

    fun actualizarProyectos(nuevosProyectos: List<MainActivity.Proyecto>) {

        proyectos.clear()
        proyectos.addAll(nuevosProyectos)
        notifyDataSetChanged()
    }

    fun setOnMenuItemClickListener(listener: (MenuItem, String) -> Unit) {
        onMenuItemClickListener = listener
    }



    inner class ProyectoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(nombreProyecto: String) {
            val nombreProyectoTextView: TextView = itemView.findViewById(R.id.nombreProyectoTextView)
            nombreProyectoTextView.text = nombreProyecto

            itemView.setOnClickListener {
                val proyecto = proyectos[adapterPosition]
                onItemClickListener?.invoke(proyecto)
            }
            itemView.setOnLongClickListener { view ->
                setSelectedItem(adapterPosition) // Seleccionar el elemento correspondiente a la posición
                showContextMenu(view) // Mostrar el menú contextual
                true
            }
        }

        fun showContextMenu(view: View) {
            val popupMenu = PopupMenu(context, view)
            popupMenu.inflate(R.menu.menu_poyect)
            popupMenu.setOnMenuItemClickListener { menuItem ->
                val selectedItem = selectedItem ?: return@setOnMenuItemClickListener false
                val hojaNombre = selectedItem.nombre
                val idProyect = selectedItem.id
                onMenuItemClickListener?.invoke(menuItem, hojaNombre)

                handleMenuItemClick(menuItem, hojaNombre, idProyect, usuarioId)

                true
            }
            popupMenu.show()


        }



        fun handleMenuItemClick(menuItem: MenuItem, proyectoNombre: String, idProyecto: String, usuarioId: String) {
            when (menuItem.itemId) {
                R.id.menu_eliminar_proyect-> {
                    // Acción para el segundo elemento del menú
                    Log.d("TAG", "Acción para el elemento eliminar: $proyectoNombre")
                    val otraClase = MainActivity()
                    otraClase.eliminarProyecto(context, idProyecto, proyectoNombre, usuarioId)
                }

                R.id.menu_cambiar_nombre_proyect-> {
                    // Acción para el segundo elemento del menú
                    Log.d("TAG", "Acción para el elemento renombrer: $proyectoNombre")

                    val otraClase = MainActivity()
                    otraClase.cambiarnombre(context, idProyecto, usuarioId)

                }

                R.id.menu_descargar_proyect -> {   // Acción para el segundo elemento del menú
                    Log.d("TAG", "Acción para el elemento renombrer: $proyectoNombre")

                    val otraClase = MainActivity()
                    otraClase.descargarProyecto(context, idProyecto, usuarioId, proyectoNombre)

                }
                R.id.menu_descargar_reporte -> {   // Acción para el segundo elemento del menú
                    Log.d("TAG", "Acción para el elemento renombrer: $proyectoNombre")

                    val otraClase = MainActivity()
                    otraClase.generarReportesHojas(context, idProyecto, usuarioId, proyectoNombre)

                }


            }
        }
    }
}