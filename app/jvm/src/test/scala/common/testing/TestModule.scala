package common.testing

import app.api.ScalaJsApiModule
import com.google.inject._
import common._
import common.time._
import models.ModelsModule
import models.access.JvmEntityAccess
import models.user.User
import models.user.Users

final class TestModule extends AbstractModule {

  override def configure() = {
    install(new ModelsModule)
    install(new ScalaJsApiModule)
    bindSingleton(classOf[Clock], classOf[FakeClock])
    bindSingleton(classOf[PlayI18n], classOf[FakePlayI18n])
    bind(classOf[I18n]).to(classOf[PlayI18n])
  }

  @Provides()
  private[testing] def playConfiguration(): play.api.Configuration = {
    play.api.Configuration.from(Map("app.media.mediaFolder" -> "~/Music"))
  }

  private def bindSingleton[T](interface: Class[T], implementation: Class[_ <: T]): Unit = {
    bind(interface).to(implementation)
    bind(implementation).asEagerSingleton()
  }
}
