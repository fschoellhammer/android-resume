package fs.resume

import android.content.Context
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = arrayOf(AppModule::class))
interface AppComponent {

    fun getApplicationContext() : Context

    fun injectApplication(app : App)
}
