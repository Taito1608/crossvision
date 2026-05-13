package com.example.solution_development

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class CapturedImagesAdapter(
    private val items: MutableList<ByteArray>,
    private val onClick: (index: Int) -> Unit
) : RecyclerView.Adapter<CapturedImagesAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.ivCaptured)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_captured_image, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val bytes = items[position]
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        holder.imageView.setImageBitmap(bmp)
        holder.itemView.setOnClickListener { onClick(position) }
    }

    override fun getItemCount(): Int = items.size

    fun addAtFront(bytes: ByteArray) {
        items.add(0, bytes)
        notifyItemInserted(0)
    }
}
