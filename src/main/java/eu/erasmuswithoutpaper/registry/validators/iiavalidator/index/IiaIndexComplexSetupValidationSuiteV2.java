package eu.erasmuswithoutpaper.registry.validators.iiavalidator.index;

import java.io.IOException;
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
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.HttpSecurityDescription;
import eu.erasmuswithoutpaper.registry.validators.InlineValidationStep;
import eu.erasmuswithoutpaper.registry.validators.ValidationParameter;
import eu.erasmuswithoutpaper.registry.validators.iiavalidator.IiaSuiteState;
import eu.erasmuswithoutpaper.registry.validators.types.IiasGetResponse;
import eu.erasmuswithoutpaper.registry.validators.types.MobilitySpecification;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

public class IiaIndexComplexSetupValidationSuiteV2 extends IiaIndexBasicSetupValidationSuiteV2 {

  private static final Logger logger =
      LoggerFactory.getLogger(IiaIndexComplexSetupValidationSuiteV2.class);

  @Override
  protected Logger getLogger() {
    return logger;
  }

  private static final String IIA_ID_PARAMETER = "iia_id";

  /**
   * Get list of parameters supported by this validator.
   *
   * @return list of supported parameters with dependencies.
   */
  public static List<ValidationParameter> getParameters() {
    return Collections.singletonList(
        new ValidationParameter(IIA_ID_PARAMETER, Collections.singletonList(HEI_ID_PARAMETER))
    );
  }

  IiaIndexComplexSetupValidationSuiteV2(
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

  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is IiaSuiteState not just SuiteState
  @Override
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void runApiSpecificTests(
      HttpSecurityDescription securityDescription) throws SuiteBroken {
    if (this.currentState.parameters.contains(IIA_ID_PARAMETER)) {
      this.currentState.selectedIiaId = this.currentState.parameters.get(IIA_ID_PARAMETER);
    } else {
      HeiIdAndString foundIiaId = getCoveredIiaIds(
          Collections.singletonList(
              new HeiIdAndUrl(
                  this.currentState.selectedHeiId,
                  this.currentState.url
              )
          ),
          securityDescription
      );
      this.currentState.selectedIiaId = foundIiaId.string;
    }
    String getUrl = getApiUrlForHei(
        this.currentState.selectedHeiId, this.getApiInfo().getApiName(), "get",
        "Retrieving 'get' endpoint url from catalogue.",
        "Couldn't find 'get' endpoint url in the catalogue. Is manifest correct?");

    // call get to get required info about this iia
    this.currentState.selectedIiaInfo = getIiaInfo(
        this.currentState.selectedHeiId,
        this.currentState.selectedIiaId,
        getUrl,
        securityDescription);
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
            url, "get", securityDescription,
            Arrays.asList(
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
          response = IiaIndexComplexSetupValidationSuiteV2.this.internet.makeRequest(request);
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