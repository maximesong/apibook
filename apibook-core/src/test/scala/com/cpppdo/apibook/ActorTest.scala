package com.cpppdo.apibook

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest.{WordSpecLike, MustMatchers}

/**
 * Created by song on 3/25/15.
 */
class ActorTest extends TestKit(ActorSystem("TestSystem"))
  with WordSpecLike
  with MustMatchers {

  "An actor" must {
    "iasas" in {
      fail("not implemented")
    }
  }
}
