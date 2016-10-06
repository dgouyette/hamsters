import io.github.hamsters.Validation._
import io.github.hamsters.{FutureEither, FutureOption}
import org.scalatest._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration._
import io.github.hamsters.MonadTransformers._

class MonadTransformersSpec extends FlatSpec with Matchers {

  "FutureOption" should "handle Future[Option[_]] type" in {
    def foa: Future[Option[String]] = Future(Some("a"))
    def fob(a: String): Future[Option[String]] = Future(Some(a + "b"))

    val composedAB: Future[Option[String]] = for {
      a <- FutureOption(foa)
      ab <- FutureOption(fob(a))
    } yield ab

    Await.result(composedAB, 1 second) shouldBe Some("ab")

    val composedABWithNone: Future[Option[String]] = for {
      a <- FutureOption(Future.successful(None))
      ab <- FutureOption(fob(a))
    } yield ab

    Await.result(composedABWithNone, 1 second) shouldBe None

    val composedABWithFailure: Future[Option[String]] = for {
      a <- FutureOption(Future.failed(new Exception("d'oh!")))
      ab <- FutureOption(fob(a))
    } yield ab

    an[Exception] should be thrownBy Await.result(composedABWithFailure, 1 second)
  }


  "FutureOption" should "be filtered with pattern matching in for comprehension" in {
    def fo: Future[Option[(String, Int)]] = Future(Some(("a", 42)))

    val filtered = for {
      (a, i) <- FutureOption(fo) if i > 5
    } yield a

    Await.result(filtered, 1 second) shouldBe Some("a")

    val filtered2 = for {
      (a, i) <- FutureOption(fo) if i > 50
    } yield a

    Await.result(filtered2, 1 second) shouldBe None
  }

  "FutureOption" should "handle future and option map/flatMap sequences" in {

    import scala.concurrent.Future
    import scala.concurrent.ExecutionContext.Implicits.global
    import io.github.hamsters.FutureOption
    import io.github.hamsters.MonadTransformers._

    implicit def optiontoFutureOption[T](o: Option[T]): Future[Option[T]] = Future.successful(o)
    implicit def futuretoFutureOption[T](f: Future[T]): Future[Some[T]] = f.map(Some(_))

    case class User(id: Int)
    case class Data(id: Int)

    object Repo {
      def user(userId:Int) = Future(Some(User(userId)))
      def getData(id: Int) = Future(Data(1))
      def updateData(id: Int) = Future(2)
    }

    type Result = String
    val userIdOption = Some(1)

    val operationSequenceOpt : Future[Option[Result]] =
      for {
        userId <- FutureOption(userIdOption)
        user <- FutureOption(Repo.user(userId))
        data <- FutureOption(Repo.getData(user.id))
        _ <- FutureOption(Repo.updateData(data.id))
      } yield {
        "redirect"
      }

     val operationSequence = operationSequenceOpt.map(_.getOrElse("error"))

     Await.result(operationSequence, 1 second) shouldBe "redirect"

  }

  "FutureEither" should "handle Future[Either[_,_]] type" in {
    def fea: Future[Either[String, Int]] = Future(OK(1))
    def feb(a: Int): Future[Either[String, Int]] = Future(OK(a + 2))

    val composedAB: Future[Either[String, Int]] = for {
      a <- FutureEither(fea)
      ab <- FutureEither(feb(a))
    } yield ab

    Await.result(composedAB, 1 second) shouldBe OK(3)

    val composedABWithNone: Future[Either[String, Int]] = for {
      a <- FutureEither(Future.successful(KO("d'oh!")))
      ab <- FutureEither(feb(a))
    } yield ab

    Await.result(composedABWithNone, 1 second) shouldBe KO("d'oh!")

    val composedABWithFailure: Future[Either[String, Int]] = for {
      a <- FutureEither(Future.failed(new Exception("d'oh!")))
      ab <- FutureEither(feb(a))
    } yield ab

    an[Exception] should be thrownBy Await.result(composedABWithFailure, 1 second)
  }

  "FutureEither" should "be filtered with pattern matching in for comprehension" in {
    def fe: Future[Either[String, (String, Int)]] = Future(OK(("a", 42)))

    val filtered = for {
      (a, i) <- FutureEither(fe) if i > 5
    } yield a

    Await.result(filtered, 1 second) shouldBe OK("a")

    val filtered2 = for {
      (a, i) <- FutureEither(fe) if i > 50
    } yield a

    Await.result(filtered2, 1 second) shouldBe KO("No value matching predicate")
  }
}