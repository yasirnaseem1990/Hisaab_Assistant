package hissab.assistant.pk.data.repository;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
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
public final class WebViewRepositoryImpl_Factory implements Factory<WebViewRepositoryImpl> {
  @Override
  public WebViewRepositoryImpl get() {
    return newInstance();
  }

  public static WebViewRepositoryImpl_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static WebViewRepositoryImpl newInstance() {
    return new WebViewRepositoryImpl();
  }

  private static final class InstanceHolder {
    private static final WebViewRepositoryImpl_Factory INSTANCE = new WebViewRepositoryImpl_Factory();
  }
}
