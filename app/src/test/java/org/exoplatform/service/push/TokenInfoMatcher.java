package org.exoplatform.service.push;

import org.exoplatform.model.TokenInfo;
import org.mockito.ArgumentMatcher;

public class TokenInfoMatcher implements ArgumentMatcher<TokenInfo> {

  private final String username;
  private final String token;

  public TokenInfoMatcher(String username, String token) {
    this.username = username;
    this.token = token;
  }

  @Override
  public boolean matches(TokenInfo argument) {
    return "android".equals(argument.getDevice()) && username.equals(argument.getUsername()) && token.equals(argument.getToken());
  }
}
