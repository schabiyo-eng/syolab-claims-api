package dev.syolab.claims.domain;

public record ValidationFinding(FindingCode code, Severity severity, String message) {}
