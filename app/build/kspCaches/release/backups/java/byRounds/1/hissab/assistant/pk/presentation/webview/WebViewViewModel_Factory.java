package hissab.assistant.pk.presentation.webview;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import hissab.assistant.pk.domain.usecase.GetDefaultWebPageUseCase;
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
public final class WebViewViewModel_Factory implements Factory<WebViewViewModel> {
  private final Provider<GetDefaultWebPageUseCase> getDefaultWebPageUseCaseProvider;

  public WebViewViewModel_Factory(
      Provider<GetDefaultWebPageUseCase> getDefaultWebPageUseCaseProvider) {
    this.getDefaultWebPageUseCaseProvider = getDefaultWebPageUseCaseProvider;
  }

  @Override
  public WebViewViewModel get() {
    return newInstance(getDefaultWebPageUseCaseProvider.get());
  }

  public static WebViewViewModel_Factory create(
      Provider<GetDefaultWebPageUseCase> getDefaultWebPageUseCaseProvider) {
    return new WebViewViewModel_Factory(getDefaultWebPageUseCaseProvider);
  }

  public static WebViewViewModel newInstance(GetDefaultWebPageUseCase getDefaultWebPageUseCase) {
    return new WebViewViewModel(getDefaultWebPageUseCase);
  }
}
