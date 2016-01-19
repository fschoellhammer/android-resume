package fs.resume

import android.app.Application

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        App.component = DaggerAppComponent.builder()
            .appModule(AppModule(this))
            .build();
        App.component.injectApplication(this)
    }

    companion object {
        lateinit var component : AppComponent
    }
}
