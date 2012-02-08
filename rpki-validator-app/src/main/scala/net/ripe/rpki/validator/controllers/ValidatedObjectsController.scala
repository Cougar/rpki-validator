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

import scala.collection.JavaConverters._
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import org.joda.time.DateTime
import net.ripe.rpki.validator.config.Main
import models._
import views.RoasView
import views.RoaTableData
import views.ValidationDetailsView
import views.ValidationDetailsTableData
import views.ValidatedObjectDetail
import grizzled.slf4j.Logging

trait ValidatedObjectsController extends ApplicationController with Logging {
  protected def validatedObjects: ValidatedObjects

  get("/roas") {
    new RoasView(validatedObjects)
  }

  get("/roas-data") {
    new RoaTableData(validatedObjects) {
      override def getParam(name: String) = params(name)
    }
  }

  get("/validation-details") {
    new ValidationDetailsView()
  }
  
  get("/validation-details-data") {
    new ValidationDetailsTableData(getValidationDetails) {
      override def getParam(name: String) = params(name)
    }
  }
  
  get("/validation-details.csv") {

    contentType = "text/csv"
    response.addHeader("Content-Disposition", "attachment; filename=validation-details-data.csv")
    response.addHeader("Pragma", "public")
    response.addHeader("Cache-Control", "no-cache")

    val Header = "URI, Object Validity, Check, Check Validity\n"
    val RowFormat = "\"%s\",%s,%s,%s\n"

    val writer = response.getWriter()
    writer.print(Header)

    val records = getValidationDetails

    records.foreach {
      record =>
        writer.print(RowFormat.format(
          record.uri,
          record.isValid,
          record.check.getKey,
          record.check.isOk
      ))
    }
  }

  def getValidationDetails = {
    val records = for {
      validatedObjects <- validatedObjects.all.values.par
      validatedObject <- validatedObjects
      check <- validatedObject.checks
    } yield {
      ValidatedObjectDetail(validatedObject.uri, validatedObject.isValid, check)
    }
    records.seq.toIndexedSeq
  }

}