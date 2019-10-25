// Copyright(C) 2018-2019 John A. De Goes. All rights reserved.

package net.degoes.abstractions

import scalaz._
import Scalaz._
import net.degoes.abstractions.functor.Leaf
import net.degoes.abstractions.functor.Fork

import scala.concurrent.Future

object algebra {
  type ??? = Nothing

  //
  // EXERCISE 1
  //
  // Define a semigroup instance for `String`.
  //
  implicit val StringSemigroup: Semigroup[String] =
    new Semigroup[String] {
      def append(l: String, r: => String): String = l + r
    }

  //
  // EXERCISE 2
  //
  // Define a semigroup instance for the `NotEmpty` data type below.
  //
  case class NotEmpty[+A](head: A, tail: Option[NotEmpty[A]])
  implicit def NotEmptySemigroup[A]: Semigroup[NotEmpty[A]] =
    new Semigroup[NotEmpty[A]] {
      def append(l: NotEmpty[A], r: => NotEmpty[A]): NotEmpty[A] =
        NotEmpty(l.head, l.tail.map(append(_, r)) orElse Some(r))
    }
  val example1 = NotEmpty(1, None) |+| NotEmpty(2, None)

  //
  // EXERCISE 3
  //
  // Define a semigroup for `Max` that chooses the maximum of two values.
  //
  final case class Max(value: Int)
  implicit val MaxSemigroup: Semigroup[Max] =
    new Semigroup[Max] {
      def append(l: Max, r: => Max): Max =
        if(l.value > r.value) r else l //Example of commutative semigroup (order doesn't matter)
    }

  //
  // EXERCISE 4
  //
  // Define a `Semigroup` for `Last[A]` that always chooses the right-most value.
  //
  final case class Last[A](value: A)
  object Last {
    implicit def LastSemigroup[A]: Semigroup[Last[A]] =
      new Semigroup[Last[A]] {
        def append(l: Last[A], r: => Last[A]): Last[A] = r
      }
  }
  final case class First[A](value: A)
  object First {
    implicit def FirstSemigroup[A]: Semigroup[First[A]] =
      new Semigroup[First[A]] {
        def append(l: First[A], r: => First[A]): First[A] = l
      }
  }

  //
  // EXERCISE 5
  //
  // Define a `Semigroup` for `Option[A]` whenever `A` forms a `Semigroup`.
  //
  implicit def OptionSemigroup[A: Semigroup]: Semigroup[Option[A]] =
    new Semigroup[Option[A]] {
      def append(l: Option[A], r: => Option[A]): Option[A] =
        (l, r) match {
          case (None, None)       => None
          case (Some(l), None)    => Some(l)
          case (None, Some(r))    => Some(r)
          case (Some(l), Some(r)) => Some(l |+| r)
        }
    }

  //
  // EXERCISE 6
  //
  // Define an instance of `Semigroup` for `(A, B)` when both `A` and
  // `B` form semigroups.
  //
  implicit def SemigroupTuple2[A: Semigroup, B: Semigroup]: Semigroup[(A, B)] = new Semigroup[(A, B)] {
    def append(l: (A, B), r: => (A, B)): (A, B) =
      (l._1 |+| r._1, l._2 |+| r._2)
  }

  trait Monoid[A] extends Semigroup[A] {
    // append(zero, a) == a
    // append(a, zero) == a
    def zero: A // Monoid type class add to the capabilities of Semigroup.
  }

  //
  // EXERCISE 7
  //
  // Define a monoid for boolean conjunction (`&&`).
  //
  final case class Conj(value: Boolean)
  object Conj {
    implicit val ConjMonoid: Monoid[Conj] =
      new Monoid[Conj] {
        def zero: Conj                        = Conj(true)
        def append(l: Conj, r: => Conj): Conj = Conj(l.value && r.value)
      }
  }

  //
  // EXERCISE 8
  //
  // Define a monoid for boolean disjunction (`||`).
  //
  final case class Disj(value: Boolean)
  object Disj {
    implicit val DisjMonoid: Monoid[Disj] =
      new Monoid[Disj] {
        def zero: Disj                        = Disj(false)
        def append(l: Disj, r: => Disj): Disj = ???
      }
  }

  //
  // EXERCISE 9
  //
  // Define a `Monoid` for `Try[A]` whenever `A` forms a `Semigroup`.
  //
  case object ZeroThrowable extends Throwable
  def TryMonoid[A: Semigroup]: Monoid[scala.util.Try[A]] =
    new Monoid[scala.util.Try[A]] {
      import scala.util._

      def zero: Try[A] = ??? //Failure()

      def append(l: Try[A], r: => Try[A]): Try[A] =
        (l, r) match {
          case (Failure(l), Failure(r)) => ???
          case (Success(l), Failure(r)) => ???
          case (Failure(l), Success(r)) => ???
          case (Success(l), Success(r)) => ???
        }
    }

  //
  // EXERCISE 10
  //
  // Write the `Monoid` instance for `Map` when the values form a semigroup.
  //
  def MonoidMap[K, V: Semigroup]: Monoid[Map[K, V]] =
    new Monoid[Map[K, V]] {
      def zero: Map[K, V] = Map.empty

      def append(l: Map[K, V], r: => Map[K, V]): Map[K, V] = ???
    }

