package eu.erasmuswithoutpaper.registry.validators.iiavalidator.v6.index;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;

class IiaIndexValidatedApiInfoV6 implements ValidatedApiInfo {
  @Override
  public KnownElement getResponseKnownElement() {
    return KnownElement.RESPONSE_IIAS_INDEX_V6;
  }

  @Override
  public KnownNamespace getApiEntryKnownNamespace() {
    return KnownNamespace.APIENTRY_IIAS_V6;
  }

  @Override
  public String getApiName() {
    return "iias";
  }

  @Override
  public ApiEndpoint getEndpoint() {
    return ApiEndpoint.Index;
  }
}
