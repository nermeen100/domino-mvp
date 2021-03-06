package org.dominokit.domino.api.client.request;

import org.dominokit.domino.api.client.ServiceRootMatcher;
import org.dominokit.domino.api.shared.history.StateHistoryToken;
import org.dominokit.domino.api.shared.request.VoidRequest;
import org.gwtproject.regexp.shared.MatchResult;
import org.gwtproject.regexp.shared.RegExp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.HttpMethod;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static java.util.Objects.*;

public class ServerRequest<R, S>
        extends BaseRequest implements Response<S>, CanFailOrSend, HasHeadersAndParameters<R, S> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerRequest.class);
    private static final String CONTENT_TYPE = "Content-type";
    private static final String ACCEPT = "Accept";

    private SenderSupplier senderSupplier = new SenderSupplier(() -> new RequestSender<R, S>() {
    });

    private Map<String, String> headers = new HashMap<>();
    private Map<String, String> parameters = new HashMap<>();
    private Map<String, String> callArguments = new HashMap<>();

    private R requestBean;

    private String url;
    private String httpMethod = HttpMethod.GET;
    private String path = "";
    private String serviceRoot = "";
    private Integer[] successCodes = new Integer[]{200, 201, 202, 203, 204};
    private boolean voidResponse = false;

    private int timeout = -1;
    private int maxRetries = -1;

    private RequestWriter<R> requestWriter = request -> null;
    private ResponseReader<S> responseReader = request -> null;
    private RequestParametersReplacer<R> requestParametersReplacer = (token, request) -> token.value();

    private Success<S> success = response -> {
    };
    private Fail fail =
            failedResponse -> {
                if (nonNull(failedResponse.getThrowable())) {
                    LOGGER.debug("could not execute request on server: ", failedResponse.getThrowable());
                } else {
                    LOGGER.debug("could not execute request on server: status [" + failedResponse.getStatusCode() + "], body [" + failedResponse.getBody() + "]");
                }
            };

    private final RequestState<ServerSuccessRequestStateContext> executedOnServer = context -> {
        success.onSuccess((S) context.responseBean);
        state = completed;
    };

    private final RequestState<ServerFailedRequestStateContext> failedOnServer =
            context -> {
                fail.onFail(context.response);
                state = completed;
            };

    private final RequestState<ServerResponseReceivedStateContext> sent = context -> {
        if (context.nextContext instanceof ServerSuccessRequestStateContext) {
            state = executedOnServer;
            ServerRequest.this.applyState(context.nextContext);
        } else if (context.nextContext instanceof ServerFailedRequestStateContext) {
            state = failedOnServer;
            ServerRequest.this.applyState(context.nextContext);
        } else {
            throw new InvalidRequestState(
                    "Request cannot be processed until a responseBean is received from the server");
        }
    };

    protected ServerRequest() {
    }

    protected ServerRequest(R requestBean) {
        this.requestBean = requestBean;
    }

    @Override
    public final void send() {
        execute();
    }

    public ServerRequest<R, S> setBean(R requestBean) {
        this.requestBean = requestBean;
        return this;
    }

    public ServerRequest<R, S> intercept(Consumer<HasHeadersAndParameters<R, S>> interceptor) {
        interceptor.accept(this);
        return this;
    }

    @Override
    public void startRouting() {
        state = sent;
        clientApp.getServerRouter().routeRequest(this);
    }

    public RequestRestSender getSender() {
        return senderSupplier.get();
    }

    public R requestBean() {
        return this.requestBean;
    }

    public ServerRequest<R, S> setHeader(String name, String value) {
        headers.put(name, value);
        return this;
    }

    public ServerRequest<R, S> setHeaders(Map<String, String> headers) {
        if (nonNull(headers) && !headers.isEmpty()) {
            this.headers.putAll(headers);
        }
        return this;
    }

    public ServerRequest<R, S> setParameter(String name, String value) {
        parameters.put(name, value);
        return this;
    }

    public ServerRequest<R, S> setParameters(Map<String, String> parameters) {
        if (nonNull(parameters) && !parameters.isEmpty()) {
            this.parameters.putAll(parameters);
        }
        return this;
    }

    public Map<String, String> headers() {
        return new HashMap<>(headers);
    }

    public Map<String, String> parameters() {
        return new HashMap<>(parameters);
    }

    public void normalizeUrl() {
        if (isNull(this.url)) {
            String root = (isNull(this.serviceRoot) || this.serviceRoot.isEmpty()) ? ServiceRootMatcher.matchedServiceRoot(path) : ServiceRootMatcher.matchedServiceRoot(this.serviceRoot + path);
            this.setUrl(formatUrl(root + this.serviceRoot + path));
        }
    }

    protected String formatUrl(String targetUrl) {
        String postfix = asTokenString(targetUrl);
        String prefix = targetUrl.replace(postfix, "");

        StateHistoryToken tempToken = new StateHistoryToken(postfix);

        replaceUrlParamsWithArguments(tempToken);

        String formattedPostfix = requestParametersReplacer.replace(tempToken, requestBean);
        return prefix + formattedPostfix;
    }

    private void replaceUrlParamsWithArguments(StateHistoryToken tempToken) {
        Map<String, String> callArguments = this.getCallArguments();
        new ArrayList<>(tempToken.paths())
                .stream()
                .filter(path -> path.startsWith(":") && callArguments.containsKey(path.replace(":", "")))
                .forEach(path -> {
                    tempToken.replacePath(path, callArguments.get(path.replace(":", "")));
                });

        tempToken.queryParameters()
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue().startsWith(":") && callArguments.containsKey(entry.getValue().replace(":", "")))
                .forEach(entry -> {
                    tempToken.replaceParameter(entry.getKey(), entry.getKey(), callArguments.get(entry.getValue().replace(":", "")));
                });

        new ArrayList<>(tempToken.fragments())
                .stream()
                .filter(fragment -> fragment.startsWith(":") && callArguments.containsKey(fragment.replace(":", "")))
                .forEach(fragment -> {
                    tempToken.replaceFragment(fragment, callArguments.get(fragment.replace(":", "")));
                });
    }

    private String asTokenString(String url) {
        if (url.contains("http:") || url.contains("https:")) {
            RegExp regExp = RegExp.compile("^((.*:)//([a-z0-9\\-.]+)(|:[0-9]+)/)(.*)$");
            MatchResult matcher = regExp.exec(url);
            boolean matchFound = matcher != null; // equivalent to regExp.test(inputStr);

            if (matchFound) {
                String suffixPart = matcher.getGroup(matcher.getGroupCount() - 1);
                return suffixPart;
            }
        }

        return url;
    }

    public ServerRequest<R, S> setUrl(String url) {
        this.url = url;
        return this;
    }

    public ServerRequest<R, S> onBeforeSend(BeforeSendHandler handler) {
        handler.onBeforeSend();
        return this;
    }

    public ServerRequest<R, S> addCallArgument(String name, String value) {
        callArguments.put(name, value);
        return this;
    }

    public ServerRequest<R, S> removeCallArgument(String name) {
        callArguments.remove(name);
        return this;
    }

    public Map<String, String> getCallArguments() {
        return new HashMap<>(callArguments);
    }

    @Override
    public CanFailOrSend onSuccess(Success<S> success) {
        this.success = success;
        return this;
    }

    public ServerRequest<R, S> setContentType(String[] contentType) {
        setHeader(CONTENT_TYPE, String.join(", ", contentType));
        return this;
    }

    public ServerRequest<R, S> setAccept(String[] accept) {
        setHeader(ACCEPT, String.join(", ", accept));
        return this;
    }

    public ServerRequest<R, S> setHttpMethod(String httpMethod) {
        requireNonNull(httpMethod);
        this.httpMethod = httpMethod.toUpperCase();
        return this;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public Integer[] getSuccessCodes() {
        return successCodes;
    }

    public ServerRequest<R, S> setSuccessCodes(Integer[] successCodes) {
        this.successCodes = successCodes;
        return this;
    }

    public String getServiceRoot() {
        return serviceRoot;
    }

    public ServerRequest<R, S> setServiceRoot(String serviceRoot) {
        this.serviceRoot = serviceRoot;
        return this;
    }

    public RequestWriter<R> getRequestWriter() {
        return requestWriter;
    }

    public void setRequestWriter(RequestWriter<R> requestWriter) {
        this.requestWriter = requestWriter;
    }

    public ResponseReader<S> getResponseReader() {
        return responseReader;
    }

    public void setResponseReader(ResponseReader<S> responseReader) {
        this.responseReader = responseReader;
    }

    public String getPath() {
        return path;
    }

    public ServerRequest<R, S> setPath(String path) {
        this.path = path;
        return this;
    }

    @Override
    public CanSend onFailed(Fail fail) {
        this.fail = fail;
        return this;
    }

    public boolean isVoidRequest() {
        return requestBean instanceof VoidRequest;
    }

    public boolean isVoidResponse() {
        return voidResponse;
    }

    protected void markAsVoidResponse() {
        this.voidResponse = true;
    }

    public String getUrl() {
        return this.url;
    }

    public RequestParametersReplacer<R> getRequestParametersReplacer() {
        return requestParametersReplacer;
    }

    public ServerRequest<R, S> setRequestParametersReplacer(RequestParametersReplacer<R> requestParametersReplacer) {
        this.requestParametersReplacer = requestParametersReplacer;
        return this;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    @FunctionalInterface
    public interface BeforeSendHandler {
        void onBeforeSend();
    }
}
