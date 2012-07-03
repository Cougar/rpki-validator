/**
 * The BSD License
 *
 * Copyright (c) 2010-2012 RIPE NCC
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *   - Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *   - Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *   - Neither the name of the RIPE NCC nor the names of its contributors may be
 *     used to endorse or promote products derived from this software without
 *     specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package net.ripe.rpki.validator.fetchers

import org.scalatest.{BeforeAndAfter, FunSuite}
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import net.ripe.certification.validator.fetchers.RsyncCertificateRepositoryObjectFetcher
import java.net.URI
import net.ripe.commons.certification.validation.ValidationResult
import org.mockito.Mockito.verify

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class RemoteObjectFetcherTest extends FunSuite with ShouldMatchers with BeforeAndAfter with MockitoSugar {

  val mockRsyncFetcher = mock[RsyncCertificateRepositoryObjectFetcher]
  val mockHttpFetcher = mock[HttpObjectFetcher]

  val subject = new RemoteObjectFetcher(mockRsyncFetcher, mockHttpFetcher) {
    override val uriMap = Map(URI.create("rsync://rpki.ripe.net/") -> URI.create("http://foo.ripe.net/bar/"))
  }

  val nonMappableUri: URI = URI.create("rsync://whatever.ripe.net/myRoa.roa")
  val mappableUri: URI = URI.create("rsync://rpki.ripe.net/repository/published/38/1/myRoa.roa")
  val mappedUri: URI = URI.create("http://foo.ripe.net/bar/repository/published/38/1/myRoa.roa")


  test("Should map from rsync to http") {
    subject.mapRsynctoHttpUri(mappableUri) should be (Some(mappedUri))
  }

  test("Should return None if no mapping exists for the given uri") {
    subject.mapRsynctoHttpUri(nonMappableUri) should be (None)
  }

  test("Should delegate prefetch to http if a uri mapping exists") {
    mockHttpFetcher.prefetch(mappedUri, new ValidationResult)

    subject.prefetch(mappableUri, new ValidationResult)

    verify(mockHttpFetcher)
  }

  test("Should delegate prefetch to rsync if no mapping exists for the given uri") {
    mockRsyncFetcher.prefetch(nonMappableUri, new ValidationResult)

    subject.prefetch(nonMappableUri, new ValidationResult)

    verify(mockRsyncFetcher)
  }

  test("Should delegate manifest fetching to http if a uri mapping exists") {
    mockHttpFetcher.getManifest(mappedUri, null, null)

    subject.getManifest(mappableUri, null, null)

    verify(mockHttpFetcher)
  }

  test("Should delegate manifest fetching to rsync if no mapping exists for the given uri") {
    mockRsyncFetcher.getManifest(nonMappableUri, null, null)

    subject.getManifest(nonMappableUri, null, null)

    verify(mockRsyncFetcher)
  }

  test("Should delegate crl fetching to http if a uri mapping exists") {
    mockHttpFetcher.getCrl(mappedUri, null, null)

    subject.getCrl(mappableUri, null, null)

    verify(mockHttpFetcher)
  }

  test("Should delegate crl fetching to rsync if no mapping exists for the given uri") {
    mockRsyncFetcher.getCrl(nonMappableUri, null, null)

    subject.getCrl(nonMappableUri, null, null)

    verify(mockRsyncFetcher)
  }

  test("Should delegate object fetching to http if a uri mapping exists") {
    mockHttpFetcher.getObject(mappedUri, null, null, null)

    subject.getObject(mappableUri, null, null, null)

    verify(mockHttpFetcher)
  }

  test("Should delegate object fetching to rsync if no mapping exists for the given uri") {
    mockRsyncFetcher.getObject(nonMappableUri, null, null, null)

    subject.getObject(nonMappableUri, null, null, null)

    verify(mockRsyncFetcher)
  }
}
