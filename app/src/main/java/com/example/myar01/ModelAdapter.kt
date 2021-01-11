package com.example.myar01

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_model.view.*

const val SELECTED_MODEL_COLOR = Color.YELLOW
const val UNSELECTED_MODEL_COLOR = Color.LTGRAY

class ModelAdapter(
    val models: List<Model>
):RecyclerView.Adapter<ModelAdapter.ModelViewHolder>() {
    var selectModel = MutableLiveData<Model>()
    private var selectModelIndex = 0

    inner class ModelViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModelViewHolder {
         val view =LayoutInflater.from(parent.context).inflate(R.layout.item_model, parent,false)
        return ModelViewHolder(view)
    }

    override fun onBindViewHolder(holder: ModelViewHolder, position: Int) {
        if (selectModelIndex == holder.layoutPosition){
            holder.itemView.setBackgroundColor(SELECTED_MODEL_COLOR)
            selectModel.value = models[holder.layoutPosition]
        }else{
            holder.itemView.setBackgroundColor(UNSELECTED_MODEL_COLOR)
        }
        holder.itemView.apply {
            ivThumbnail.setImageResource(models[position].imageResourceId)
            tvTitle.text = models[position].title

            setOnClickListener {
                selectModel(holder)
            }
        }
    }

    override fun getItemCount()= models.size

    private fun selectModel(holder: ModelViewHolder){
        if(selectModelIndex != holder.layoutPosition){
                holder.itemView.setBackgroundColor(SELECTED_MODEL_COLOR)
                notifyItemChanged(selectModelIndex)
                selectModelIndex = holder.layoutPosition
                selectModel.value = models[holder.layoutPosition]
            }
    }
}