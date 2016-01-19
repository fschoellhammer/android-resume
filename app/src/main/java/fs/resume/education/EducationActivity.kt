package fs.resume.education

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import dagger.Component
import fs.resume.App
import fs.resume.AppComponent
import fs.resume.R
import fs.resume.util.PerActivity


class EducationActivity : AppCompatActivity() {

    @PerActivity
    @Component(dependencies = arrayOf(AppComponent::class))
    interface ActivityComponent {
        fun injectActivity(activity : EducationActivity)
    }

    init {
        DaggerEducationActivity_ActivityComponent.builder()
                .appComponent(App.component)
                .build().injectActivity(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.education_layout)


    }
}