  //
  // EXERCISE 11
  //
  // Design a permission system for securing some resource. Design the core
  // permissions data type, implement a monoid for the instance, and implement
  // the missing methods for the data type.
  //
  // Assumptions:
  //   1. Users have multiple accounts (`AccountID`)
  //   2. Each account gives different capabilities (`Capability`) to
  //      different resources (`ResourceID`)
  //
  type AccountID  = String
  type ResourceID = String
  sealed trait Capability
  object Capability {
    final case object Read  extends Capability
    final case object Write extends Capability
  }
//  case class UserPermission(value: Map[ResourceID, Set[(AccountID, Capability)]]) {
//  case class UserPermission(value: Map[ResourceID, (Map[Capability, Set[Capability]]) {
  case class UserPermission(value: Set[(AccountID, ResourceID, Capability)]) {

    /**
     * Provides the set of all resources the user has access to.
     */
    def allResources: Set[ResourceID] = ??? //value._2

    /**
     * Provides the set of all capabilities the user has on the
     * specified resource.
     */
    def capabilitiesFor(resourceID: ResourceID): Set[Capability] = ??? //value._3

    /**
     * Determines which accounts give the user access to the specified
     * capability on the specified resource.
     */
    def audit(resourceID: ResourceID, capability: Capability): Set[AccountID] = ???
  }
  implicit val MonoidUserPermission: Monoid[UserPermission] =
    new Monoid[UserPermission] {
      def zero: UserPermission = UserPermission(Set())

      def append(l: UserPermission, r: => UserPermission): UserPermission = ???
    }
//  val example2 = mzero[UserPermission] |+| UserPermission(???)

  //
  // EXERCISE 12
  //
  // Try to define an instance of `Monoid` for `NotEmpty` for any type `A`.
  //
  implicit def MonoidNotEmpty[A]: Monoid[NotEmpty[A]] = ???
}

object functor {

  /**
   * Identity Law
   *   map(fa)(identity) == fa
   *
   * Composition Law
   *   map(map(fa)(g))(f) == map(fa)(f compose g)
   */
  val Numbers          = List(12, 123, 0, 123981)
  val Expected         = List(2, 3, 1, 6)
  val g: Int => String = (i: Int) => i.toString
  val f: String => Int = (s: String) => s.length
  Numbers.map(identity) == Numbers
  Numbers.map(f compose g) == (Numbers map g).map(f)

  //
  // EXERCISE 1
  //
  // Define an instance of `Functor` for `BTree`.
  //
  sealed trait BTree[+A]
  final case class Leaf[A](a: A)                            extends BTree[A]
  final case class Fork[A](left: BTree[A], right: BTree[A]) extends BTree[A]
  implicit val BTreeFunctor: Functor[BTree] =
    new Functor[BTree] {
      def map[A, B](fa: BTree[A])(f: A => B): BTree[B] =
        fa match {
          case Leaf(a) => Leaf(f(a))
          case Fork(left, right) => Fork(map(left)(f), map(right)(f))
        }
    }

  //
  // EXERCISE 2
  //
  // Define an instance of `Functor` for `Nothing`.
  //
  implicit val NothingFunctor: Functor[Nothing] = ???

  //
  // EXERCISE 3
  //
  // Define an instance of `Functor` for Parser[E, ?].
  //
  def ParserFunctor[E]: Functor[Parser[E, ?]] =
    new Functor[Parser[E, ?]] {
      def map[A, B](fa: Parser[E, A])(f: A => B): Parser[E, B] = ???
    }
  final case class Parser[+E, +A](run: String => Either[E, (String, A)])
  object Parser {
    final def fail[E](e: E): Parser[E, Nothing] =
      Parser(input => Left(e))

    final def succeed[A](a: => A): Parser[Nothing, A] =
      Parser(input => Right((input, a)))

    final val char: Parser[Unit, Char] =
      Parser(
        input =>
          if (input.length == 0) Left(())
          else Right((input.drop(1), input.charAt(0)))
      )
  }

  //
  // EXERCISE 4
  //
  // Try to define an instance of `Functor` for the following data type.
  //
  case class DataType[A](f: A => A)
  implicit val DataTypeFunctor: Functor[DataType] =
    new Functor[DataType] {
      def map[A, B](fa: DataType[A])(f: A => B): DataType[B] =
        ???
    }

  //
  // EXERCISE 5
  //
  // Define an instance of `Functor` for `FunctorProduct`.
  //
  case class FunctorProduct[F[_], G[_], A](l: F[A], r: G[A])
  implicit def FunctorProductFunctor[F[_]: Functor, G[_]: Functor]: Functor[FunctorProduct[F, G, ?]] =
    new Functor[FunctorProduct[F, G, ?]] {
      def map[A, B](fa: FunctorProduct[F, G, A])(f: A => B): FunctorProduct[F, G, B] =
      FunctorProduct(Functor[F].map(fa.l)(f), Functor[G].map(fa.r)(f))

    }

