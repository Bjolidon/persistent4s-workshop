package infrastructure

trait Repository[F[_], K, V]:

  def find(key: K): F[Option[V]]

  def findMany(keys: List[K]): F[Map[K, Option[V]]]

  def persistMany(states: Map[K, Option[V]]): F[Unit]
