package dev.slne.stats.api.stat.repository;

import dev.slne.stats.api.stat.ItemStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * The interface Item stat repository.
 */
@Repository
public interface ItemStatRepository extends JpaRepository<ItemStat, Long> {

	/**
	 * Find by stat owner completable future.
	 *
	 * @param statOwner the stat owner
	 * @param server    the server
	 *
	 * @return the completable future
	 */
	@Async
	CompletableFuture<List<ItemStat>> findByStatOwnerAndServer(@NonNull UUID statOwner, @NonNull String server);
}
