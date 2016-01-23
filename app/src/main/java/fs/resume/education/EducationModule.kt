package fs.resume.education

import android.content.Context
import dagger.Module
import dagger.Provides
import fs.resume.R
import fs.resume.util.PerActivity
import org.threeten.bp.LocalDate

@Module
class EducationModule {

    @PerActivity @Provides
    fun getEducationProvider(ctx : Context) : EducationProvider {
        return object : EducationProvider {
            override fun getEducation() = getEducationList(ctx)
        }
    }

    private fun getEducationList(ctx : Context) : List<Education> {
        fun date(year : Int, month : Int) : LocalDate = LocalDate.of(year, month, 1)

        with(ctx) {
            return listOf(
                    Education(
                            institution = getString(R.string.edu_tu),
                            description = "Media Informatics",
                            from = date(2003, 9),
                            to = date(2006, 7),
                            icon = R.drawable.ic_tu),
                    Education(
                            institution = getString(R.string.edu_waseda),
                            description = "Intensive Japanese Language Program",
                            from = date(2006, 9),
                            to = date(2007, 7),
                            icon = R.drawable.ic_waseda),
                    Education(
                            institution = getString(R.string.edu_tu),
                            description = "Software Engineering &amp; Internet Computing",
                            from = date(2007, 9),
                            to = date(2009, 7),
                            icon = R.drawable.ic_tu),
                    Education(
                            institution = getString(R.string.edu_southeast),
                            description = "Intensive Chinese Language Program",
                            from = date(2009, 9),
                            to = date(2010, 7),
                            icon = R.drawable.ic_southeast)
            )
        }
    }
}

