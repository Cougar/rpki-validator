/**
 * The BSD License
 *
 * Copyright (c) 2010, 2011 RIPE NCC
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
package bgp.preview

import scalaz.concurrent.Promise

import collection.JavaConversions._

import net.ripe.commons.certification.validation.roa.RouteOriginValidationPolicy
import net.ripe.commons.certification.validation.roa.RouteValidityState

import models.RtrPrefix
import net.ripe.ipresource.Asn
import net.ripe.ipresource.IpRange
import net.ripe.commons.certification.validation.roa.AnnouncedRoute

object BgpAnnouncementValidator {

  val VISIBILITY_THRESHOLD = 5

  val validationPolicy: RouteOriginValidationPolicy = new RouteOriginValidationPolicy()

  val announcedRoutes: Promise[Set[AnnouncedRoute]] = Promise {
    val bgpEntries =
      RisWhoisParser.parseFile(new java.net.URL("http://www.ris.ripe.net/dumps/riswhoisdump.IPv4.gz")) ++
        RisWhoisParser.parseFile(new java.net.URL("http://www.ris.ripe.net/dumps/riswhoisdump.IPv6.gz"))

    bgpEntries
      .filter(_.visibility >= VISIBILITY_THRESHOLD)
      .map(entry => new AnnouncedRoute(entry.origin, entry.prefix))
      .toSet
  }

//  var rtrPrefixes: List[net.ripe.commons.certification.validation.roa.RtrPrefix] = _

  var validatedAnnouncements: Promise[Set[ValidatedAnnouncement]] = Promise{Set.empty[ValidatedAnnouncement]}

  def updateRtrPrefixes(newRtrPrefixes: Set[RtrPrefix]) = {
    validatedAnnouncements = Promise {
      val rtrPrefixes = scala.collection.JavaConversions
                  .seqAsJavaList(convertRtrPrefixesToJava(newRtrPrefixes))

      val announcedRoutesSet: Set[AnnouncedRoute] = announcedRoutes.get
      for {
          route <- announcedRoutesSet
        } yield {
          val routeValidityJava = validationPolicy.determineRouteValidityState(rtrPrefixes, route)

          var validity: AnnouncementValidity = null

          if (routeValidityJava == RouteValidityState.VALID) {
            validity = ValidAnnouncement
          }
          if (routeValidityJava == RouteValidityState.INVALID) {
              validity = InvalidAnnouncement
          }
          if (routeValidityJava == RouteValidityState.UNKNOWN) {
              validity = UnknownAnnouncement
          }

          new ValidatedAnnouncement(asn = route.getOriginAsn, prefix = route.getPrefix(), validity = validity)
        }
    }
  }

  def convertRtrPrefixesToJava(rtrPrefixes: Set[RtrPrefix]) = {
    (for { prefix <- rtrPrefixes } yield {
      new net.ripe.commons.certification.validation.roa.RtrPrefix(prefix.prefix, prefix.maxPrefixLength.getOrElse(prefix.prefix.getPrefixLength()), prefix.asn)
    }).toList
  }

}

trait AnnouncementValidity

case object ValidAnnouncement extends AnnouncementValidity
case object InvalidAnnouncement extends AnnouncementValidity
case object UnknownAnnouncement extends AnnouncementValidity

case class ValidatedAnnouncement(asn: Asn, prefix: IpRange, validity: AnnouncementValidity)