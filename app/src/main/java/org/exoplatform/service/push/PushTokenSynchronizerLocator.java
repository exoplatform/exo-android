package org.exoplatform.service.push;

public class PushTokenSynchronizerLocator {

  private static PushTokenSynchronizerLocator instance = new PushTokenSynchronizerLocator();

  public static PushTokenSynchronizer getInstance() {
    return instance.synchronizer;
  }

  private PushTokenSynchronizerLocator() {
    synchronizer = new PushTokenSynchronizer(new PushTokenRestServiceFactory());
  }

  private PushTokenSynchronizer synchronizer;

}
