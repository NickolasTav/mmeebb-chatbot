package br.edu.unipam.tcc.service;

import br.edu.unipam.tcc.dto.UazapiWebhookDto;

/**
 * Orchestrator service for chat flows and state machine transitions.
 * Coordinates conversation states, session management, and response dispatch.
 */
public interface ChatFlowOrchestrator {

    /**
     * Processes an incoming message event received from the WhatsApp queue.
     * Handles student resolution, state transitions, MMEEBB algorithm evaluations,
     * and WhatsApp responses.
     *
     * @param webhookDto Incoming webhook payload.
     */
    void processIncomingMessage(UazapiWebhookDto webhookDto);
}
