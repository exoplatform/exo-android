package org.exoplatform.service.push;

import org.exoplatform.model.TokenInfo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class PushTokenSynchronizerTest {

  @Mock
  PushTokenRestServiceFactory factory;
  @Mock
  PushTokenRestService restService;
  @Mock
  Call<ResponseBody> call;
  Response successResponse;

  PushTokenSynchronizer synchronizer;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    when(restService.registerToken(any(TokenInfo.class))).thenReturn(call);
    when(restService.deleteToken(anyString())).thenReturn(call);
    when(factory.create(anyString())).thenReturn(restService);
    successResponse = Response.success(mock(ResponseBody.class));
    synchronizer = new PushTokenSynchronizer(factory);
  }

  @Test
  public void setConnectedUserAndSync_shouldNotCallRestIfTokenIsNotSet() throws Exception {
    // given
    String user = "username";
    String url = "http://example.com";

    // when
    synchronizer.setConnectedUserAndSync(user, url);

    // then
    verify(factory, never()).create(anyString());
    verify(restService, never()).registerToken(any(TokenInfo.class));
  }

  @Test
  public void setConnectedUserAndSync_shouldCallRestIfTokenIsNotEmpty() throws Exception {
    // given
    String token = "example_token";
    String user = "username";
    String url = "http://example.com";

    // when
    synchronizer.setTokenAndSync(token);
    synchronizer.setConnectedUserAndSync(user, url);

    // then
    verify(factory, times(1)).create(eq(url));
    verify(restService, times(1)).registerToken(argThat(new TokenInfoMatcher(user, token)));
  }

  @Test
  public void setTokenAndSync_shouldNotCallRestIfUsernameIsNotSet() throws Exception {
    // given
    String token = "example_token";

    // when
    synchronizer.setTokenAndSync(token);

    // then
    verify(factory, never()).create(anyString());
    verify(restService, never()).registerToken(any(TokenInfo.class));
  }

  @Test
  public void setTokenAndSync_shouldCallRestIfUsernameIsNotEmpty() throws Exception {
    // given
    String token = "example_token";
    String user = "username";
    String url = "http://example.com";

    // when
    synchronizer.setConnectedUserAndSync(user, url);
    synchronizer.setTokenAndSync(token);

    // then
    verify(factory, times(1)).create(eq(url));
    verify(restService, times(1)).registerToken(argThat(new TokenInfoMatcher(user, token)));
  }

  @Test
  public void tryToDestroyToken_shouldNotCallRestIfIsNotBeenSynchronized() throws Exception {
    // given nothing
    // when
    synchronizer.tryToDestroyToken();

    // then
    verify(factory, never()).create(anyString());
    verify(restService, never()).registerToken(any(TokenInfo.class));
  }

  @Test
  public void tryToDestroyToken_shouldRestIfIsSynchronized() throws Exception {
    // given
    String token = "example_token";
    String user = "username";
    String url = "http://example.com";
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        Callback callback = invocation.getArgument(0);
        callback.onResponse(call, successResponse);
        return null;
      }
    }).when(call).enqueue(any(Callback.class));

    // when
    synchronizer.setConnectedUserAndSync(user, url);
    synchronizer.setTokenAndSync(token);
    synchronizer.tryToDestroyToken();

    // then
    verify(factory, times(1)).create(anyString());
    verify(restService, times(1)).deleteToken(eq(token));
  }

}