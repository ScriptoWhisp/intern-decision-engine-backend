# TICKET-101 Validation Report

## What Was Good in the Original Code

- **Clear structure and method naming**  
  The initial code had a reasonably straightforward structure: a single `DecisionEngine` class with methods for validating inputs, computing the highest possible loan, and determining the credit modifier. Despite not fulfilling all of the project requirements, the naming of methods and the general flow (validate inputs → compute modifier → decide on the loan) made it relatively simple to understand the overall process.

- **Handling the main input checks**  
  The original solution already contained checks for input values (loan amount, loan period, and personal code). Although some of them needed to be fine-tuned, the presence of these validations showed a good approach to error handling.

---

# Updated Code Changes and Reasons

## Overview

As part of TICKET-101, the code has been updated to fully comply with the credit decision business requirements:

1. **Support four scenarios** based on specific personal codes (either debt or different segments).  
2. **Validate input** (loan amount in [2000..10000] and loan period in [12..48]).  
3. **Calculate and decide the maximum loan** or adjust the period if the requested combination is not feasible.  
4. **Reject** cases where no valid loan can be approved within the constraints.

Below is a summary of the **changes** made in the code and **why** these changes were essential.

---

## Detailed Changes and Justifications

1. **Fixed loan amount and period validation**  
   - Previously, some checks used confusing negations (`!(x <= ...)`).  
   - Now the `verifyInputs` method clearly confirms that:
     - The amount is between `2000` and `10000` inclusive.
     - The period is between `12` and `48` inclusive.  
   This removes potential confusion and aligns with the exact business rules.

2. **Set the maximum period to 48**  
   - By requirement, `MAXIMUM_LOAN_PERIOD` must be 48, not 60.  
   - This change ensures the code does not allow loan terms beyond 48 months.

3. **Implemented cleaner logic for “approving more or less”**  
   - If a user requests an amount and the engine can approve **more**, the code now returns the larger amount (up to 10000).  
   - If the request is too high but at least a smaller amount can be approved (≥ 2000), the code returns that smaller amount.  
   - If even the minimum amount (2000) cannot be approved at the requested period, the engine attempts to **increase** the period (up to 48 months) to see if that helps (As task does not specify, we try to find period for initial amount, as it seems more logic to the author).

4. **Added a `neededPeriodFor(int loanAmount)` method**  
   - Instead of incrementing the period blindly, we now compute the “needed period” using the formula \(\lceil \frac{\text{loanAmount}}{\text{creditModifier}} \rceil\).  
   - This directly follows from the scoring condition `score >= 0.1` and is more efficient and transparent than a loop.

5. **Updated `getCreditModifier` with the four specific scenarios**  
   - The method now maps each of the four personal codes to the correct modifier (or zero for debt) as stated in the task:
     - Debt/NA → 0  
     - Segment 1, 2, and 3 → distinct modifier values  
   - This precisely reflects the four scenarios required by TICKET-101.

6. **Renamed and clarified variables and methods for readability**  
   - For example, `loanPeriod` → `loanPeriodMonths`.  
   - Introduced `neededPeriodFor`, which clearly indicates the calculation of the required period.  
   - Overall, these changes make the code more understandable and maintainable.

---
## Fixed and Extended Tests

- **Fixed existing tests** that previously expected incorrect boundary checks or periods up to 60 months. Now, all tests respect the [12..48] range for the loan period and [2000..10000] for the loan amount.
- **Extended coverage** to ensure the system correctly handles the more scenarios.
- **Improved personal codes** codes are changed to comply with 4 scenarios constraint.

---

## DecisionType Enum and Revised Controller

- A new **`DecisionType` enum** was introduced to differentiate between approved and declined decisions (and potentially more statuses in the future). This makes the response more explicit and paves the way for extended decision statuses if needed.
- The **controller** has been updated to reflect these changes:
    - **`DecisionResponse`** now includes a `decision` field (of type `DecisionType`) instead of solely relying on a null or non-null error message to determine success vs. failure.
    - **HTTP response codes** are aligned with the new logic:
        - `400 Bad Request` for invalid inputs (period, amount, or personal code).
        - `404 Not Found` for no valid loan scenarios.
        - `500 Internal Server Error` for unexpected failures.

---

## The Most Critical Issue That Was Fixed

**The main shortcoming** in the previous solution was **not accounting for the requirement** to either increase or decrease the requested loan and/or adjust the period to find the maximum possible sum within the constraints. Earlier code did not always return the higher possible approved amount or correctly handle an extended period beyond the initially requested one.

Now, the logic ensures:

- If the requested amount is **less** than the maximum feasible, the engine returns the **greater** amount.  
- If the requested amount is **too high** but a smaller one is feasible, the engine returns that smaller sum.  
- If the loan cannot be approved at the requested period, the engine tries **extending** the period (for the initial amount) up to 48.  
- It rejects the request if no valid combination exists.

This fully meets the specification of TICKET-101.

---

## Conclusion

- The code now meets all TICKET-101 requirements:
  - Checking personal codes for debt/segment.  
  - Enforcing 2000–10000 for loan amounts and 12–48 months for the period.  
  - Providing logic to adjust the requested amount/period to the best valid option.  
  - Rejecting if no valid solution exists.  
- The updates enhance **readability** and **reliability** of the solution, making it easier to maintain in the future.  
- The primary improvement is the **correct implementation** of finding an “optimal” combination of loan amount and period in compliance with the specification.

For a more detailed overview of the changes, please refer to the commit history https://github.com/ScriptoWhisp/intern-decision-engine-backend/pull/1/commits
