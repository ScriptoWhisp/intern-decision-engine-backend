package ee.taltech.inbankbackend.service;

import com.github.vladislavgoltjajev.personalcode.locale.estonia.EstonianPersonalCodeValidator;
import ee.taltech.inbankbackend.config.DecisionEngineConstants;
import ee.taltech.inbankbackend.config.DecisionType;
import ee.taltech.inbankbackend.exceptions.InvalidLoanAmountException;
import ee.taltech.inbankbackend.exceptions.InvalidLoanPeriodException;
import ee.taltech.inbankbackend.exceptions.InvalidPersonalCodeException;
import ee.taltech.inbankbackend.exceptions.NoValidLoanException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * A service class that provides a method for calculating an approved loan amount and period for a customer.
 * The loan amount is calculated based on the customer's credit modifier,
 * which is determined by the last four digits of their ID code.
 */
@Service
@RequiredArgsConstructor
public class DecisionEngine {

    private final EstonianPersonalCodeValidator validator = new EstonianPersonalCodeValidator();
    private int creditModifier;

    /**
     * Calculates the maximum loan amount and period for the customer based on their ID code,
     * the requested loan amount and the loan period.
     * The loan period must be between 12 and 48 months (inclusive).
     * The loan amount must be between 2000 and 10000 euros (inclusive).
     *
     * @param personalCode ID code of the customer that made the request.
     * @param loanAmount Requested loan amount
     * @param loanPeriodMonths Requested loan period
     * @return A Decision object containing the approved loan amount and period, and an error message (if any)
     * @throws InvalidPersonalCodeException If the provided personal ID code is invalid
     * @throws InvalidLoanAmountException If the requested loan amount is invalid
     * @throws InvalidLoanPeriodException If the requested loan period is invalid
     * @throws NoValidLoanException If there is no valid loan found for the given ID code, loan amount and loan period
     */
    public Decision calculateApprovedLoan(String personalCode, Long loanAmount, int loanPeriodMonths)
            throws InvalidPersonalCodeException, InvalidLoanAmountException, InvalidLoanPeriodException,
            NoValidLoanException {
        // Verify initial inputs
        verifyInputs(personalCode, loanAmount, loanPeriodMonths);

        creditModifier = getCreditModifier(personalCode);

        // If the customer has debt then creditModifier is 0 – no loan is approved.
        if (creditModifier == 0) {
            throw new NoValidLoanException("No valid loan found! Customer has debt.");
        }

        int maxLoanForPeriod = highestValidLoanAmount(loanPeriodMonths);
        // 1. variant -> requested less than we can offer
        if (loanAmount <= maxLoanForPeriod) {
            return new Decision(maxLoanForPeriod, loanPeriodMonths, DecisionType.APPROVED);
        }

        // 2. variant -> requested more than we can offer, but we can maybe offer less
        if (maxLoanForPeriod >= DecisionEngineConstants.MINIMUM_LOAN_AMOUNT) {
            return new Decision(maxLoanForPeriod, loanPeriodMonths, DecisionType.APPROVED);
        }

        // 3. variant -> we cannot offer anything in the requested period, but we can offer something in a longer period
        int neededPeriod = neededPeriodFor(loanAmount.intValue());
        if (neededPeriod <= DecisionEngineConstants.MAXIMUM_LOAN_PERIOD && neededPeriod >= DecisionEngineConstants.MINIMUM_LOAN_PERIOD) {
            return new Decision(loanAmount.intValue(), neededPeriod, DecisionType.APPROVED);
        }

        // 4. variant -> we cannot offer anything
        throw new NoValidLoanException("No valid loan found!");
    }

    /**
     * Calculates the period needed to pay off the loan amount.
     *
     * @param loanAmount Amount of the loan
     * @return Period needed to pay off the loan
     */
    private int neededPeriodFor(int loanAmount) {
        return (int) Math.ceil((double) loanAmount / creditModifier);
    }

    /**
     * Calculates the largest valid loan for the current credit modifier and loan period.
     *
     * @return Largest valid loan amount
     */
    private int highestValidLoanAmount(int loanPeriod) {
        return creditModifier * loanPeriod > DecisionEngineConstants.MAXIMUM_LOAN_AMOUNT ?
                DecisionEngineConstants.MAXIMUM_LOAN_AMOUNT : creditModifier * loanPeriod;
    }

    /**
     * Calculates the credit modifier of the customer to according to the 4 scenarios in the task.
     * Debt - 49002010965
     * Segment 1 - 49002010976
     * Segment 2 - 49002010987
     * Segment 3 - 49002010998
     *
     * @param personalCode ID code of the customer that made the request.
     * @return modifier of segment to which the customer belongs.
     */
    private int getCreditModifier(String personalCode) {
        return switch (personalCode) {
            case "49002010965" -> 0;
            case "49002010976" -> DecisionEngineConstants.SEGMENT_1_CREDIT_MODIFIER;
            case "49002010987" -> DecisionEngineConstants.SEGMENT_2_CREDIT_MODIFIER;
            case "49002010998" -> DecisionEngineConstants.SEGMENT_3_CREDIT_MODIFIER;
            default -> 0;
        };
    }


    /**
     * Verify that all inputs are valid according to business rules.
     * If inputs are invalid, then throws corresponding exceptions.
     *
     * @param personalCode Provided personal ID code
     * @param loanAmount Requested loan amount
     * @param loanPeriodMonths Requested loan period
     * @throws InvalidPersonalCodeException If the provided personal ID code is invalid
     * @throws InvalidLoanAmountException If the requested loan amount is invalid
     * @throws InvalidLoanPeriodException If the requested loan period is invalid
     */
    public void verifyInputs(String personalCode, Long loanAmount, int loanPeriodMonths)
            throws InvalidPersonalCodeException, InvalidLoanAmountException, InvalidLoanPeriodException {

        if (!validator.isValid(personalCode)) {
            throw new InvalidPersonalCodeException("Invalid personal ID code!");
        }
        if (loanAmount == null
                || DecisionEngineConstants.MINIMUM_LOAN_AMOUNT > loanAmount
                || loanAmount > DecisionEngineConstants.MAXIMUM_LOAN_AMOUNT) {
            throw new InvalidLoanAmountException("Invalid loan amount!");
        }
        if (DecisionEngineConstants.MINIMUM_LOAN_PERIOD > loanPeriodMonths
                || loanPeriodMonths > DecisionEngineConstants.MAXIMUM_LOAN_PERIOD) {
            throw new InvalidLoanPeriodException("Invalid loan period!");
        }

    }
}