  //
  // EXERCISE 6
  //
  // Define an instance of `Functor` for `FunctorSum`.
  //
  case class FunctorSum[F[_], G[_], A](run: Either[F[A], G[A]])
  implicit def FunctorSumFunctor[F[_]: Functor, G[_]: Functor]: Functor[FunctorSum[F, G, ?]] =
    new Functor[FunctorSum[F, G, ?]] {
      def map[A, B](fa: FunctorSum[F, G, A])(f: A => B): FunctorSum[F, G, B] =
        ???
    }

  //
  // EXERCISE 7
  //
  // Define an instance of `Functor` for `FunctorNest`.
  // e.g. Future[Option[A]], Future[Either[Error, Option[A]]]
  // Future[List[Either[Error, Option[A]]]]
  //
  case class FunctorNest[F[_], G[_], A](run: F[G[A]])
  implicit def FunctorNestFunctor[F[_]: Functor, G[_]: Functor]: Functor[FunctorNest[F, G, ?]] =
    new Functor[FunctorNest[F, G, ?]] {
      def map[A, B](fa: FunctorNest[F, G, A])(f: A => B): FunctorNest[F, G, B] =
        ???
    }

  //
  // EXERCISE 8
  //
  // Define a natural transformation between `List` and `Option`.
  //
  val headOption: List ~> Option = ???
  headOption(List(1, 2, 3))
  headOption(List("foo", "bar", "baz"))

  //
  // EXERCISE 9
  //
  // Define a natural transformation between `Either[Throwable, ?]` and
  // `Future`.
  //
  val eitherToFuture: Either[Throwable, ?] ~> scala.concurrent.Future =
    new (Either[Throwable, ?] ~> scala.concurrent.Future) {
      def apply[A](fa: Either[Throwable, A]): scala.concurrent.Future[A] =
        fa match {
          case Left(t) => Future.failed(t)
          case Right(v) => Future.successful(v)
        }
    }

  //
  // EXERCISE 10
  //
  // Define an instance of `Zip` for `Option`.
  //
  trait Zip[F[_]] extends Functor[F] {
    def zip[A, B](l: F[A], r: F[B]): F[(A, B)]
  }
  object Zip {
    def apply[F[_]](implicit F: Zip[F]): Zip[F] = F

    implicit val ZipOption: Zip[Option] =
      new Zip[Option] {
        def map[A, B](fa: Option[A])(f: A => B) = fa map f

        def zip[A, B](l: Option[A], r: Option[B]): Option[(A, B)] =
          (l, r) match {
            case (None, None) => None
            case (Some(l), None) => None
            case (None, Some(r)) => None
            case (Some(l), Some(r)) => Some((l, r))
          }
      }
  }
  implicit class ZipSyntax[F[_], A](left: F[A]) {
    def zip[B](right: F[B])(implicit F: Zip[F]): F[(A, B)] =
      F.zip(left, right)
  }

  //
  // EXERCISE 11
  //
  // Define an instance of `Zip` for `List`
  //
  val ZipList: Zip[List] =
    new Zip[List] {
      def map[A, B](fa: List[A])(f: A => B): List[B] =
        fa map f

      def zip[A, B](l: List[A], r: List[B]): List[(A, B)] =
        l.flatMap(l1 => r.map(r1 => (l1, r1)))
    }

  //
  // EXERCISE 12
  //
  // Define an instance of `Zip` for `Parser[E, ?]`.
  //
  def ZipParser[E]: Zip[Parser[E, ?]] =
    new Zip[Parser[E, ?]] {
      def map[A, B](fa: Parser[E, A])(f: A => B): Parser[E, B] =
        fa map f

      def zip[A, B](l: Parser[E, A], r: Parser[E, B]): Parser[E, (A, B)] =
        ???
    }

  //
  // EXERCISE 13
  //
  // Define an instance of `Zip` for `Future`.
  //
  val ZipFuture: Zip[scala.concurrent.Future] =
    new Zip[scala.concurrent.Future] {
      import scala.concurrent.Future
      import scala.concurrent.ExecutionContext.Implicits.global

      def map[A, B](fa: Future[A])(f: A => B): Future[B] = fa map f

      def zip[A, B](l: Future[A], r: Future[B]): Future[(A, B)] = {
        val rr: Future[(A, B)] = l.flatMap(a => r.map(b => (a, b)))
        rr
      }
    }

  //
  // EXERCISE 14
  //
  // Define `Applicative` for `Option`.
  //
  val OptionApplicative: Applicative[Option] =
    new Applicative[Option] {
      def point[A](a: => A): Option[A] = Some(a)

      def zip[A, B](fa: Option[A], fb: Option[B]): Option[(A, B)] = ???

      final def ap[A, B](fa: => Option[A])(f: => Option[A => B]): Option[B] =
        zip(f, fa).map(t => t._1(t._2))
    }

  //
  // EXERCISE 15
  //
  // Implement `zip` in terms of the applicative composition using `|@|`.
  //
  // Bonus: Implement `ap2` in terms of `zip`.
  //
  val example1 = (Option(3) |@| Option(5))(_ + _)
  val example2 = zip(Option(3), Option("foo")): Option[(Int, String)]
  def zip[F[_]: Applicative, A, B](l: F[A], r: F[B]): F[(A, B)] =
    ???
  def ap2[F[_]: Zip, A, B](fa: F[A], fab: F[A => B]): F[B] =
    ???

