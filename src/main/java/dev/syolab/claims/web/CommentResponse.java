package dev.syolab.claims.web;

import dev.syolab.claims.auth.Role;
import dev.syolab.claims.domain.Comment;
import java.time.Instant;

public record CommentResponse(String id, Instant at, String author, Role role, String body) {

  public static CommentResponse from(Comment comment) {
    return new CommentResponse(
        comment.id().toString(), comment.at(), comment.actor(), comment.role(), comment.body());
  }
}
