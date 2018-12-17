package org.dominokit.domino.gwt.client.options;

import com.google.gwt.core.client.GWT;
import org.dominokit.domino.api.client.CanSetDominoOptions;
import org.dominokit.domino.api.client.DominoOptions;
import org.dominokit.domino.api.client.DynamicServiceRoot;
import org.dominokit.domino.api.client.request.RequestInterceptor;

import java.util.ArrayList;
import java.util.List;

public class DefaultDominoOptions implements DominoOptions {
    private String defaultServiceRoot = GWT.getModuleBaseURL().replace("static", "service");
    private String defaultJsonDateFormat = null;
    private List<DynamicServiceRoot> dynamicServiceRoots = new ArrayList<>();
    private RequestInterceptor requestInterceptor = (request, callBack) -> callBack.onComplete();

    @Override
    public void applyOptions() {
        //not implemented yet
    }

    @Override
    public CanSetDominoOptions setDefaultServiceRoot(String defaultServiceRoot) {
        this.defaultServiceRoot = defaultServiceRoot;
        return this;
    }

    @Override
    public CanSetDominoOptions setDefaultJsonDateFormat(String defaultJsonDateFormat) {
        this.defaultJsonDateFormat = defaultJsonDateFormat;
        return this;
    }

    @Override
    public CanSetDominoOptions addDynamicServiceRoot(DynamicServiceRoot dynamicServiceRoot) {
        dynamicServiceRoots.add(dynamicServiceRoot);
        return this;
    }

    @Override
    public CanSetDominoOptions setRequestInterceptor(RequestInterceptor interceptor) {
        this.requestInterceptor = interceptor;
        return this;
    }

    @Override
    public RequestInterceptor getRequestInterceptor() {
        return requestInterceptor;
    }

    @Override
    public String getDefaultServiceRoot() {
        return defaultServiceRoot;
    }

    @Override
    public String getDefaultJsonDateFormat() {
        return defaultJsonDateFormat;
    }

    @Override
    public List<DynamicServiceRoot> getServiceRoots() {
        return dynamicServiceRoots;
    }
}