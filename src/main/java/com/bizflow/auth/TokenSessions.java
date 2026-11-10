package com.bizflow.auth;

import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenSessions {
    public static class Session { public final Long userId; public Long businessId; public final Instant created=Instant.now(); Session(Long u){userId=u;} }
    private final ConcurrentHashMap<String,Session> sessions=new ConcurrentHashMap<>();
    public String create(Long userId){String token=UUID.randomUUID()+"."+UUID.randomUUID();sessions.put(token,new Session(userId));return token;}
    public Session get(String token){Session s=sessions.get(token);if(s!=null&&s.created.plusSeconds(60L*60*24*14).isBefore(Instant.now())){sessions.remove(token);return null;}return s;}
    public void remove(String token){sessions.remove(token);}
}
