package dev.slne.stats.api.stat.repository;

import dev.slne.stats.api.stat.ItemStat;
import org.jetbrains.annotations.ApiStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Repository interface for ItemStat entities.
 * This interface provides methods to interact with the database for ItemStat entities.
 */
@ApiStatus.NonExtendable
@Repository
public interface ItemStatRepository extends JpaRepository<ItemStat, Long> {

	/**
	 * Finds the ItemStat entities based on the stat owner and server.
	 *
	 * @param statOwner The UUID of the stat owner.
	 * @param server    The server.
	 * @return A CompletableFuture that returns a List of ItemStat objects that match the stat owner and server.
	 */
	@Async
	CompletableFuture<List<ItemStat>> findByStatOwnerAndServer(@NonNull UUID statOwner, @NonNull String server);
}
