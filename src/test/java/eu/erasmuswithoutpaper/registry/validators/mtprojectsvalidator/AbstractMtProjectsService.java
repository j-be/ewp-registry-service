package eu.erasmuswithoutpaper.registry.validators.mtprojectsvalidator;

import java.io.IOException;
import java.util.List;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.validators.AbstractApiService;
import eu.erasmuswithoutpaper.registry.validators.types.MtProjectsResponse;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

public abstract class AbstractMtProjectsService extends AbstractApiService {
  protected final String myEndpoint;
  protected final RegistryClient registryClient;

  /**
   * @param url
   *     The endpoint at which to listen for requests.
   * @param registryClient
   *     Initialized and refreshed {@link RegistryClient} instance.
   */
  public AbstractMtProjectsService(String url, RegistryClient registryClient) {
    this.myEndpoint = url;
    this.registryClient = registryClient;
  }

  @Override
  public Response handleInternetRequest(Request request) throws IOException {
    try {
      if (request.getUrl().startsWith(this.myEndpoint)) {
        return handleMtProjectsRequest(request);
      }
      return null;
    } catch (ErrorResponseException e) {
      return e.response;
    }
  }

  protected Response createMtProjectsReponse(List<MtProjectsResponse.Project> data) {
    MtProjectsResponse response = new MtProjectsResponse();
    response.getProject().addAll(data);
    return marshallResponse(200, response);
  }

  protected abstract Response handleMtProjectsRequest(Request request)
      throws IOException, ErrorResponseException;
}