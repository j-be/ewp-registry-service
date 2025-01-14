package eu.erasmuswithoutpaper.registry.validators.iiavalidator.v2.index;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.validators.AbstractSetupValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.HttpSecurityDescription;
import eu.erasmuswithoutpaper.registry.validators.InlineValidationStep;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.ValidationParameter;
import eu.erasmuswithoutpaper.registry.validators.iiavalidator.IiaSuiteState;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import https.github_com.erasmus_without_paper.ewp_specs_api_iias.blob.stable_v2.endpoints.get_response.IiasGetResponse;
import https.github_com.erasmus_without_paper.ewp_specs_api_iias.blob.stable_v2.endpoints.get_response.MobilitySpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

public class IiaIndexComplexSetupValidationSuiteV2
    extends AbstractSetupValidationSuite<IiaSuiteState> {

  private static final Logger logger =
      LoggerFactory.getLogger(IiaIndexComplexSetupValidationSuiteV2.class);

  private static final ValidatedApiInfo apiInfo = new IiaIndexValidatedApiInfoV2();

  @Override
  protected Logger getLogger() {
    return logger;
  }

  private static final String HEI_ID_PARAMETER = "hei_id";
  private static final String IIA_ID_PARAMETER = "iia_id";
  private static final String PARTNER_HEI_ID_PARAMETER = "partner_hei_id";
  private static final String RECEIVING_ACADEMIC_YEAR_ID = "receiving_academic_year_id";
  private static final String IIA_ID_PARAMETER_DESCRIPTION =
      "This parameter is used to fetch partner_hei_id and receiving_academic_year_id using"
          + " GET endpoint.";

  /**
   * Get list of parameters supported by this validator.
   *
   * @return list of supported parameters with dependencies.
   */
  public static List<ValidationParameter> getParameters() {
    return Arrays.asList(
        new ValidationParameter(IIA_ID_PARAMETER)
            .dependsOn(HEI_ID_PARAMETER)
            .blockedBy(PARTNER_HEI_ID_PARAMETER)
            .withDescription(IIA_ID_PARAMETER_DESCRIPTION),
        new ValidationParameter(PARTNER_HEI_ID_PARAMETER)
            .dependsOn(HEI_ID_PARAMETER)
            .blockedBy(IIA_ID_PARAMETER),
        new ValidationParameter(RECEIVING_ACADEMIC_YEAR_ID)
            .dependsOn(HEI_ID_PARAMETER, PARTNER_HEI_ID_PARAMETER)
            .blockedBy(IIA_ID_PARAMETER)
    );
  }

  public IiaIndexComplexSetupValidationSuiteV2(
      ApiValidator<IiaSuiteState> validator,
      IiaSuiteState state,
      ValidationSuiteConfig config) {
    super(validator, state, config);
  }

  // Overriding runTests to skip checking url, api version etc.
  @Override
  protected void runTests(HttpSecurityDescription security) throws SuiteBroken {
    runApiSpecificTests(security);
  }

  @Override
  public ValidatedApiInfo getApiInfo() {
    return apiInfo;
  }

  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is IiaSuiteState not just SuiteState
  @Override
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void runApiSpecificTests(
      HttpSecurityDescription securityDescription) throws SuiteBroken {
    if (isParameterProvided(PARTNER_HEI_ID_PARAMETER)) {
      useUserProvidedParameters();
    } else {
      useGetEndpointToFetchParameters(securityDescription);
    }
  }

  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  private void useGetEndpointToFetchParameters(
      HttpSecurityDescription securityDescription) throws SuiteBroken {
    this.currentState.selectedIiaId = getParameterValue(IIA_ID_PARAMETER,
        () -> getIiaIdParameter(securityDescription));
    String getUrl = getApiUrlForHei(
        this.currentState.selectedHeiId, this.getApiInfo().getApiName(), ApiEndpoint.Get,
        "Retrieving 'get' endpoint url from catalogue.",
        "Couldn't find 'get' endpoint url in the catalogue. Is manifest correct?");

    // call get to get required info about this iia
    this.currentState.selectedIiaInfo = getIiaInfo(
        this.currentState.selectedHeiId,
        this.currentState.selectedIiaId,
        getUrl,
        securityDescription);
  }

  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  private void useUserProvidedParameters() throws SuiteBroken {
    this.currentState.selectedIiaInfo.heiId = this.currentState.selectedHeiId;
    this.currentState.selectedIiaInfo.partnerHeiId = getParameterValue(PARTNER_HEI_ID_PARAMETER);

    if (isParameterProvided(RECEIVING_ACADEMIC_YEAR_ID)) {
      this.currentState.selectedIiaInfo.receivingAcademicYears
          .add(getParameterValue(RECEIVING_ACADEMIC_YEAR_ID));
    }
  }

  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected String getIiaIdParameter(
      HttpSecurityDescription securityDescription) throws SuiteBroken {
    HeiIdAndString foundIiaId = getCoveredIiaIds(
        Collections.singletonList(
            new HeiIdAndUrl(
                this.currentState.selectedHeiId,
                this.currentState.url,
                ApiEndpoint.Index
            )
        ),
        securityDescription
    );
    return foundIiaId.string;
  }

  protected IiaSuiteState.IiaInfo getIiaInfo(
      String heiId, String iiaId, String url,
      HttpSecurityDescription securityDescription
  ) throws SuiteBroken {
    IiaSuiteState.IiaInfo iiaInfo = new IiaSuiteState.IiaInfo();
    this.setup(new InlineValidationStep() {
      @Override
      public String getName() {
        return "Use 'get' endpoint to retrieve info about selected IIA.";
      }

      @Override
      @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
      protected Optional<Response> innerRun() throws Failure {
        Request request = makeApiRequestWithPreferredSecurity(
            this,
            url, ApiEndpoint.Get, securityDescription,
            new ParameterList(
                new Parameter("hei_id", heiId),
                new Parameter("iia_id", iiaId)
            ));

        if (request == null) {
          throw new Failure(
              "Couldn't find correct 'get' endpoint url in catalogue.",
              Status.NOTICE, null);
        }

        Response response;
        try {
          response = IiaIndexComplexSetupValidationSuiteV2.this.internet.makeRequest(request,
              IiaIndexComplexSetupValidationSuiteV2.this.timeoutMillis);
        } catch (SocketTimeoutException e) {
          throw new Failure("Request to 'get' endpoint timed out.",
              Status.ERROR, true);
        } catch (IOException ignored) {
          throw new Failure("Internal error: couldn't perform request to 'get' endpoint.",
              Status.ERROR, null);
        }
        expect200(response);

        IiasGetResponse getResponse;
        try {
          JAXBContext jaxbContext = JAXBContext.newInstance(IiasGetResponse.class);
          Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
          Element xml = makeXmlFromBytes(response.getBody(), true);
          getResponse = (IiasGetResponse) unmarshaller.unmarshal(xml);
        } catch (JAXBException e) {
          throw new Failure(
              "Received 200 OK but the response was empty or didn't contain correct "
                  + "get-response. Tests cannot be continued. "
                  + "Consult tests for 'get' endpoint.",
              Status.NOTICE, response);
        }

        if (getResponse.getIia().isEmpty()) {
          throw new Failure(
              "Received 200 OK but the response did not contain any IIA, but we requested one. "
                  + "Consult tests for 'get' endpoint.",
              Status.NOTICE, response);
        }

        IiasGetResponse.Iia iia = getResponse.getIia().get(0);

        // Schema ensures that there are at least two partners in every iia element.
        iiaInfo.heiId = iia.getPartner().get(0).getHeiId();

        if (!Objects.equals(iiaInfo.heiId,
            IiaIndexComplexSetupValidationSuiteV2.this.currentState.selectedHeiId)) {
          throw new Failure(
              "Received 200 OK but <hei-id> of first <partner> was different than we requested."
                  + "Consult tests for 'get' endpoint.",
              Status.NOTICE, response);
        }

        iiaInfo.partnerHeiId = iia.getPartner().get(1).getHeiId();
        ArrayList<MobilitySpecification> specs = new ArrayList<>();
        specs.addAll(iia.getCooperationConditions().getStudentStudiesMobilitySpec());
        specs.addAll(iia.getCooperationConditions().getStudentTraineeshipMobilitySpec());
        specs.addAll(iia.getCooperationConditions().getStaffTeacherMobilitySpec());
        specs.addAll(iia.getCooperationConditions().getStaffTrainingMobilitySpec());

        iiaInfo.receivingAcademicYears = specs.stream()
            .flatMap(ms -> ms.getReceivingAcademicYearId().stream()).distinct()
            .collect(Collectors.toList());

        return Optional.empty();
      }
    });

    return iiaInfo;
  }

  private HeiIdAndString getCoveredIiaIds(
      List<HeiIdAndUrl> heiIdAndUrls,
      HttpSecurityDescription securityDescription)
      throws SuiteBroken {
    return findResponseWithString(
        heiIdAndUrls,
        securityDescription,
        "/iias-index-response/iia-id",
        "Find iia-id to work with.",
        "We tried to find iia-id to perform tests on, but index endpoint doesn't report "
            + "any iia-id, cannot continue tests."
    );
  }
}
