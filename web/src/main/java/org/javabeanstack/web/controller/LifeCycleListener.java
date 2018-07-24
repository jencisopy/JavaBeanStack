package org.javabeanstack.web.controller;

import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import org.apache.log4j.Logger;

public class LifeCycleListener implements PhaseListener {

    private static final Logger LOGGER = Logger.getLogger(LifeCycleListener.class);

    @Override
    public PhaseId getPhaseId() {
        return PhaseId.ANY_PHASE;
    }

    @Override
    public void beforePhase(PhaseEvent event) {
        LOGGER.info("BeforePhase: " + event.getPhaseId().getName());
    }

    @Override
    public void afterPhase(PhaseEvent event) {
        LOGGER.info("AfterPhase: " + event.getPhaseId().getName());
    }
}