  //
  // EXERCISE 16
  //
  // Define an instance of `Applicative` for `Parser[E, ?]`.
  //
  def ApplicativeParser[E]: Applicative[Parser[E, ?]] =
    new Applicative[Parser[E, ?]] {
      def point[A](a: => A): Parser[E, A] =
        ???

      def zip[A, B](fa: Parser[E, A], fb: Parser[E, B]): Parser[E, (A, B)] =
        ZipParser.zip(fa, fb)

      final def ap[A, B](fa: => Parser[E, A])(f: => Parser[E, A => B]): Parser[E, B] =
        zip(f, fa).map(t => t._1(t._2))
    }

  //
  // EXERCISE 17
  //
  // Define an instance of `Monad` for `BTree`.
  //
  implicit val MonadBTree: Monad[BTree] =
    new Monad[BTree] {
      def point[A](a: => A): BTree[A] =
        Leaf(a)

      def bind[A, B](fa: BTree[A])(f: A => BTree[B]): BTree[B] =
        fa match {
          case Leaf(a) => f(a)
          case Fork(l, r) => Fork(bind(l)(f), bind(r)(f))
        }
    }

  //
  // EXERCISE 18
  //
  // Define an instance of `Monad` for `Parser[E, ?]`.
  //
  implicit def MonadParser[E]: Monad[Parser[E, ?]] =
    new Monad[Parser[E, ?]] {
      def point[A](a: => A): Parser[E, A] =
        Parser[E, A](s => Right((s, a)))

      def bind[A, B](fa: Parser[E, A])(f: A => Parser[E, B]): Parser[E, B] = ???
    }

  //
  // EXERCISE 19
  //
  // Define an instance of `Monad` for `Identity`.
  //
  case class Identity[A](run: A)
  implicit val IdentityMonad: Monad[Identity] =
    new Monad[Identity] {
      def point[A](a: => A): Identity[A] = ???

      def bind[A, B](fa: Identity[A])(f: A => Identity[B]): Identity[B] =
        ???
    }

  for {
    x <- Identity(2)
    y <- Identity(x * x)
    z <- Identity(y + x * y + 100)
    z <- Identity(z + z)
  } yield z

  //
  // EXERCISE 20
  //
  // Use the list monad to find pairs of numbers separated by 2 integers away
  // from each other.
  //
  val integers = List(1, 9, 2, 7, 4, 8, 2, 0, 10)
  val solution = ???
}

object parser {
  //
  // EXERCISE 1
  //
  // Implement all missing methods for parser.
  //
  case class Parser[+E, +A](run: String => Either[E, (String, A)]) { self =>
    def ~[E1 >: E, B](that: => Parser[E1, B]): Parser[E1, (A, B)] =
      self.flatMap(a => that.map(b => (a, b)))

    def ~>[E1 >: E, B](that: => Parser[E1, B]): Parser[E1, B] =
      (self ~ that).map(_._2)

    def <~[E1 >: E, B](that: => Parser[E1, B]): Parser[E1, A] =
      (self ~ that).map(_._1)

    def map[B](f: A => B): Parser[E, B] =
      flatMap(f.andThen(Parser.succeed[B](_)))

    def flatMap[E1 >: E, B](f: A => Parser[E1, B]): Parser[E1, B] =
      Parser { string =>
        this.run(string).flatMap {
          case(s, a) => f(a).run(s)
        }
      }

    def orElse[E2, B](that: => Parser[E2, B]): Parser[E2, Either[A, B]] = ???

    def filter[E1 >: E](e0: E1)(f: A => Boolean): Parser[E1, A] =
      Parser(
        input =>
          self.run(input) match {
            case Left(e)           => Left(e)
            case Right((input, a)) => if (f(a)) Right((input, a)) else Left(e0)
          }
      )

    def |[E2, A1 >: A](that: => Parser[E2, A1]): Parser[E2, A1] =
      (self orElse that).map(_.merge)

    def rep: Parser[E, List[A]] =
      (self.map(List(_)) ~ rep).map(t => t._1 ++ t._2) | Parser.succeed[List[A]](Nil)

    def rep1: Parser[E, (A, List[A])] = self ~ rep

    def repsep[E1 >: E](sep: Parser[E1, Any]): Parser[E1, List[A]] =
      ((self <~ sep).rep ~ (self ?)).map {
        case (list, opt) => list ++ opt.toList
      }

    def rep1sep[E1 >: E](sep: Parser[E1, Any]): Parser[E1, (A, List[A])] =
      (self <~ (sep ?)) ~ repsep(sep)

    def ? : Parser[Nothing, Option[A]] = self.map(Some(_)) | Parser.succeed(None)
  }
  object Parser {
    def fail[E](e: E): Parser[E, Nothing] =
      Parser(input => Left(e))

    def succeed[A](a: => A): Parser[Nothing, A] =
      Parser(input => Right((input, a)))

    def maybeChar: Parser[Nothing, Option[Char]] = char ?

    def char: Parser[Unit, Char] =
      Parser(
        input =>
          if (input.length == 0) Left(())
          else Right((input.drop(1), input.charAt(0)))
      )

