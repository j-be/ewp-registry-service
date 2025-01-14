package eu.erasmuswithoutpaper.registry.validators.mtinstitutionsvalidator;

import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.internet.Internet;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.ValidatorKeyStoreSet;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;
import org.springframework.stereotype.Service;

import com.google.common.collect.ListMultimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class MtInstitutionsValidator extends ApiValidator<MtInstitutionsSuiteState> {
  private static final Logger logger = LoggerFactory.getLogger(
      MtInstitutionsValidator.class);
  private static ListMultimap<SemanticVersion, ValidationSuiteInfo<MtInstitutionsSuiteState>>
      validationSuites;

  static {
    validationSuites = ApiValidator.createMultimap();
    validationSuites.put(
        new SemanticVersion(1, 0, 0),
        new ValidationSuiteInfo<>(
            MtInstitutionsSetupValidationSuiteV1::new,
            MtInstitutionsSetupValidationSuiteV1.getParameters()
        )
    );
    validationSuites.put(
        new SemanticVersion(1, 0, 0),
        new ValidationSuiteInfo<>(MtInstitutionsValidationSuiteV1::new)
    );
    validationSuites.put(
            new SemanticVersion(1, 0, 0),
            new ValidationSuiteInfo<>(
                    MtInstitutionsSetupValidationSuiteV1::new,
                    MtInstitutionsSetupValidationSuiteV1.getParameters()
            )
    );
    validationSuites.put(
            new SemanticVersion(1, 0, 0),
            new ValidationSuiteInfo<>(MtInstitutionsValidationSuiteV1::new)
    );
  }

  public MtInstitutionsValidator(EwpDocBuilder docBuilder, Internet internet,
      RegistryClient client,
      ValidatorKeyStoreSet validatorKeyStoreSet) {
    super(docBuilder, internet, client, validatorKeyStoreSet, "mt-institutions");
  }

  @Override
  public Logger getLogger() {
    return logger;
  }

  @Override
  protected ListMultimap<SemanticVersion, ValidationSuiteInfo<MtInstitutionsSuiteState>>
      getValidationSuites() {
    return validationSuites;
  }

  @Override
  protected MtInstitutionsSuiteState createState(String url, SemanticVersion version) {
    return new MtInstitutionsSuiteState(url, version);
  }
}
