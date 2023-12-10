package dev.slne.stats.api.stat.repository;

import dev.slne.stats.api.stat.GeneralStat;
import org.jetbrains.annotations.ApiStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * The GeneralStatRepository interface provides methods for accessing and manipulating GeneralStat entities in the database.
 */
@ApiStatus.NonExtendable
@Repository
public interface GeneralStatRepository extends JpaRepository<GeneralStat, Long> {

	/**
	 * Finds the general statistics by stat owner and server.
	 *
	 * @param statOwner the UUID of the stat owner
	 * @param server    the server name
	 * @return a CompletableFuture containing a list of GeneralStat objects that match the given stat owner and server
	 */
	@Async
	CompletableFuture<List<GeneralStat>> findByStatOwnerAndServer(@NonNull UUID statOwner, @NonNull String server);
}
