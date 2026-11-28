package dev.aserver.motionrp;

import kr.toxicity.model.api.tracker.Tracker;
import java.util.function.BiConsumer;

/** The close-reason enum moved in BetterModel 3.8; the callback's erased ABI did not. */
final class TrackerCompatibility {
 private TrackerCompatibility(){}
 @SuppressWarnings({"unchecked","rawtypes"})
 static void onClose(Tracker tracker,BiConsumer<Tracker,Object> listener){
  // Keep both the callback and its lambda descriptor free of version-specific enum types.
  tracker.handleCloseEvent((BiConsumer)listener);
 }
}
