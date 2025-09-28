package bootanimation.zip.bootzipcreator.callbacks;

import java.util.List;
import bootanimation.zip.bootzipcreator.models.BootAnimation;

public interface BootAnimationCallback {
    void onComplete(List<BootAnimation> animations);
    void onError(Exception e);
}