    def digit: Parser[Unit, Int] =
      for {
        c      <- char
        option = scala.util.Try(c.toString.toInt).toOption
        d      <- option.fold[Parser[Unit, Int]](Parser.fail(()))(succeed(_))
      } yield d

    def literal(c0: Char): Parser[Unit, Char] =
      for {
        c <- char
        _ <- if (c != c0) Parser.succeed(c) else Parser.fail(())
      } yield c

    def whitespace: Parser[Nothing, Unit] =
      Parser(input => Right((input.dropWhile(_ == ' '), ())))
  }

  // [1,2,3]
  import Parser.{ digit, literal }
  val parser: Parser[Unit, List[Int]] =
    for {
      _      <- literal('[')
      digits <- digit.repsep(literal(','))
      _      <- literal(']')
    } yield digits
}

object foldable {
  //
  // EXERCISE 1
  //
  // Define an instance of `Foldable` for `List`
  //
  implicit val FoldableList: Foldable[List] = new Foldable[List] {
    def foldMap[A, B: Monoid](fa: List[A])(f: A => B): B = {
      fa.map(f).foldLeft(Monoid[B].zero)(_ |+| _)
    }


    def foldRight[A, B](fa: List[A], z: => B)(f: (A, => B) => B): B = ???
  }

  //
  // EXERCISE 2
  //
  // Define an instance of `Foldable` for `BTree`.
  //
  sealed trait BTree[+A]
  final case class Leaf[A](a: A)                            extends BTree[A]
  final case class Fork[A](left: BTree[A], right: BTree[A]) extends BTree[A]
  implicit val FoldableBTree: Foldable[BTree] =
    new Foldable[BTree] {
      def foldMap[A, B: Monoid](fa: BTree[A])(f: A => B): B =
        ???

      def foldRight[A, B](fa: BTree[A], z: => B)(f: (A, => B) => B): B =
        ???
    }
  val btree: BTree[String] = Fork(Leaf("foo"), Fork(Leaf("bar"), Leaf("baz")))

  //
  // EXERCISE 3
  //
  // Try to define an instance of `Foldable` for `A0 => ?`.
  //
  implicit def FunctionFoldable[A0]: Foldable[A0 => ?] = ???

  // Imperative version of `traverse`
  def foreach[A, B](source: List[A])(f: A => B): List[B] = {
    var output = List.empty[B]

    source.foreach { a =>
      f(a) :: output // GEt back `B`
    }

    output.reverse
  }

  foreach((1 to 3).toList) { number =>
    println(number) //Because of the print
  }

  def foreach_v2[G[_] : Applicative, F[_], A, B](source: F[A])(f: A => G[B]): G[F[B]] = {
    ???
  }

    //
  // EXERCISE 4
  //
  // Define an instance of `Traverse` for `BTree`.
  //
  implicit lazy val TraverseBTree: Traverse[BTree] =
    new Traverse[BTree] {
      def traverseImpl[G[_]: Applicative, A, B](fa: BTree[A])(f: A => G[B]): G[BTree[B]] =
        fa match {
          case Leaf(a) => f(a).map(Leaf(_))
          case Fork(left, right) =>
            val l1 = traverseImpl(left)
            val r1 = traverseImpl(right)

//            (l1 |@| r1).map(Fork(_, _))
            ???
        }
    }

  //
  // EXERCISE 5
  //
  // Try to define an instance of `Traverse` for `Parser[E, ?]`.
  //
  case class Parser[+E, +A](run: String => Either[E, (String, A)])
  implicit def TraverseParser[E]: Traverse[Parser[E, ?]] =
    new Traverse[Parser[E, ?]] {
      def traverseImpl[G[_]: Applicative, A, B](fa: Parser[E, A])(f: A => G[B]): G[Parser[E, B]] =
        ???
    }

//  BONUS EXERCISES
  object exercises {
    import scala.concurrent._

  def foreachOption[A, B](list: List[A])(f: A => Option[B]): Option[List[B]] = {
    list match {
      case Nil => Some(Nil)
      case a :: as =>
        val optionB = f(a)
        val optionsB = foreachOption(as)(f)
        //          (optionB zipWith optionBs).map(_ :: _)

        for {
          b <- optionB
          bs <- optionsB
        } yield b :: bs
    }
  }

    def foreachFuture[A, B](tree: BTree[A])(f: A => Future[B]): Future[BTree[B]] = {
      import scala.concurrent.ExecutionContext.Implicits.global

      tree match {
        case Leaf(a) => f(a).map(Leaf(_))
        case Fork(left, right) =>
          val left = foreachFuture(left)(f)
          val right = foreachFuture(right)(f)

          (left zip right).map {
            case (l, r) => Fork(l, r)
          }
      }

    }

    def foreachEither[K, E, A, B](map: Map[K, A])(f: A => Either[E, B]): Either[E, Map[K, B]] = {
      map.foldLeft[Either[E, Map[K, B]]](Right(Map())) {
        case (eitherMap, (k,a)) =>
          for {
            map <- eitherMap
            b <- f(a)
          } yield map + (k -> b)
      }
    }
  }
}

