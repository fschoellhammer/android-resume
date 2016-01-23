package fs.resume.education

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import dagger.Component
import fs.resume.App
import fs.resume.AppComponent
import fs.resume.R
import fs.resume.util.PerActivity
import kotlinx.android.synthetic.main.education_layout.*
import javax.inject.Inject


class EducationActivity : AppCompatActivity() {

    @Inject
    lateinit var educationProvider : EducationProvider

    @PerActivity
    @Component(dependencies = arrayOf(AppComponent::class), modules = arrayOf(EducationModule::class))
    interface ActivityComponent {
        fun injectActivity(activity : EducationActivity)
    }

    init {
        DaggerEducationActivity_ActivityComponent.builder()
                .appComponent(App.component)
                .educationModule(EducationModule())
                .build().injectActivity(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.education_layout)
        recycler.adapter = EducationListAdapter(this, educationProvider.getEducation())
        recycler.layoutManager = LinearLayoutManager(this)
    }
}
