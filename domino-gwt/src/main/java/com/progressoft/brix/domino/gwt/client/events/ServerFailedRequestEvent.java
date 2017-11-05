package com.progressoft.brix.domino.gwt.client.events;

import com.progressoft.brix.domino.api.client.ClientApp;
import com.progressoft.brix.domino.api.client.events.Event;
import com.progressoft.brix.domino.api.client.events.EventProcessor;
import com.progressoft.brix.domino.api.client.events.EventsBus;
import com.progressoft.brix.domino.api.client.request.ServerRequest;
import com.progressoft.brix.domino.api.client.request.Request;
import com.progressoft.brix.domino.api.shared.request.FailedResponseBean;

@SuppressWarnings({"unchecked", "rawtypes"})
public class ServerFailedRequestEvent extends ServerFailedRequestGwtEvent implements Event {

    protected final ServerRequest request;
    private final Throwable error;
    private final ClientApp clientApp = ClientApp.make();

    ServerFailedRequestEvent(ServerRequest request, Throwable error) {
        this.request = request;
        this.error = error;
    }

    @Override
    public void fire() {
        clientApp.getEventsBus().publishEvent(new GWTRequestEvent(this));
    }

    @Override
    public void process() {
        request.applyState(new Request.ServerResponseReceivedStateContext(makeFailedContext()));
    }

    private Request.ServerFailedRequestStateContext makeFailedContext() {
        return new Request.ServerFailedRequestStateContext(new FailedResponseBean(error));
    }

    @Override
    protected void dispatch(EventProcessor eventProcessor) {
        eventProcessor.process(this);
    }

    private class GWTRequestEvent implements EventsBus.RequestEvent<ServerFailedRequestGwtEvent> {

        private final ServerFailedRequestGwtEvent event;

        public GWTRequestEvent(ServerFailedRequestGwtEvent event) {
            this.event = event;
        }

        @Override
        public ServerFailedRequestGwtEvent asEvent() {
            return event;
        }
    }
}
