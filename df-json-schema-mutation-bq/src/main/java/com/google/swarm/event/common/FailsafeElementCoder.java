/*
 * Copyright (C) 2019 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.swarm.event.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import org.apache.beam.sdk.coders.Coder;
import org.apache.beam.sdk.coders.CoderException;
import org.apache.beam.sdk.coders.CustomCoder;
import org.apache.beam.sdk.coders.NullableCoder;
import org.apache.beam.sdk.coders.StringUtf8Coder;
import org.apache.beam.sdk.values.TypeDescriptor;
import org.apache.beam.sdk.values.TypeParameter;

public class FailsafeElementCoder<OriginalT, CurrentT>
    extends CustomCoder<FailsafeElement<OriginalT, CurrentT>> {

  private static final long serialVersionUID = 7062001962102651585L;
  private static final NullableCoder<String> STRING_CODER = NullableCoder.of(StringUtf8Coder.of());
  private final Coder<OriginalT> originalPayloadCoder;
  private final Coder<CurrentT> currentPayloadCoder;

  private FailsafeElementCoder(
      Coder<OriginalT> originalPayloadCoder, Coder<CurrentT> currentPayloadCoder) {
    this.originalPayloadCoder = originalPayloadCoder;
    this.currentPayloadCoder = currentPayloadCoder;
  }

  public Coder<OriginalT> getOriginalPayloadCoder() {
    return originalPayloadCoder;
  }

  public Coder<CurrentT> getCurrentPayloadCoder() {
    return currentPayloadCoder;
  }

  public static <OriginalT, CurrentT> FailsafeElementCoder<OriginalT, CurrentT> of(
      Coder<OriginalT> originalPayloadCoder, Coder<CurrentT> currentPayloadCoder) {
    return new FailsafeElementCoder<>(originalPayloadCoder, currentPayloadCoder);
  }

  @Override
  public void encode(FailsafeElement<OriginalT, CurrentT> value, OutputStream outStream)
      throws IOException {
    if (value == null) {
      throw new CoderException("The FailsafeElementCoder cannot encode a null object!");
    }

    originalPayloadCoder.encode(value.getOriginalPayload(), outStream);
    currentPayloadCoder.encode(value.getPayload(), outStream);
    STRING_CODER.encode(value.getErrorMessage(), outStream);
    STRING_CODER.encode(value.getStacktrace(), outStream);
  }

  @Override
  public FailsafeElement<OriginalT, CurrentT> decode(InputStream inStream) throws IOException {

    OriginalT originalPayload = originalPayloadCoder.decode(inStream);
    CurrentT currentPayload = currentPayloadCoder.decode(inStream);
    String errorMessage = STRING_CODER.decode(inStream);
    String stacktrace = STRING_CODER.decode(inStream);

    return FailsafeElement.of(originalPayload, currentPayload)
        .setErrorMessage(errorMessage)
        .setStacktrace(stacktrace);
  }

  @Override
  public List<? extends Coder<?>> getCoderArguments() {
    return Arrays.asList(originalPayloadCoder, currentPayloadCoder);
  }

  @Override
  public TypeDescriptor<FailsafeElement<OriginalT, CurrentT>> getEncodedTypeDescriptor() {
    return new TypeDescriptor<FailsafeElement<OriginalT, CurrentT>>() {}.where(
            new TypeParameter<OriginalT>() {}, originalPayloadCoder.getEncodedTypeDescriptor())
        .where(new TypeParameter<CurrentT>() {}, currentPayloadCoder.getEncodedTypeDescriptor());
  }
}
