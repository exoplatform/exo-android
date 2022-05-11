package org.exoplatform.service.push;

public class PushTokenSynchronizerLocator {

  private static final PushTokenSynchronizerLocator instance = new PushTokenSynchronizerLocator();

  public static PushTokenSynchronizer getInstance() {
    return instance.synchronizer;
  }

  private PushTokenSynchronizerLocator() {
    synchronizer = new PushTokenSynchronizer(new PushTokenRestServiceFactory());
  }

  private final PushTokenSynchronizer synchronizer;

}
