package com.example.scanerformulario

import android.app.AlertDialog
import android.content.Context
import android.text.InputType
import android.util.Log
import android.view.*
import android.widget.*
import androidx.recyclerview.widget.RecyclerView

class HojaAdapter(private val context: Context, private val proyectoId: String) : RecyclerView.Adapter<HojaAdapter.HojaViewHolder>() {

    private var hojas: MutableList<HojasActivity.Hoja> = mutableListOf()
    private var onMenuItemClickListener: ((MenuItem, String) -> Unit)? = null
    private var selectedItem: HojasActivity.Hoja? = null

    interface HojaAdapterListener {
        fun onRenombrarClicked(nombreHoja: String)
    }
    private var listener: HojaAdapterListener? = null

    fun setListener(listener: HojaAdapterListener) {
        this.listener = listener
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HojaViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.hojas, parent, false)
        return HojaViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: HojaViewHolder, position: Int) {
        val hoja = hojas[position]
        holder.bind(hoja.nombre)

    }

    fun setSelectedItem(position: Int) {
        selectedItem = hojas[position]
    }

    fun getItem(position: Int): HojasActivity.Hoja {
        return hojas[position]
    }

    override fun getItemCount(): Int {
        return hojas.size
    }

    fun actualizarProyectos(nuevasHojas: List<HojasActivity.Hoja>) {
        hojas.clear()
        hojas.addAll(nuevasHojas)
        notifyDataSetChanged()
    }

    fun setOnMenuItemClickListener(listener: (MenuItem, String) -> Unit) {
        onMenuItemClickListener = listener
    }

    inner class HojaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(nombreHoja: String) {
            val nombreProyectoTextView: TextView = itemView.findViewById(R.id.nombreHojaTextView)
            nombreProyectoTextView.text = nombreHoja

            itemView.setOnLongClickListener { view ->
                setSelectedItem(adapterPosition) // Seleccionar el elemento correspondiente a la posición
                showContextMenu(view) // Mostrar el menú contextual
                true
            }
        }

        fun showContextMenu(view: View) {
            val popupMenu = PopupMenu(context, view)
            popupMenu.inflate(R.menu.menu_contextual)
            popupMenu.setOnMenuItemClickListener { menuItem ->
                val selectedItem = selectedItem ?: return@setOnMenuItemClickListener false
                val hojaNombre = selectedItem.nombre
                onMenuItemClickListener?.invoke(menuItem, hojaNombre)

                handleMenuItemClick(menuItem, hojaNombre)

                true
            }
            popupMenu.show()

        }


         fun handleMenuItemClick(menuItem: MenuItem, hojaNombre: String) {
            when (menuItem.itemId) {
                R.id.menu_eliminar-> {
                    // Acción para el segundo elemento del menú
                    Log.d("TAG", "Acción para el elemento eliminar: $hojaNombre")
                    val otraClase = HojasActivity()
                    otraClase.eliminarHoja(context, hojaNombre, proyectoId)

                }
                R.id.menu_descargar-> {
                    // Acción para el segundo elemento del menú
                    Log.d("TAG", "Acción para el elemento descargar: $hojaNombre")
                    val otraClase = HojasActivity()
                    otraClase.descargarHoja(context, hojaNombre, proyectoId)
                }
                R.id.menu_cambiar_nombre-> {
                    // Acción para el segundo elemento del menú
                    Log.d("TAG", "Acción para el elemento renombrer: $hojaNombre")
                    val otraClase = HojasActivity()
                    otraClase.renombrar(context, hojaNombre, proyectoId)
                }

                R.id.menu_reporte-> {
                    Log.d("TAG", "Acción para generar reporte: $hojaNombre")
                    val otraClase = HojasActivity()
                    otraClase.generarReporte(context, hojaNombre, proyectoId)
                }


            }
        }

    }

}








