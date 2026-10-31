package dev.syolab.claims.store;

import dev.syolab.claims.domain.Activity;
import dev.syolab.claims.domain.Comment;
import dev.syolab.claims.domain.DecisionChange;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class ActivityLog {

  private final List<Activity> entries = new ArrayList<>();
  private final Map<UUID, List<Activity>> byClaim = new HashMap<>();

  public synchronized void clear() {
    entries.clear();
    byClaim.clear();
  }

  public synchronized void append(Activity activity) {
    entries.add(activity);
    byClaim.computeIfAbsent(activity.claimId(), id -> new ArrayList<>()).add(activity);
  }

  public synchronized List<DecisionChange> history(UUID claimId) {
    return byClaim.getOrDefault(claimId, List.of()).stream()
        .filter(DecisionChange.class::isInstance)
        .map(DecisionChange.class::cast)
        .toList();
  }

  public synchronized List<Comment> comments(UUID claimId) {
    return byClaim.getOrDefault(claimId, List.of()).stream()
        .filter(Comment.class::isInstance)
        .map(Comment.class::cast)
        .toList();
  }

  public synchronized List<Activity> all() {
    return List.copyOf(entries);
  }
}
