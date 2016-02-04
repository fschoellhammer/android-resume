package fs.resume.timeline

import android.os.Bundle
import dagger.Component
import fs.resume.App
import fs.resume.AppComponent
import fs.resume.R
import fs.resume.util.BaseDrawerActivity
import fs.resume.util.PerActivity
import kotlinx.android.synthetic.main.timeline_activity.*
import org.threeten.bp.LocalDate

class TimelineActivity : BaseDrawerActivity() {

    override val selectedMenuId = R.id.menu_timeline


    @PerActivity
    @Component(dependencies = arrayOf(AppComponent::class))
    interface ActivityComponent {
        fun injectActivity(activity : TimelineActivity)
    }

    init {
//        DaggerTimelineActivity_ActivityComponent.builder()
//                .appComponent(App.component)
//                .build().injectActivity(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.timeline_activity)

        timeline.events = listOf(
                TimelineEvent(LocalDate.of(2003, 1, 1), "Uni Wien", R.drawable.ic_uni, 0f, -30f),
                TimelineEvent(LocalDate.of(2004, 2, 1), "TU Wien", R.drawable.ic_tu, 100f, 30f),
                TimelineEvent(LocalDate.of(2005, 9, 1), "Waseda", R.drawable.ic_waseda, 180f, -40f),
                TimelineEvent(LocalDate.of(2008, 7, 1), "China", R.drawable.ic_southeast, 300f, 60f),
                TimelineEvent(LocalDate.of(2011, 6, 1), "S-Can", R.drawable.ic_scan, 380f, -10f),
                TimelineEvent(LocalDate.of(2013, 1, 1), "Rakuten", R.drawable.ic_rakuten, 520f, -50f),
                TimelineEvent(LocalDate.of(2015, 2, 1), "Solarier", R.drawable.ic_solarier, 640f, 20f))

        timeline.onEventSelected = { onEventSelected(it)}
    }

    private fun onEventSelected(event: TimelineEvent) {

    }
}

