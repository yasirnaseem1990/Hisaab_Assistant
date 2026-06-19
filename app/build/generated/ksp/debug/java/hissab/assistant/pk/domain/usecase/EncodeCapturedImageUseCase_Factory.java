package hissab.assistant.pk.domain.usecase;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import hissab.assistant.pk.domain.repository.ImageProcessor;
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
public final class EncodeCapturedImageUseCase_Factory implements Factory<EncodeCapturedImageUseCase> {
  private final Provider<ImageProcessor> imageProcessorProvider;

  public EncodeCapturedImageUseCase_Factory(Provider<ImageProcessor> imageProcessorProvider) {
    this.imageProcessorProvider = imageProcessorProvider;
  }

  @Override
  public EncodeCapturedImageUseCase get() {
    return newInstance(imageProcessorProvider.get());
  }

  public static EncodeCapturedImageUseCase_Factory create(
      Provider<ImageProcessor> imageProcessorProvider) {
    return new EncodeCapturedImageUseCase_Factory(imageProcessorProvider);
  }

  public static EncodeCapturedImageUseCase newInstance(ImageProcessor imageProcessor) {
    return new EncodeCapturedImageUseCase(imageProcessor);
  }
}
