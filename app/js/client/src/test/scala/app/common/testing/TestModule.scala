package app.common.testing

import hydro.common.testing.FakeClock
import hydro.common.testing.FakeI18n
import hydro.common.testing.FakeJsEntityAccess
import hydro.common.testing.FakeRouterContext
import hydro.flux.action.Dispatcher
import hydro.models.access.HydroPushSocketClientFactory

class TestModule {

  // ******************* Fake implementations ******************* //
  implicit lazy val fakeEntityAccess = new FakeJsEntityAccess
  implicit lazy val fakeClock = new FakeClock
  implicit lazy val fakeDispatcher = new Dispatcher.Fake
  implicit lazy val fakeI18n = new FakeI18n
  implicit lazy val testUser = TestObjects.testUser
  implicit lazy val fakeScalaJsApiClient = new FakeScalaJsApiClient
  implicit lazy val fakeRouterContext = new FakeRouterContext

  // ******************* Non-fake implementations ******************* //
  implicit lazy val hydroPushSocketClientFactory: HydroPushSocketClientFactory =
    new HydroPushSocketClientFactory
}
