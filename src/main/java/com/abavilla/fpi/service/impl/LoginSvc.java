package com.abavilla.fpi.service.impl;

import com.abavilla.fpi.dto.impl.LoginDto;
import com.abavilla.fpi.dto.impl.SessionDto;
import com.abavilla.fpi.entity.impl.Session;
import com.abavilla.fpi.mapper.SessionMapper;
import com.abavilla.fpi.repo.impl.SessionRepo;
import com.abavilla.fpi.service.AbsSvc;
import io.smallrye.mutiny.Uni;
import org.keycloak.authorization.client.AuthzClient;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
@ApplicationScoped
public class LoginSvc extends AbsSvc<SessionDto, Session> {
  @Inject
  SessionRepo advRepo;
  @Inject
  AuthzClient authzClient;
  @Inject
  SessionMapper mapper;

  public Uni<SessionDto> login(LoginDto login) {
    Uni<Optional<Session>> byUsername = advRepo.findByUsername(login.getUsername());
    return byUsername.chain(session -> {
      if (session.isEmpty()) {
        var auth = authzClient.obtainAccessToken(login.getUsername(), login.getPassword());
        Session newSession = new Session();
        newSession.setUsername(login.getUsername());
        newSession.setAccessToken(auth.getToken());
        newSession.setRefreshToken(auth.getRefreshToken());
        newSession.setDateCreated(LocalDateTime.now(ZoneOffset.UTC));
        return repo.persist(newSession);
      }
      return Uni.createFrom().item(session.get());
    }).map(this::mapToDto);
  }

  public Uni<SessionDto> refreshToken(LoginDto login) {
    Uni<Optional<Session>> byUsername = advRepo.findByUsername(login.getUsername());
    return byUsername.chain(session -> {
      var auth = authzClient.obtainAccessToken(login.getUsername(), login.getPassword());
      Session newSession = session.orElse(new Session());
      newSession.setUsername(login.getUsername());
      newSession.setAccessToken(auth.getToken());
      newSession.setRefreshToken(auth.getRefreshToken());
      newSession.setDateCreated(LocalDateTime.now(ZoneOffset.UTC));
      return repo.persistOrUpdate(newSession);
    }).map(this::mapToDto);
  }

  @Override
  public SessionDto mapToDto(Session entity) {
    return mapper.mapToDto(entity);
  }

  @Override
  public Session mapToEntity(SessionDto dto) {
    return mapper.mapToEntity(dto);
  }
}
