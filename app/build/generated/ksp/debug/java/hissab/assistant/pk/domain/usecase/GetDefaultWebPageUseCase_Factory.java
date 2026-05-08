package hissab.assistant.pk.domain.usecase;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import hissab.assistant.pk.domain.repository.WebViewRepository;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class GetDefaultWebPageUseCase_Factory implements Factory<GetDefaultWebPageUseCase> {
  private final Provider<WebViewRepository> repositoryProvider;

  public GetDefaultWebPageUseCase_Factory(Provider<WebViewRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public GetDefaultWebPageUseCase get() {
    return newInstance(repositoryProvider.get());
  }

  public static GetDefaultWebPageUseCase_Factory create(
      Provider<WebViewRepository> repositoryProvider) {
    return new GetDefaultWebPageUseCase_Factory(repositoryProvider);
  }

  public static GetDefaultWebPageUseCase newInstance(WebViewRepository repository) {
    return new GetDefaultWebPageUseCase(repository);
  }
}
