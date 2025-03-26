package ee.taltech.inbankbackend.endpoint;

import ee.taltech.inbankbackend.config.DecisionType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

/**
 * Holds the response data of the REST endpoint.
 */
@Getter
@Setter
@Component
public class DecisionResponse {
    private Integer loanAmount;
    private Integer loanPeriod;
    private DecisionType decision;
    private String errorMessage;
}
