import app.api.ScalaJsApiModule
import app.common.CommonModule
import app.models.ModelsModule
import com.google.inject.AbstractModule
import tools.ApplicationStartHook

final class Module extends AbstractModule {
  override def configure() = {
    bind(classOf[ApplicationStartHook]).asEagerSingleton

    install(new CommonModule)
    install(new ModelsModule)
    install(new ScalaJsApiModule)
  }
}
