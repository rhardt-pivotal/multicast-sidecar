package io.hardt.k8s.util;

import java.util.List;
import java.util.Map;

public class PropagationSidecarPojo {
    private List<String> secretHeaders;
    private Map<String, String> headers;
    private Map<String, String> params;
    private int sequenceNumber;

    public PropagationSidecarPojo(List<String> secretHeaders, Map<String, String> headers, Map<String, String> params, int sequenceNumber) {
        this.secretHeaders = secretHeaders;
        this.headers = headers;
        this.params = params;
        this.sequenceNumber = sequenceNumber;
    }

    public List<String> getSecretHeaders() {
        return secretHeaders;
    }

    public void setSecretHeaders(List<String> secretHeaders) {
        this.secretHeaders = secretHeaders;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }
}