object optics {
  sealed trait Country
  object Country {
    val usa: Prism[Country, Unit] =
      Prism[Country, Unit](_ match { case USA => Some(()); case _ => None }, _ => USA)

    val uk: Prism[Country, UKRegion] = ???

    val poland: Prism[Country, Unit] = ???
  }
  case object USA                 extends Country
  case class UK(region: UKRegion) extends Country
  case object Poland              extends Country
  sealed trait UKRegion

  case class Org(name: String, address: Address, site: Site)
  object Org {
    val site: Lens[Org, Site] =
      Lens[Org, Site](_.site, s => _.copy(site = s))

    val address = Lens[Org, Address] = Lens[Org, Address](_.address, a => _.copy(address = a))
    val name = Lens[Org, String] = Lens[Org, String](_.name, a => _.copy(name = a))
  }

  case class Address(number: String, street: String, postalCode: String, country: Country)
  object Address {
    val country: Lens[Address, Country] =
      Lens[Address, Country](_.country, c => _.copy(country = c))

    val street: Lens[Address, String] = Lens[Address, String](_.street, s => _.copy(street = s))
    val postalCode: Lens[Address, String] = Lens[Address, String](_.postalCode, s => _.copy(street = s))
  }

  case class Site(manager: Employee, address: Address, employees: Set[Employee])
  object Site {
    val address: Lens[Site, Address] =
      Lens[Site, Address](_.address, a => _.copy(address = a))
    val manager: Lens[Site, Employee] =
      Lens[Site, Employee](_.manager, m => _.copy(manager = m))
  }
  case class Employee(name: String, dob: java.time.Instant, salary: BigDecimal, address: Address)
  object Employee {
    val name: Lens[Employee, String] =
      Lens[Employee, String](_.name, n => _.copy(name = n))

    val salary: Lens[Employee, BigDecimal] =
      Lens[Employee, BigDecimal](_.salary, s => _.copy(salary = s))
  }

  lazy val org: Org = ???

  // Lens is a type o Optics
  // Allows us to focus on Product type
  //


  // EXERCISE 1
  //
  // Implement the `⋅` method of `Lens` for `Lens`.
  //
  final case class Lens[S, A](
    get: S => A,
    set: A => (S => S)
  ) { self =>
    def ⋅[B](that: Lens[A, B]): Lens[S, B] =
      Lens[S, B] (
//        get = (s: S) => (that.get(self.get(s)) : B),
        get = that.get compose self.get,
        set = (b: B) => (s: S) => self.set(that.set(b)(self.get(s)))(s)
      )

    def ⋅[B](that: Optional[A, B]): Optional[S, B] = ???

    def ⋅[B](that: Prism[A, B]): Optional[S, B] = ???

    def ⋅[B](that: Traversal[A, B]): Traversal[S, B] = ???

    final def updated(f: A => A): S => S =
      (s: S) => self.set(f(self.get(s)))(s)
  }

  //
  // EXERCISE 2
  //
  // Create a version of `org2` that uses lenses to update the salaries.
  //
  lazy val org2 =
    org.copy(
      site = org.site.copy(
        manager = org.site.manager.copy(
          salary = org.site.manager.salary * 0.95
        )
      )
    )

//  in mutable code (procedural programming):
  org.site.manager.salary * 0.95 // Optics help us copy immutable data in this way

// All Optics will have:
  type Optic[S, T, A, B]
  // S  = superstructure before the operation
  // T  = superstructure after the operation
  // A  = substructure before the operation
  // B  = substructure after the operation

//  List[A'] => List[B']
//  S = List[A']
//  T = List[B']
//  A = A'
//  B = B'

  type Optic_SimpleVersion[S, A]

  import Org.site
  import Site.manager
  import Employee.salary
  lazy val org2_lens: Org = {
//    val rr = ((site ⋅ manager ⋅ salary).updated(_ * 0.95) andThen
//      (Org.name).updated(_ => "New org name"))(org)
//    rr
    ???
  }

  //
  // EXERCISE 3
  //
  // Implement `⋅` for `Prism` for `Prism`.
  //
  final case class Prism[S, A](get: S => Option[A], set: A => S) { self =>
    def ⋅[B](that: Prism[A, B]): Prism[S, B] = {
      Prism[S, B](
        get = (s: S) => self.get(s).flatMap(that.get),
        set = self.set compose that.set
      )
    }

    def ⋅[B](that: Lens[A, B]): Optional[S, B] = ???

    def ⋅[B](that: Traversal[A, B]): Traversal[S, B] = ???

    final def select(implicit ev: Unit =:= A): S =
      set(ev(()))
  }

  //
  // EXERCISE 4
  //
  // Implement `_Left` and `_Right`.
  //
  def _Left[A, B]: Prism[Either[A, B], A]  = ???
  def _Right[A, B]: Prism[Either[A, B], B] = ???

  //
  // EXERCISE 5
  //
  // Implement `⋅` for `Optional` for `Optional`.
  //
  final case class Optional[S, A](get: S => Option[A], set: A => (S => S)) {
    def ⋅[B](that: Optional[A, B]): Optional[S, B] = ???

    def ⋅[B](that: Lens[A, B]): Optional[S, B] = ???

    def ⋅[B](that: Prism[A, B]): Optional[S, B] = ???

    def ⋅[B](that: Traversal[A, B]): Traversal[S, B] = ???
  }

