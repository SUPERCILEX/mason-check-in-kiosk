/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
/*
 * This code was generated by https://github.com/google/apis-client-generator/
 * Modify at your own risk.
 */

package com.google.tokenservice;

/**
 * TokenService request.
 *
 * @since 1.3
 */
@SuppressWarnings("javadoc")
public abstract class TokenServiceRequest<T> extends
        com.google.api.client.googleapis.services.json.AbstractGoogleJsonClientRequest<T> {

    /**
     * @param client Google client
     * @param method HTTP Method
     * @param uriTemplate URI template for the path relative to the base URL. If it starts with a
     * "/" the base path from the base URL will be stripped out. The URI template can also be a full
     * URL. URI template expansion is done using {@link com.google.api.client.http.UriTemplate#expand(String,
     * String, Object, boolean)}
     * @param content A POJO that can be serialized into JSON or {@code null} for none
     * @param responseClass response class to parse into
     */
    public TokenServiceRequest(
            TokenService client, String method, String uriTemplate, Object content,
            Class<T> responseClass) {
        super(
                client,
                method,
                uriTemplate,
                content,
                responseClass);
    }

    /**
     * Data format for the response.
     */
    @com.google.api.client.util.Key
    private java.lang.String alt;

    /**
     * Data format for the response. [default: json]
     */
    public java.lang.String getAlt() {
        return alt;
    }

    /**
     * Data format for the response.
     */
    public TokenServiceRequest<T> setAlt(java.lang.String alt) {
        this.alt = alt;
        return this;
    }

    /**
     * API key. Your API key identifies your project and provides you with API access, quota, and
     * reports. Required unless you provide an OAuth 2.0 token.
     */
    @com.google.api.client.util.Key
    private java.lang.String key;

    /**
     * API key. Your API key identifies your project and provides you with API access, quota, and
     * reports. Required unless you provide an OAuth 2.0 token.
     */
    public java.lang.String getKey() {
        return key;
    }

    /**
     * API key. Your API key identifies your project and provides you with API access, quota, and
     * reports. Required unless you provide an OAuth 2.0 token.
     */
    public TokenServiceRequest<T> setKey(java.lang.String key) {
        this.key = key;
        return this;
    }

    @Override
    public final TokenService getAbstractGoogleClient() {
        return (TokenService) super.getAbstractGoogleClient();
    }

    @Override
    public TokenServiceRequest<T> setDisableGZipContent(boolean disableGZipContent) {
        return (TokenServiceRequest<T>) super.setDisableGZipContent(disableGZipContent);
    }

    @Override
    public TokenServiceRequest<T> setRequestHeaders(
            com.google.api.client.http.HttpHeaders headers) {
        return (TokenServiceRequest<T>) super.setRequestHeaders(headers);
    }

    @Override
    public TokenServiceRequest<T> set(String parameterName, Object value) {
        return (TokenServiceRequest<T>) super.set(parameterName, value);
    }
}
