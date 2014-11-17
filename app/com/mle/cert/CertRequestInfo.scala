package com.mle.cert

import play.api.libs.json.Json

/**
 * @author Michael
 */
case class CertRequestInfo(countryCode: String,
                           state: String,
                           locality: String,
                           organization: String,
                           commonName: String,
                           pkcs12Password: String,
                           jksPassword: String)

object CertRequestInfo {
  implicit val json = Json.format[CertRequestInfo]
}