  //
  // EXERCISE 6
  //
  // Implement `⋅` for `Traversal` for `Traversal`.
  //
  trait Traversal[S, A] { self =>
    def modifyF[F[_]: Applicative](f: A => F[A])(s: S): F[S]

    def ⋅[B](that: Traversal[A, B]): Traversal[S, B] = ???

    def ⋅[B](that: Optional[A, B]): Traversal[S, B] = ???

    def ⋅[B](that: Lens[A, B]): Traversal[S, B] = ???

    def ⋅[B](that: Prism[A, B]): Traversal[S, B] = ???
  }

  //
  // EXERCISE 7
  //
  // Set the country of `org` using a `Prism`.
  //
  (Org.site ⋅ Site.address ⋅ Address.country ⋅ Country.usa)
}

object recursion {
  object manual {
    sealed trait Json { self =>
      import Json._

      def fold0[Z](pf: PartialFunction[(Z, Json), Z]): Z = ???

      def fold1[Z](
        ifNull: Z => Z,
        ifBool: (Z, Boolean) => Z,
        ifStr: (Z, String) => Z,
        ifObj: (Z, Map[String, Json]) => Z,
        ifArr: (Z, List[Json]) => Z
      ): Z = ???

      def fold[Z](
        ifNull: => Z,
        ifBool: Boolean => Z,
        ifStr: String => Z,
        ifObj: Map[String, Z] => Z,
        ifArr: List[Z] => Z
      ): Z = self match {
        case Json.Null        => ifNull
        case Json.Bool(value) => ifBool(value)
        case Json.Str(value)  => ifStr(value)
        case Json.Obj(values) => ifObj(values.mapValues(_.fold(ifNull, ifBool, ifStr, ifObj, ifArr)))
        case Json.Arr(values) => ifArr(values.map(_.fold(ifNull, ifBool, ifStr, ifObj, ifArr)))
      }
    }
    object Json {
      case object Null                                extends Json
      final case class Bool(value: Boolean)           extends Json
      final case class Str(value: String)             extends Json
      final case class Obj(values: Map[String, Json]) extends Json
      final case class Arr(values: List[Json])        extends Json
    }

    def sum1(list: List[Int]): Int =
      list match {
        case Nil     => 0
        case x :: xs => x + sum1(xs)
      }

    def sum2(list: List[Int], acc: Int): Int =
      list match {
        case Nil     => acc
        case x :: xs => sum2(xs, x + acc)
      }

    def sum3(list: List[Int]): Int = list.foldLeft(0)(_ + _)

    def keys(json: Json): Set[String] =
      json.fold[Set[String]](
        Set(),
        _ => Set(),
        _ => Set(),
        (map: Map[String, Set[String]]) => map.keys.toSet ++ map.values.flatten,
        _.toSet.flatten
      )
  }

  object fixpoint {
    sealed trait JsonF[+A]
    object JsonF {
      case object Null                                extends JsonF[Nothing]
      final case class Bool(value: Boolean)           extends JsonF[Nothing]
      final case class Str(value: String)             extends JsonF[Nothing]
      final case class Obj[A](values: Map[String, A]) extends JsonF[A]
      final case class Arr[A](values: List[A])        extends JsonF[A]

      implicit val FunctorJsonF: Functor[JsonF] =
        new Functor[JsonF] {
          def map[A, B](fa: JsonF[A])(f: A => B): JsonF[B] =
            fa match {
              case Str(value)  => Str(value)
              case Bool(value) => Bool(value)
              case Null        => Null
              case Obj(values) => Obj(values.mapValues(f))
              case Arr(values) => Arr(values.map(f))
            }
        }
    }

    val array: JsonF[JsonF[JsonF[Nothing]]] =
      JsonF.Arr(JsonF.Arr(JsonF.Bool(true) :: Nil) :: JsonF.Bool(false) :: Nil)

    // type IndefiniteJson = JsonF[IndefiniteJson]

    final case class Fix[F[_]](unfix: F[Fix[F]]) {
      def cata[Z](alg: F[Z] => Z)(implicit F: Functor[F]): Z = alg(F.map(unfix)(_.cata(alg)))
    }

    type Json = Fix[JsonF]
    object Json {
      def bool(v: Boolean): Json          = Fix[JsonF](JsonF.Bool(v))
      val jnull: Json                     = Fix[JsonF](JsonF.Null)
      def str(v: String): Json            = Fix[JsonF](JsonF.Str(v))
      def obj(v: Map[String, Json]): Json = Fix[JsonF](JsonF.Obj(v))
      def arr(v: List[Json]): Json        = Fix[JsonF](JsonF.Arr(v))
    }

    val json: Json =
      Json.obj(
        Map(
          "name" -> Json.str("John Doe"),
          "age"  -> Json.str("42")
        )
      )

    type Algebra[F[_], A] = F[A] => A

    val CollectKeys: Algebra[JsonF, Set[String]] = {
      case JsonF.Obj(v) => v.keys.toSet ++ v.values.flatten
      case JsonF.Arr(v) => v.toSet.flatten
      case _            => Set()
    }

