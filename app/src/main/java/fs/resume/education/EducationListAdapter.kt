package fs.resume.education

import android.content.Context
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import fs.resume.R
import fs.resume.util.inflate
import fs.resume.util.setCompoundDrawableTint
import fs.resume.util.visible
import kotlinx.android.synthetic.main.education_tile.view.*
import org.threeten.bp.format.DateTimeFormatter

/**
 * Created by Administrator on 2016-01-23.
 */
class EducationListAdapter(val ctx : Context, val items : List<Education>) : RecyclerView.Adapter<EducationListAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = parent.inflate(R.layout.education_tile)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size


    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val ctx = view.context!!
        private val format = DateTimeFormatter.ofPattern(ctx.getString(R.string.format_yearmonth))

        fun bind(item : Education) {
            itemView.title.text = item.institution
            itemView.description.text = item.description
            itemView.major.text = item.major
            itemView.major.visible = !item.major.isEmpty()
            itemView.location.text = item.location
            itemView.date.text = ctx.getString(R.string.format_date_range, format.format(item.from), format.format(item.to))
            itemView.icon.setImageResource(item.icon)
            for (tv in arrayOf(itemView.major, itemView.location, itemView.date)) {
                tv.setCompoundDrawableTint(ctx.getColor(R.color.textColorHint))
            }
        }
    }
}