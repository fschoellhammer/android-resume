package fs.resume.timeline

import android.os.Bundle
import dagger.Component
import fs.resume.App
import fs.resume.AppComponent
import fs.resume.R
import fs.resume.util.BaseDrawerActivity
import fs.resume.util.PerActivity

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
    }
}
