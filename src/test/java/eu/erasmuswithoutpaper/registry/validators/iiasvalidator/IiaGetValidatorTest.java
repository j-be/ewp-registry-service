package eu.erasmuswithoutpaper.registry.validators.iiasvalidator;


import static eu.erasmuswithoutpaper.registry.validators.TestValidationReportAsset.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.TestValidationReport;
import eu.erasmuswithoutpaper.registry.validators.iiavalidator.IiaSuiteState;
import eu.erasmuswithoutpaper.registry.validators.iiavalidator.get.IiaGetValidator;
import eu.erasmuswithoutpaper.registry.validators.types.IiasGetResponse;
import org.springframework.beans.factory.annotation.Autowired;

import org.junit.Test;

public class IiaGetValidatorTest extends IiaValidatorTestBase {
  @Autowired
  protected IiaGetValidator validator;

  @Override
  protected ApiValidator<IiaSuiteState> GetValidator() {
    return validator;
  }

  @Override
  protected String getUrl() {
    return iiaGetUrl;
  }

  @Test
  public void testValidationOnValidServiceIsSuccessful() {
    IiasServiceV2Valid service = new IiasServiceV2Valid(iiaIndexUrl, iiaGetUrl, this.client);
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).isCorrect();
  }

  @Test
  public void testNotValidatingIiaIdListIsDetected() {
    IiasServiceV2Valid service = new IiasServiceV2Valid(iiaIndexUrl, iiaGetUrl, this.client) {
      @Override
      protected void ErrorMaxIdsExceeded(RequestData requestData)
          throws ErrorResponseException {
        //Do nothing
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request more than <max-iia-ids> known iia-ids, expect 400.");
  }

  @Test
  public void testNotValidatingIiaCodeListIsDetected() {
    IiasServiceV2Valid service = new IiasServiceV2Valid(iiaIndexUrl, iiaGetUrl, this.client) {
      @Override
      protected void ErrorMaxCodesExceeded(RequestData requestData)
          throws ErrorResponseException {
        //Do nothing
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request more than <max-iia-codes> known iia-codes, expect 400.");
  }

  @Test
  public void testNotReportingMissingRequiredParametersAsAnErrorIsDetected() {
    IiasServiceV2Valid service = new IiasServiceV2Valid(iiaIndexUrl, iiaGetUrl, this.client) {
      @Override
      protected void errorNoParams(RequestData requestData)
          throws ErrorResponseException {
        throw new ErrorResponseException(createIiasGetResponse(new ArrayList<>()));
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request without hei-ids and iia-ids, expect 400.");
  }

  @Test
  public void testReturningErrorWhenInvalidParameterIsPassedIsDetected() {
    IiasServiceV2Valid service = new IiasServiceV2Valid(iiaIndexUrl, iiaGetUrl, this.client) {
      @Override
      protected void handleUnexpectedParams(
          RequestData requestData) throws ErrorResponseException {
        throw new ErrorResponseException(
            createErrorResponse(requestData.request, 400, "Unknown parameter")
        );
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request with additional parameter, expect 200 and one iia in response.");
  }

  @Test
  public void testNotReportingMissingHeiIdParameterAsAnErrorIsDetected() {
    IiasServiceV2Valid service = new IiasServiceV2Valid(iiaIndexUrl, iiaGetUrl, this.client) {
      @Override
      protected void errorNoHeiId(RequestData requestData) throws ErrorResponseException {
        throw new ErrorResponseException(
            createIiasGetResponse(new ArrayList<>())
        );
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request without hei-ids, expect 400.");
  }

  @Test
  public void testNotReportingMissingIiaIdAndIiaCodeAsAnErrorIsDetected() {
    IiasServiceV2Valid service = new IiasServiceV2Valid(iiaIndexUrl, iiaGetUrl, this.client) {
      @Override
      protected void errorNoIdsNorCodes(RequestData requestData) throws ErrorResponseException {
        throw new ErrorResponseException(
            createIiasGetResponse(new ArrayList<>())
        );
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request without iia-ids and iia-codes, expect 400.");
  }

  @Test
  public void testIgnoringAdditionalHeiIdsIsDetected() {
    IiasServiceV2Valid service = new IiasServiceV2Valid(iiaIndexUrl, iiaGetUrl, this.client) {
      @Override
      protected void errorMultipleHeiIds(RequestData requestData) throws ErrorResponseException {
        //Ignore
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request with correct hei_id twice, expect 400.");
  }

  @Test
  public void testHandlingBothIdsAndCodesIsDetected() {
    IiasServiceV2Valid service = new IiasServiceV2Valid(iiaIndexUrl, iiaGetUrl, this.client) {
      @Override
      protected void errorIdsAndCodes(RequestData requestData) throws ErrorResponseException {
        //Ignore
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request with correct iia_id and correct iia_code, expect 400.");
  }

  @Test
  public void testIgnoringCodesWhenIdsAndCodesArePassedIsDetected() {
    IiasServiceV2Valid service = new IiasServiceV2Valid(iiaIndexUrl, iiaGetUrl, this.client) {
      @Override
      protected void errorIdsAndCodes(RequestData requestData) throws ErrorResponseException {
        requestData.iiaCodes.clear();
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request with correct iia_id and correct iia_code, expect 400.");
  }

  @Test
  public void testIgnoringIdsWhenIdsAndCodesArePassedIsDetected() {
    IiasServiceV2Valid service = new IiasServiceV2Valid(iiaIndexUrl, iiaGetUrl, this.client) {
      @Override
      protected void errorIdsAndCodes(RequestData requestData) throws ErrorResponseException {
        requestData.iiaIds.clear();
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request with correct iia_id and correct iia_code, expect 400.");
  }

  @Test
  public void testReturningCorrectDataWhenNoHeiIdIsPassedIsDetected() {
    IiasServiceV2Valid service = new IiasServiceV2Valid(iiaIndexUrl, iiaGetUrl, this.client) {
      @Override
      protected void errorNoHeiId(RequestData requestData) throws ErrorResponseException {
        requestData.heiId = this.coveredHeiIds.get(0);
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request without hei-ids, expect 400.");
  }

  @Test
  public void testReturningCorrectDataWhenNeitherIiaIdNorIiaCodeIsPassedIsDetected() {
    IiasServiceV2Valid service = new IiasServiceV2Valid(iiaIndexUrl, iiaGetUrl, this.client) {
      @Override
      protected void errorNoIdsNorCodes(RequestData requestData) throws ErrorResponseException {
        String id = this.iias.get(0).getPartner().get(0).getIiaId();
        requestData.iiaIds = Arrays.asList(id);
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request without iia-ids and iia-codes, expect 400.");
  }

  @Test
  public void testNotReportingErrorWhenUnknownHeiIdIsPassedIsDetected() {
    IiasServiceV2Valid service = new IiasServiceV2Valid(iiaIndexUrl, iiaGetUrl, this.client) {
      @Override
      protected void errorUnknownHeiId(RequestData requestData) throws ErrorResponseException {
        throw new ErrorResponseException(
            createIiasGetResponse(new ArrayList<>())
        );
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request for one of known iia-ids with unknown hei-id, expect 400.");
  }

  @Test
  public void testNonEmptyResponseWhenUnknownHeiIdIsPassedIsDetected() {
    IiasServiceV2Valid service = new IiasServiceV2Valid(iiaIndexUrl, iiaGetUrl, this.client) {
      @Override
      protected void errorUnknownHeiId(RequestData requestData) throws ErrorResponseException {
        requestData.heiId = this.coveredHeiIds.get(0);
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request for one of known iia-ids with unknown hei-id, expect 400.");
  }

  @Test
  public void testReturningWrongIiaIsDetected() {
    IiasServiceV2Valid service = new IiasServiceV2Valid(iiaIndexUrl, iiaGetUrl, this.client) {
      @Override
      protected String handleKnownIiaId(String iiaId) {
        return iias.stream().filter(
            i -> !i.getPartner().get(0).getIiaId().equals(iiaId)).findFirst().get().getPartner()
            .get(0).getIiaId();
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request for one of known iia-ids, expect 200 OK.");
  }

  @Test
  public void testReturningDataForUnknownIiaIdIsDetected() {
    IiasServiceV2Valid service = new IiasServiceV2Valid(iiaIndexUrl, iiaGetUrl, this.client) {
      @Override
      protected String handleUnknownIiaId(String iiaId,
          List<IiasGetResponse.Iia> selectedIias) {
        return selectedIias.get(0).getPartner().get(0).getIiaId();
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request one unknown iia-id, expect 200 and empty response.");
    assertThat(report).containsFailure(
        "Request one known and one unknown iia-id, expect 200 and only one iia in response.");
  }

  @Test
  public void testReturningDataForUnknownIiaCodeIsDetected() {
    IiasServiceV2Valid service = new IiasServiceV2Valid(iiaIndexUrl, iiaGetUrl, this.client) {
      @Override
      protected String handleUnknownIiaCode(String iiaCode,
          List<IiasGetResponse.Iia> selectedIias) {
        return selectedIias.get(0).getPartner().get(0).getIiaCode();
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request one known and one unknown iia-code, expect 200 and only one iia in response.");
  }

  @Test
  public void testTooLargeMaxIiaIdsInManifestIsDetected() {
    IiasServiceV2Valid service = new IiasServiceV2Valid(iiaIndexUrl, iiaGetUrl, this.client) {
      @Override
      protected int getMaxIiaIds() {
        return super.getMaxIiaIds() - 1;
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request exactly <max-iia-ids> known iia-ids, "
            + "expect 200 and <max-iia-ids> iia-ids in response.");
  }

  @Test
  public void testTooLargeMaxIiaCodesInManifestIsDetected() {
    IiasServiceV2Valid service = new IiasServiceV2Valid(iiaIndexUrl, iiaGetUrl, this.client) {
      @Override
      protected int getMaxIiaCodes() {
        return super.getMaxIiaCodes() - 1;
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request exactly <max-iia-codes> known iia-codes, "
            + "expect 200 and <max-iia-codes> iia-codes in response.");
  }
}