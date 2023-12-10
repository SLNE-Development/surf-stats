package dev.slne.stats.api.stat.repository;

import dev.slne.stats.api.stat.MobStat;
import org.jetbrains.annotations.ApiStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * The MobStatRepository interface extends the JpaRepository interface and provides
 * methods for retrieving and manipulating MobStat objects in the database.
 */
@ApiStatus.NonExtendable
@Repository
public interface MobStatRepository extends JpaRepository<MobStat, Long> {

	/**
	 * Retrieves a list of MobStat objects based on the provided stat owner and server.
	 *
	 * @param statOwner The UUID of the stat owner.
	 * @param server The name of the server.
	 * @return A CompletableFuture that completes with a List of MobStat objects representing
	 *         the retrieved MobStats.
	 */
	@Async
	CompletableFuture<List<MobStat>> findByStatOwnerAndServer(@NonNull UUID statOwner, @NonNull String server);
}
