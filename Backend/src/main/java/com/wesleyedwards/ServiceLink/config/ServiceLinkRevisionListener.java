package com.wesleyedwards.ServiceLink.config;

import org.hibernate.envers.RevisionListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Fires once per audited transaction. Reads the authenticated principal from the
 * SecurityContext (still on the request thread here) and stamps the revision with
 * the acting user. Non-web / anonymous writes (startup, tests) leave the actor null.
 */
public class ServiceLinkRevisionListener implements RevisionListener {

    @Override
    public void newRevision(Object revisionEntity) {
        ServiceLinkRevision rev = (ServiceLinkRevision) revisionEntity;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            rev.setActorName(principal.getUsername());
            rev.setActorId(principal.getUserId());
        }
    }
}
