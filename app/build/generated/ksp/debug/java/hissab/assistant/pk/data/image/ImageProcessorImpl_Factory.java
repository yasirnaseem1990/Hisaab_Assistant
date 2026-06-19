package hissab.assistant.pk.data.image;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import kotlinx.coroutines.CoroutineDispatcher;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("hissab.assistant.pk.di.IoDispatcher")
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
public final class ImageProcessorImpl_Factory implements Factory<ImageProcessorImpl> {
  private final Provider<CoroutineDispatcher> ioDispatcherProvider;

  public ImageProcessorImpl_Factory(Provider<CoroutineDispatcher> ioDispatcherProvider) {
    this.ioDispatcherProvider = ioDispatcherProvider;
  }

  @Override
  public ImageProcessorImpl get() {
    return newInstance(ioDispatcherProvider.get());
  }

  public static ImageProcessorImpl_Factory create(
      Provider<CoroutineDispatcher> ioDispatcherProvider) {
    return new ImageProcessorImpl_Factory(ioDispatcherProvider);
  }

  public static ImageProcessorImpl newInstance(CoroutineDispatcher ioDispatcher) {
    return new ImageProcessorImpl(ioDispatcher);
  }
}
