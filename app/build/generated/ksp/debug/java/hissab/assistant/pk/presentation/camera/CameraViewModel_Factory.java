package hissab.assistant.pk.presentation.camera;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class CameraViewModel_Factory implements Factory<CameraViewModel> {
  @Override
  public CameraViewModel get() {
    return newInstance();
  }

  public static CameraViewModel_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static CameraViewModel newInstance() {
    return new CameraViewModel();
  }

  private static final class InstanceHolder {
    private static final CameraViewModel_Factory INSTANCE = new CameraViewModel_Factory();
  }
}
