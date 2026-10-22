# Flag claims over the policy limit

Adjusters want a clearer signal when a claim goes over the policy limit. The finding today is `EXCEEDS_COVERAGE`. Replace it with `OVER_POLICY_LIMIT`.

## Acceptance criteria

1. Replace the `EXCEEDS_COVERAGE` finding with `OVER_POLICY_LIMIT` (severity `ERROR`). A claim that trips this rule is `REJECTED`.
2. A claim is over the policy limit when its amount is greater than or equal to the policy coverage limit. Compare that claim's amount with the coverage limit on the policy.
3. Add unit tests, and only these cases for the new rule:
   - Amount `15000.00` on a policy with coverage limit `10000.00`, no other claims, required documents present: status `REJECTED` and finding `OVER_POLICY_LIMIT`.
   - Amount `5000.00` on a policy with coverage limit `10000.00`, no other claims, required documents present: `OVER_POLICY_LIMIT` is absent.
4. Do not weaken or delete existing tests. They are the regression net.

## Out of scope

Fraud wording, required documents, and inactive policies stay as they are.
