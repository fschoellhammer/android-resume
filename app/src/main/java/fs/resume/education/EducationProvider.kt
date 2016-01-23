package fs.resume.education

import android.support.annotation.DrawableRes
import org.threeten.bp.LocalDate

interface EducationProvider {
    /**
     * Get list of education entries
     */
    fun getEducation() : List<Education>
}


//fun EducationProvider(handler: () -> List<Education>): EducationProvider = object : EducationProvider {
//    override fun getEducation() = handler()
//}

data class Education (
    val institution : String,
    val description : String,
    val major : String = "",
    val location : String,

    val from : LocalDate,
    val to : LocalDate,

    @DrawableRes
    val icon : Int
): Comparable<Education> {

    override fun compareTo(other: Education): Int = when {
        from != this.from -> from.compareTo(other.from)
        else -> to.compareTo(other.to)
    }
}