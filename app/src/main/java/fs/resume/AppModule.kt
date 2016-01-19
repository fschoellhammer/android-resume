package fs.resume

import android.content.Context
import android.content.res.Resources
import dagger.Module
import dagger.Provides

@Module
class AppModule(val app : App) {

    @Provides
    fun getApplication() = app

    @Provides
    fun getContext() : Context = app.applicationContext

    @Provides
    fun getResources(ctx : Context) : Resources = ctx.resources
}
