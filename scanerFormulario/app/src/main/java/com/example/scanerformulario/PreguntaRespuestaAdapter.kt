package com.example.scanerformulario

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.TextView

class PreguntaRespuestaAdapter(
    private val context: Context,
    private val preguntasRespuestas: List<PreguntaRespuesta>
) : BaseExpandableListAdapter() {
    override fun getGroupCount(): Int {
        return preguntasRespuestas.size
    }

    override fun getChildrenCount(groupPosition: Int): Int {
        return 1 // Cada pregunta tiene una respuesta
    }

    override fun getGroup(groupPosition: Int): Any {
        return preguntasRespuestas[groupPosition].pregunta
    }

    override fun getChild(groupPosition: Int, childPosition: Int): Any {
        return preguntasRespuestas[groupPosition].respuesta
    }

    override fun getGroupId(groupPosition: Int): Long {
        return groupPosition.toLong()
    }

    override fun getChildId(groupPosition: Int, childPosition: Int): Long {
        return childPosition.toLong()
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
        return false
    }

    override fun getGroupView(
        groupPosition: Int,
        isExpanded: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        var view = convertView
        if (view == null) {
            val inflater =
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            view = inflater.inflate(R.layout.item_pregunta, null)
        }

        // Obtiene la pregunta actual
        val pregunta = getGroup(groupPosition) as String

        // Actualiza la vista con la pregunta
        val preguntaTextView = view!!.findViewById<TextView>(R.id.preguntaTextView)
        preguntaTextView.text = pregunta

        return view
    }

    override fun getChildView(
        groupPosition: Int,
        childPosition: Int,
        isLastChild: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        var view = convertView
        if (view == null) {
            val inflater =
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            view = inflater.inflate(R.layout.item_respuesta, null)
        }

        // Obtiene la respuesta actual
        val respuesta = getChild(groupPosition, childPosition) as String

        // Actualiza la vista con la respuesta
        val respuestaTextView = view!!.findViewById<TextView>(R.id.respuestaTextView)
        respuestaTextView.text = respuesta

        return view
    }
}
