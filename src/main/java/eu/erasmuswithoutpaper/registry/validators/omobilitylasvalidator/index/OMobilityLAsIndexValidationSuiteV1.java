package eu.erasmuswithoutpaper.registry.validators.omobilitylasvalidator.index;

import java.util.Arrays;

import eu.erasmuswithoutpaper.registry.validators.AbstractValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.Combination;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.ValidationStepWithStatus.Status;
import eu.erasmuswithoutpaper.registry.validators.omobilitylasvalidator.OMobilityLAsSuiteState;
import eu.erasmuswithoutpaper.registry.validators.verifiers.Verifier;
import eu.erasmuswithoutpaper.registry.validators.verifiers.VerifierFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Describes the set of test/steps to be run on an OMobilities API index endpoint implementation
 * in order to properly validate it.
 */
class OMobilityLAsIndexValidationSuiteV1
    extends AbstractValidationSuite<OMobilityLAsSuiteState> {
  private static final Logger logger =
      LoggerFactory.getLogger(
          OMobilityLAsIndexValidationSuiteV1.class);
  private static final ValidatedApiInfo apiInfo = new OMobilityLAsIndexValidatedApiInfo();

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  public ValidatedApiInfo getApiInfo() {
    return apiInfo;
  }

  OMobilityLAsIndexValidationSuiteV1(ApiValidator<OMobilityLAsSuiteState> validator,
                                     OMobilityLAsSuiteState state, ValidationSuiteConfig config) {
    super(validator, state, config);
  }

  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is OMobilityLAsSuiteState not just SuiteState
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void validateCombinationAny(Combination combination)
      throws SuiteBroken {
    Verifier hasAnyElementVerifier = omobilityIdVerifierFactory.expectResponseToBeNotEmpty();
    hasAnyElementVerifier.setCustomErrorMessage("However we received an empty response, no"
        + " omobility ids were returned. Some tests will be skipped. To perform more tests"
        + " provide parameters that will allow us to receive omobility-ids.");
    testParameters200(
        combination,
        "Request one known sending_hei_id, expect 200 OK.",
        new ParameterList(
            new Parameter("sending_hei_id", this.currentState.sendingHeiId)
        ),
        hasAnyElementVerifier,
        Status.FAILURE
    );

    final boolean noOMobilityIdReturned = !hasAnyElementVerifier.getVerificationResult();
    final String noOMobilitySkipReason = "OMobilities list was empty.";

    testsRequestingReceivingHeiIds(combination,
        "receiving_hei_id",
        this.currentState.receivingHeiId,
        "sending_hei_id",
        this.currentState.sendingHeiId,
        noOMobilityIdReturned,
        noOMobilitySkipReason,
        omobilityIdVerifierFactory,
        true
    );

    testReceivingAcademicYears(combination,
        "sending_hei_id",
        this.currentState.sendingHeiId,
        omobilityIdVerifierFactory
    );

    // Modified since
    modifiedSinceTests(combination,
        "sending_hei_id",
        this.currentState.sendingHeiId,
        true,
        noOMobilityIdReturned,
        noOMobilitySkipReason,
        omobilityIdVerifierFactory
    );

    // Permission tests
    // Am I allowed to see omobilities issued by others?
    testParameters200(
        combination,
        "Request with known sending_hei_id and receiving_hei_id valid but not covered by"
            + " the validator, expect empty response.",
        new ParameterList(
            new Parameter("sending_hei_id", this.currentState.sendingHeiId),
            new Parameter("receiving_hei_id", this.currentState.notPermittedHeiId)
        ),
        omobilityIdVerifierFactory.expectResponseToBeEmpty(),
        Status.WARNING,
        noOMobilityIdReturned,
        noOMobilitySkipReason
    );

    // Are others able to see omobilities issued by me?
    testParameters200AsOtherEwpParticipant(
        combination,
        "Request one known sending_hei_id as other EWP participant, expect 200 OK and empty "
            + "response.",
        new ParameterList(
            new Parameter("sending_hei_id", this.currentState.sendingHeiId)
        ),
        omobilityIdVerifierFactory.expectResponseToBeEmpty(),
        Status.FAILURE,
        noOMobilityIdReturned,
        noOMobilitySkipReason
    );
  }

  private VerifierFactory omobilityIdVerifierFactory = new VerifierFactory(
      Arrays.asList("omobility-id"));
}
