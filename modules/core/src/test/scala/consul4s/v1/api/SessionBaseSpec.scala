package consul4s.v1.api

import com.dimafeng.testcontainers.scalatest.TestContainerForAll
import consul4s.model.CheckStatus
import consul4s.model.catalog.{NodeDeregistration, NodeRegistration}
import consul4s.model.health.NewHealthCheck
import consul4s.model.session.NewSession
import consul4s.{ConsistencyMode, ConsulContainer, ConsulSpec, JsonDecoder, JsonEncoder}

class SessionBaseSpec(implicit jsonDecoder: JsonDecoder, jsonEncoder: JsonEncoder) extends ConsulSpec with TestContainerForAll {
  override val containerDef: ConsulContainer.Def = ConsulContainer.Def()

  "session api" should {
    "create session" in withContainers { consul =>
      val client = createClient(consul)
      val session = NewSession("node", "15s")

      runEither {
        for {
          // node should exist
          _ <- client
            .registerEntity(
              NodeRegistration("node", "address", check = Some(NewHealthCheck("node", "serfHealth", status = Some(CheckStatus.Passing))))
            )
            .body
          sessionId <- client.createSession(session).body
          sessionList <- client.getListOfActiveSessions(consistencyMode = ConsistencyMode.Consistent).body
          _ <- client.deregisterEntity(NodeDeregistration("node")).body
        } yield {
          assert(sessionList.exists(_.id.contains(sessionId.id)))
        }
      }
    }

    "create session and list sessions for specific node" in withContainers { consul =>
      val client = createClient(consul)
      val session = NewSession("node", "15s")

      runEither {
        for {
          // node should exist
          _ <- client
            .registerEntity(
              NodeRegistration("node", "address", check = Some(NewHealthCheck("node", "serfHealth", status = Some(CheckStatus.Passing))))
            )
            .body
          r <- client.getListOfActiveNodeSessions("node").body
          sessionId <- client.createSession(session).body
          sessionList <- client.getListOfActiveNodeSessions("node", consistencyMode = ConsistencyMode.Consistent).body
          _ <- client.deregisterEntity(NodeDeregistration("node")).body
        } yield {
          assert(r.isEmpty)
          assert(sessionList.exists(_.id.contains(sessionId.id)))
        }
      }
    }

    "create session and get session info" in withContainers { consul =>
      val client = createClient(consul)
      val session = NewSession("node", "15s")

      runEither {
        for {
          // node should exist
          _ <- client
            .registerEntity(
              NodeRegistration("node", "address", check = Some(NewHealthCheck("node", "serfHealth", status = Some(CheckStatus.Passing))))
            )
            .body
          sessionId <- client.createSession(session).body
          sessionInfo <- client.getSessionInfo(sessionId, consistencyMode = ConsistencyMode.Consistent).body
          _ <- client.deregisterEntity(NodeDeregistration("node")).body
        } yield {
          assert(sessionInfo.exists(_.id.contains(sessionId.id)))
        }
      }
    }

    "create session, get session info and delete" in withContainers { consul =>
      val client = createClient(consul)
      val session = NewSession("node", "15s")

      runEither {
        for {
          // node should exist
          _ <- client
            .registerEntity(
              NodeRegistration("node", "address", check = Some(NewHealthCheck("node", "serfHealth", status = Some(CheckStatus.Passing))))
            )
            .body
          sessionId <- client.createSession(session).body
          sessionInfo <- client.getSessionInfo(sessionId, consistencyMode = ConsistencyMode.Consistent).body
          _ <- client.deleteSession(sessionId).body
          sessionInfoAfterDeletion <- client.getSessionInfo(sessionId, consistencyMode = ConsistencyMode.Consistent).body
          _ <- client.deregisterEntity(NodeDeregistration("node")).body
        } yield {
          assert(sessionInfo.exists(_.id.contains(sessionId.id)))
          assert(sessionInfoAfterDeletion.isEmpty)
        }
      }
    }

    "create and renew session" in withContainers { consul =>
      val client = createClient(consul)
      val session = NewSession("node", "15s")

      runEither {
        for {
          // node should exist
          _ <- client
            .registerEntity(
              NodeRegistration("node", "address", check = Some(NewHealthCheck("node", "serfHealth", status = Some(CheckStatus.Passing))))
            )
            .body
          sessionId <- client.createSession(session).body
          sessionList <- client.getListOfActiveSessions(consistencyMode = ConsistencyMode.Consistent).body
          response <- client.renewSession(sessionId).body
          _ <- client.deregisterEntity(NodeDeregistration("node")).body
        } yield {
          assert(sessionList.exists(_.id.contains(sessionId.id)))
          assert(response.exists(_.id.contains(sessionId.id)))
        }
      }
    }
  }
}
