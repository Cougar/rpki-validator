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
package net.ripe.rpki.validator
package controllers

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import support.ControllerTestCase
import models.RtrPrefix
import net.ripe.ipresource.{ IpRange, Asn }

@RunWith(classOf[JUnitRunner])
class ExportControllerTest extends ControllerTestCase {

  val PREFIX1 = RtrPrefix(asn = Asn.parse("AS6500"), prefix = IpRange.parse("10/8"), maxPrefixLength = None)
  val PREFIX2 = RtrPrefix(asn = Asn.parse("AS6500"), prefix = IpRange.parse("192.168/16"), maxPrefixLength = Some(24))
  val TEST_PREFIXES = Set[RtrPrefix](PREFIX1, PREFIX2)
  
  override def controller = new ControllerFilter with ExportController {
    override def getRtrPrefixes: Set[RtrPrefix] = {
      TEST_PREFIXES
    }
  }
  
  test("Should make CSV with max lengths filled out") {
    get("/export.csv") {

      val expectedResponse =
        """ASN,IP Prefix,Max Length
          |AS6500,10.0.0.0/8,8
          |AS6500,192.168.0.0/16,24
          |""".stripMargin

      status should equal(200)
      body should equal(expectedResponse)
      header("Content-Type") should equal("text/csv;charset=UTF-8")
      header("Pragma") should equal("public")
      header("Cache-Control") should equal("no-cache")
    }
  }

  test("Should export JSON with max lengths filled out") {
    get("/export.json") {
      status should equal(200)
      body should equal( """{"roas":[{"asn":"AS6500","prefix":"10.0.0.0/8","maxLength":8},{"asn":"AS6500","prefix":"192.168.0.0/16","maxLength":24}]}""")
      header("Content-Type") should equal("text/json;charset=UTF-8")
      header("Pragma") should equal("public")
      header("Cache-Control") should equal("no-cache")
    }
  }

}