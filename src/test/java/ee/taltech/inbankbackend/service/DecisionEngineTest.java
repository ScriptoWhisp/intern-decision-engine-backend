package ee.taltech.inbankbackend.service;

import com.github.vladislavgoltjajev.personalcode.exception.PersonalCodeException;
import ee.taltech.inbankbackend.config.DecisionEngineConstants;
import ee.taltech.inbankbackend.config.DecisionType;
import ee.taltech.inbankbackend.exceptions.InvalidLoanAmountException;
import ee.taltech.inbankbackend.exceptions.InvalidLoanPeriodException;
import ee.taltech.inbankbackend.exceptions.InvalidPersonalCodeException;
import ee.taltech.inbankbackend.exceptions.NoValidLoanException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DecisionEngineTest {

    @InjectMocks
    private DecisionEngine decisionEngine;

    private String debtorPersonalCode;
    private String segment1PersonalCode;
    private String segment2PersonalCode;
    private String segment3PersonalCode;

    @BeforeEach
    void setUp() {
        debtorPersonalCode = "49002010965";
        segment1PersonalCode = "49002010976";
        segment2PersonalCode = "49002010987";
        segment3PersonalCode = "49002010998";
    }

    @Test
    void givenInvalidPersonalCode_whenCalculateApprovedLoan_thenThrowsInvalidPersonalCodeException() {
        assertThrows(
                InvalidPersonalCodeException.class,
                () -> decisionEngine.calculateApprovedLoan("00000000000", 4000L, 12)
        );
    }

    @Test
    void givenTooYoungCustomer_whenCalculateApprovedLoan_thenThrowsInvalidPersonalCodeException() {
        assertThrows(
                InvalidPersonalCodeException.class,
                () -> decisionEngine.calculateApprovedLoan("50901230240", 4000L, 12)
        );
    }

    @Test
    void givenTooOldCustomer_whenCalculateApprovedLoan_thenThrowsInvalidPersonalCodeException() {
        assertThrows(
                InvalidPersonalCodeException.class,
                () -> decisionEngine.calculateApprovedLoan("30901230240", 4000L, 12)
        );
    }

    @Test
    void givenNullLoanAmount_whenCalculateApprovedLoan_thenThrowsInvalidLoanAmountException() {
        assertThrows(
                InvalidLoanAmountException.class,
                () -> decisionEngine.calculateApprovedLoan(segment1PersonalCode, null, 12)
        );
    }

    @Test
    void givenLoanAmountBelowMinimum_whenCalculateApprovedLoan_thenThrowsInvalidLoanAmountException() {
        Long belowMin = DecisionEngineConstants.MINIMUM_LOAN_AMOUNT.longValue() - 1;
        assertThrows(
                InvalidLoanAmountException.class,
                () -> decisionEngine.calculateApprovedLoan(segment1PersonalCode, belowMin, 12)
        );
    }

    @Test
    void givenLoanAmountAboveMaximum_whenCalculateApprovedLoan_thenThrowsInvalidLoanAmountException() {
        Long aboveMax = DecisionEngineConstants.MAXIMUM_LOAN_AMOUNT.longValue() + 1;
        assertThrows(
                InvalidLoanAmountException.class,
                () -> decisionEngine.calculateApprovedLoan(segment1PersonalCode, aboveMax, 12)
        );
    }

    @Test
    void givenLoanPeriodBelowMinimum_whenCalculateApprovedLoan_thenThrowsInvalidLoanPeriodException() {
        int belowMin = DecisionEngineConstants.MINIMUM_LOAN_PERIOD - 1;
        assertThrows(
                InvalidLoanPeriodException.class,
                () -> decisionEngine.calculateApprovedLoan(segment1PersonalCode, 4000L, belowMin)
        );
    }

    @Test
    void givenLoanPeriodAboveMaximum_whenCalculateApprovedLoan_thenThrowsInvalidLoanPeriodException() {
        int aboveMax = DecisionEngineConstants.MAXIMUM_LOAN_PERIOD + 1;
        assertThrows(
                InvalidLoanPeriodException.class,
                () -> decisionEngine.calculateApprovedLoan(segment1PersonalCode, 4000L, aboveMax)
        );
    }

    @Test
    void givenDebtorPersonalCode_whenCalculateApprovedLoan_thenThrowsNoValidLoanException() {
        assertThrows(
                NoValidLoanException.class,
                () -> decisionEngine.calculateApprovedLoan(debtorPersonalCode, 4000L, 12)
        );
    }

    @Test
    void givenDebtorEvenAtMaxPeriod_whenCalculateApprovedLoan_thenThrowsNoValidLoanException() {
        assertThrows(
                NoValidLoanException.class,
                () -> decisionEngine.calculateApprovedLoan(debtorPersonalCode, 10000L, 48)
        );
    }

    @Test
    void givenValidChecksButCannotOfferLoan_whenCalculateApprovedLoan_thenThrowsNoValidLoanException() {
        assertThrows(
                NoValidLoanException.class,
                () -> decisionEngine.calculateApprovedLoan(segment1PersonalCode, 10000L, 12)
        );
    }

    @Test
    void givenUnrecognizedButFormatValidCode_whenCalculateApprovedLoan_thenThrowsNoValidLoanException() {
        assertThrows(
                NoValidLoanException.class,
                () -> decisionEngine.calculateApprovedLoan("50401230240", 4000L, 12)
        );
    }

    @Test
    void givenSegment1PersonalCodeAndExtendedPeriodNeeded_whenCalculateApprovedLoan_thenApproves()
            throws PersonalCodeException, InvalidPersonalCodeException, InvalidLoanAmountException,
            InvalidLoanPeriodException, NoValidLoanException {

        Decision decision = decisionEngine.calculateApprovedLoan(segment1PersonalCode, 4000L, 12);
        assertEquals(4000, decision.getLoanAmount());
        assertEquals(40, decision.getLoanPeriod());
        assertEquals(DecisionType.APPROVED, decision.getDecision());
    }

    @Test
    void givenSegment2PersonalCodeAndPartialLoan_whenCalculateApprovedLoan_thenApprovesMaxForPeriod()
            throws PersonalCodeException, InvalidPersonalCodeException, InvalidLoanAmountException,
            InvalidLoanPeriodException, NoValidLoanException {

        Decision decision = decisionEngine.calculateApprovedLoan(segment2PersonalCode, 4000L, 12);
        assertEquals(3600, decision.getLoanAmount());
        assertEquals(12, decision.getLoanPeriod());
        assertEquals(DecisionType.APPROVED, decision.getDecision());
    }

    @Test
    void givenSegment3PersonalCodeAndHighModifier_whenCalculateApprovedLoan_thenApprovesMaxLoan()
            throws PersonalCodeException, InvalidPersonalCodeException, InvalidLoanAmountException,
            InvalidLoanPeriodException, NoValidLoanException {

        Decision decision = decisionEngine.calculateApprovedLoan(segment3PersonalCode, 4000L, 12);
        assertEquals(10000, decision.getLoanAmount());
        assertEquals(12, decision.getLoanPeriod());
        assertEquals(DecisionType.APPROVED, decision.getDecision());
    }

    @Test
    void givenSegment1AndCannotCoverRequested_whenCalculateApprovedLoan_thenFindsSuitableLongerPeriod()
            throws PersonalCodeException, InvalidPersonalCodeException, InvalidLoanAmountException,
            InvalidLoanPeriodException, NoValidLoanException {

        Decision decision = decisionEngine.calculateApprovedLoan(segment1PersonalCode, 3000L, 12);
        assertEquals(3000, decision.getLoanAmount());
        assertEquals(30, decision.getLoanPeriod());
    }
}
