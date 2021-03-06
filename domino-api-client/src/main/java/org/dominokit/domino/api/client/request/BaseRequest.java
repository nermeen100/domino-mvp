package org.dominokit.domino.api.client.request;

import org.dominokit.domino.api.client.ClientApp;

public abstract class BaseRequest implements Request {

    public static final String REQUEST_HAVE_ALREADY_BEEN_SENT = "Request have already been sent";

    protected RequestState state;
    protected final ClientApp clientApp = ClientApp.make();

    protected final RequestState<DefaultRequestStateContext> ready = context -> startRouting();

    protected final RequestState<DefaultRequestStateContext> completed = context -> {
        throw new InvalidRequestState("This request have already been completed!.");
    };

    public BaseRequest() {
        this.state = ready;
    }

    protected void execute() {
        if (!state.equals(ready))
            throw new InvalidRequestState(REQUEST_HAVE_ALREADY_BEEN_SENT);
        this.state.execute(new DefaultRequestStateContext());
    }


    @Override
    public void applyState(RequestStateContext context) {
        state.execute(context);
    }

}
