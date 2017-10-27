package com.progressoft.brix.domino.api.client.request;

import com.progressoft.brix.domino.api.shared.request.ResponseBean;

public interface ServerRequestCallBack {
    void onFailure(Throwable throwable);
    void onSuccess(ResponseBean response);
}