    val CollectIndices: Algebra[JsonF, Set[Int]] = {
      case JsonF.Obj(v) => v.values.toSet.flatten
      case JsonF.Arr(v) => v.toSet.flatten ++ (0 to v.length).toSet
      case _            => Set()
    }

    def product[F[_]: Functor, A, B](left: Algebra[F, A], right: Algebra[F, B]): Algebra[F, (A, B)] =
      (fab: F[(A, B)]) => (left(fab.map(_._1)), right(fab.map(_._2)))

    def keys(json: Json): Set[String] = json.cata(CollectKeys)

    def indices(json: Json): Set[Int] = json.cata(CollectIndices)

    val KeysAndIndices = product(CollectKeys, CollectIndices)
  }

  object composition {
    import fixpoint._
    import scalaz._
    import Scalaz._

    sealed trait NewLeafF[+A]
    final case class DateTime(value: java.time.LocalDate) extends NewLeafF[Nothing]

    type ExtendedJson = Fix[Coproduct[JsonF, NewLeafF, ?]]

    def upgrade[A](jsonF: JsonF[A]): Coproduct[JsonF, NewLeafF, A] =
      jsonF match {
        case JsonF.Str(value) => ???
        case x                => Coproduct(-\/(x))
      }
  }
}

object comonads {
  case class NonEmptyList[A](head: A, tail: List[A])

  type Tuple[A] = (String, A)

  trait Monad[F[_]] {
    def point[A](a: => A): F[A]

    def map[A, B](fa: F[A])(f: A => B): F[B]

    def join[A](fa: F[F[A]]): F[A]
  }

  trait Comonad[F[_]] {
    def copoint[A](fa: F[A]): A

    def map[A, B](fa: F[A])(f: A => B): F[B]

    def cojoin[A](fa: F[A]): F[F[A]]
  }

  case class Cofree[F[_], A](head: A, tail: F[Cofree[F, A]]) { self =>
    def cojoin(implicit F: Functor[F]): Cofree[F, Cofree[F, A]] =
      Cofree(self, tail.map(_.cojoin))
  }
}

object validation {
  final case class Validate[+E, +A](run: Either[List[E], A]) { self =>
    def zip[E1 >: E, B](that: Validate[E1, B]): Validate[E1, (A, B)] =
      Validate((self.run, that.run) match {
        case (Left(el), Left(er)) => Left(el ++ er)
        case (Left(e), _)         => Left(e)
        case (_, Left(e))         => Left(e)
        case (Right(a), Right(b)) => Right((a, b))
      })
  }
}

object state {
  sealed trait Input

  trait GameWorld

  type GameFunction = (GameWorld, Input) => GameWorld

  case class Stats(health: Int, stamina: Int)

  val stats = Stats(100, 100)

  final case class State[S, +A](run: S => (S, A)) { self =>
    def map[B](f: A => B): State[S, B] =
      State[S, B](
        state =>
          self.run(state) match {
            case (state, a) => (state, f(a))
          }
      )

    def flatMap[B](f: A => State[S, B]): State[S, B] =
      State[S, B](
        state =>
          self.run(state) match {
            case (state, a) => f(a).run(state)
          }
      )

    def zip[B](that: State[S, B]): State[S, (A, B)] =
      self.flatMap(a => that.map(b => (a, b)))
  }
  object State {
    def succeed[S, A](a: A): State[S, A] = ???

    def get[S]: State[S, S] = State[S, S](state => (state, state))

    def set[S](s: S): State[S, Unit] = State[S, Unit](state => (s, ()))

    def update[S](f: S => S): State[S, S] = State[S, S] { state =>
      val newState = f(state)
      (newState, newState)
    }
  }

  def updateHealth(adjust: Int): State[Stats, Int] =
    State.update[Stats](s => s.copy(health = s.health + adjust)).map(_.health)

  def updateStamina(adjust: Int): State[Stats, Int] =
    State.update[Stats](s => s.copy(stamina = s.stamina + adjust)).map(_.stamina)

  val update: State[Stats, (Int, Int)] =
    for {
      newHealth  <- updateHealth(50)
      newStamina <- updateStamina(25)
    } yield (newHealth, newStamina)

  val (stats2, (newHealth, newStamina)) = update.run(stats)
}

object tagless_final {
  import zio._

  trait Console[F[_]] {
    def putStrLn(line: String): F[Unit]

    val getStrLn: F[String]
  }
  object Console {
    def apply[F[_]](implicit F: Console[F]): Console[F] = F

    def putStrLn[F[_]: Console](line: String): F[Unit] = Console[F].putStrLn(line)
    def getStrLn[F[_]: Console]: F[String]             = Console[F].getStrLn

    implicit val ConsoleTask: Console[Task] =
      new Console[Task] {
        def putStrLn(line: String): Task[Unit] = Task(println(line))

        val getStrLn: Task[String] = Task(scala.io.StdIn.readLine())
      }
  }

  case class TestTask[A]()

  def program[F[_]: Console]: F[String] =
    Console.getStrLn[F]

  val programTask: Task[String] = program[Task]

  // val programTestTask: TestTask[String] = program[TestTask]

}
