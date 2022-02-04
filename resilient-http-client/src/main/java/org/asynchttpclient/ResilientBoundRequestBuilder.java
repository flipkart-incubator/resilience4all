/*
 * Copyright (c) 2022 [The original author]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.asynchttpclient;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.resolver.NameResolver;
import org.asynchttpclient.channel.ChannelPoolPartitioning;
import org.asynchttpclient.proxy.ProxyServer;
import org.asynchttpclient.proxy.ProxyServer.Builder;
import org.asynchttpclient.request.body.generator.BodyGenerator;
import org.asynchttpclient.request.body.multipart.Part;
import org.asynchttpclient.uri.Uri;
import org.reactivestreams.Publisher;

import java.io.File;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ResilientBoundRequestBuilder extends BoundRequestBuilder {

  private final BoundRequestBuilder innerBoundRequestBuilder;

  public ResilientBoundRequestBuilder(
      AsyncHttpClient client, BoundRequestBuilder innerBoundRequestBuilder) {
    // Whatever we pass in the super constructor, does not matter
    // Because we are finally gonna build the request using innerBoundRequestBuilder
    super(
        client,
        innerBoundRequestBuilder.method,
        client.getConfig().isDisableUrlEncodingForBoundRequests());
    this.innerBoundRequestBuilder = innerBoundRequestBuilder;
  }

  @Override
  public BoundRequestBuilder setUrl(String url) {
    innerBoundRequestBuilder.setUrl(url);
    return this;
  }

  @Override
  public BoundRequestBuilder setUri(Uri uri) {
    innerBoundRequestBuilder.setUri(uri);
    return this;
  }

  @Override
  public BoundRequestBuilder setAddress(InetAddress address) {
    innerBoundRequestBuilder.setAddress(address);
    return this;
  }

  @Override
  public BoundRequestBuilder setLocalAddress(InetAddress address) {
    innerBoundRequestBuilder.setLocalAddress(address);
    return this;
  }

  @Override
  public BoundRequestBuilder setVirtualHost(String virtualHost) {
    innerBoundRequestBuilder.setVirtualHost(virtualHost);
    return this;
  }

  @Override
  public BoundRequestBuilder clearHeaders() {
    innerBoundRequestBuilder.clearHeaders();
    return this;
  }

  @Override
  public BoundRequestBuilder setHeader(CharSequence name, String value) {
    innerBoundRequestBuilder.setHeader(name, value);
    return this;
  }

  @Override
  public BoundRequestBuilder setHeader(CharSequence name, Object value) {
    innerBoundRequestBuilder.setHeader(name, value);
    return this;
  }

  @Override
  public BoundRequestBuilder setHeader(CharSequence name, Iterable<?> values) {
    innerBoundRequestBuilder.setHeader(name, values);
    return this;
  }

  @Override
  public BoundRequestBuilder addHeader(CharSequence name, String value) {
    innerBoundRequestBuilder.addHeader(name, value);
    return this;
  }

  @Override
  public BoundRequestBuilder addHeader(CharSequence name, Object value) {
    innerBoundRequestBuilder.addHeader(name, value);
    return this;
  }

  @Override
  public BoundRequestBuilder addHeader(CharSequence name, Iterable<?> values) {
    innerBoundRequestBuilder.addHeader(name, values);
    return this;
  }

  @Override
  public BoundRequestBuilder setHeaders(HttpHeaders headers) {
    innerBoundRequestBuilder.setHeaders(headers);
    return this;
  }

  @Override
  public BoundRequestBuilder setHeaders(
      Map<? extends CharSequence, ? extends Iterable<?>> headers) {
    innerBoundRequestBuilder.setHeaders(headers);
    return this;
  }

  @Override
  public BoundRequestBuilder setSingleHeaders(Map<? extends CharSequence, ?> headers) {
    innerBoundRequestBuilder.setSingleHeaders(headers);
    return this;
  }

  @Override
  public BoundRequestBuilder setCookies(Collection<Cookie> cookies) {
    innerBoundRequestBuilder.setCookies(cookies);
    return this;
  }

  @Override
  public BoundRequestBuilder addCookie(Cookie cookie) {
    innerBoundRequestBuilder.addCookie(cookie);
    return this;
  }

  @Override
  public BoundRequestBuilder addOrReplaceCookie(Cookie cookie) {
    innerBoundRequestBuilder.addOrReplaceCookie(cookie);
    return this;
  }

  @Override
  public void resetCookies() {
    innerBoundRequestBuilder.resetCookies();
  }

  @Override
  public void resetQuery() {
    innerBoundRequestBuilder.resetQuery();
  }

  @Override
  public void resetFormParams() {
    innerBoundRequestBuilder.resetFormParams();
  }

  @Override
  public void resetNonMultipartData() {
    innerBoundRequestBuilder.resetNonMultipartData();
  }

  @Override
  public void resetMultipartData() {
    innerBoundRequestBuilder.resetMultipartData();
  }

  @Override
  public BoundRequestBuilder setBody(File file) {
    innerBoundRequestBuilder.setBody(file);
    return this;
  }

  @Override
  public BoundRequestBuilder setBody(byte[] data) {
    innerBoundRequestBuilder.setBody(data);
    return this;
  }

  @Override
  public BoundRequestBuilder setBody(List<byte[]> data) {
    innerBoundRequestBuilder.setBody(data);
    return this;
  }

  @Override
  public BoundRequestBuilder setBody(String data) {
    innerBoundRequestBuilder.setBody(data);
    return this;
  }

  @Override
  public BoundRequestBuilder setBody(ByteBuffer data) {
    innerBoundRequestBuilder.setBody(data);
    return this;
  }

  @Override
  public BoundRequestBuilder setBody(InputStream stream) {
    innerBoundRequestBuilder.setBody(stream);
    return this;
  }

  @Override
  public BoundRequestBuilder setBody(Publisher<ByteBuf> publisher) {
    innerBoundRequestBuilder.setBody(publisher);
    return this;
  }

  @Override
  public BoundRequestBuilder setBody(Publisher<ByteBuf> publisher, long contentLength) {
    innerBoundRequestBuilder.setBody(publisher, contentLength);
    return this;
  }

  @Override
  public BoundRequestBuilder setBody(BodyGenerator bodyGenerator) {
    innerBoundRequestBuilder.setBody(bodyGenerator);
    return this;
  }

  @Override
  public BoundRequestBuilder addQueryParam(String name, String value) {
    innerBoundRequestBuilder.addQueryParam(name, value);
    return this;
  }

  @Override
  public BoundRequestBuilder addQueryParams(List<Param> params) {
    innerBoundRequestBuilder.addQueryParams(params);
    return this;
  }

  @Override
  public BoundRequestBuilder setQueryParams(Map<String, List<String>> map) {
    innerBoundRequestBuilder.setQueryParams(map);
    return this;
  }

  @Override
  public BoundRequestBuilder setQueryParams(List<Param> params) {
    innerBoundRequestBuilder.setQueryParams(params);
    return this;
  }

  @Override
  public BoundRequestBuilder addFormParam(String name, String value) {
    innerBoundRequestBuilder.addFormParam(name, value);
    return this;
  }

  @Override
  public BoundRequestBuilder setFormParams(Map<String, List<String>> map) {
    innerBoundRequestBuilder.setFormParams(map);
    return this;
  }

  @Override
  public BoundRequestBuilder setFormParams(List<Param> params) {
    innerBoundRequestBuilder.setFormParams(params);
    return this;
  }

  @Override
  public BoundRequestBuilder addBodyPart(Part bodyPart) {
    innerBoundRequestBuilder.addBodyPart(bodyPart);
    return this;
  }

  @Override
  public BoundRequestBuilder setBodyParts(List<Part> bodyParts) {
    innerBoundRequestBuilder.setBodyParts(bodyParts);
    return this;
  }

  @Override
  public BoundRequestBuilder setProxyServer(ProxyServer proxyServer) {
    innerBoundRequestBuilder.setProxyServer(proxyServer);
    return this;
  }

  @Override
  public BoundRequestBuilder setProxyServer(Builder proxyServerBuilder) {
    innerBoundRequestBuilder.setProxyServer(proxyServerBuilder);
    return this;
  }

  @Override
  public BoundRequestBuilder setRealm(Realm.Builder realm) {
    innerBoundRequestBuilder.setRealm(realm);
    return this;
  }

  @Override
  public BoundRequestBuilder setRealm(Realm realm) {
    innerBoundRequestBuilder.setRealm(realm);
    return this;
  }

  @Override
  public BoundRequestBuilder setFollowRedirect(boolean followRedirect) {
    innerBoundRequestBuilder.setFollowRedirect(followRedirect);
    return this;
  }

  @Override
  public BoundRequestBuilder setRequestTimeout(int requestTimeout) {
    innerBoundRequestBuilder.setRequestTimeout(requestTimeout);
    return this;
  }

  @Override
  public BoundRequestBuilder setReadTimeout(int readTimeout) {
    innerBoundRequestBuilder.setReadTimeout(readTimeout);
    return this;
  }

  @Override
  public BoundRequestBuilder setRangeOffset(long rangeOffset) {
    innerBoundRequestBuilder.setRangeOffset(rangeOffset);
    return this;
  }

  @Override
  public BoundRequestBuilder setMethod(String method) {
    innerBoundRequestBuilder.setMethod(method);
    return this;
  }

  @Override
  public BoundRequestBuilder setCharset(Charset charset) {
    innerBoundRequestBuilder.setCharset(charset);
    return this;
  }

  @Override
  public BoundRequestBuilder setChannelPoolPartitioning(
      ChannelPoolPartitioning channelPoolPartitioning) {
    innerBoundRequestBuilder.setChannelPoolPartitioning(channelPoolPartitioning);
    return this;
  }

  @Override
  public BoundRequestBuilder setNameResolver(NameResolver<InetAddress> nameResolver) {
    innerBoundRequestBuilder.setNameResolver(nameResolver);
    return this;
  }

  @Override
  public BoundRequestBuilder setSignatureCalculator(SignatureCalculator signatureCalculator) {
    innerBoundRequestBuilder.setSignatureCalculator(signatureCalculator);
    return this;
  }

  @Override
  public Request build() {
    // Because we have overwritten build, we do not need to override execute methods.
    return innerBoundRequestBuilder.build();
  }
}
