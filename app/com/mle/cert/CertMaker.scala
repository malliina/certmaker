package com.mle.cert

import java.nio.file.{Files, Path}

import com.mle.cmd.{CommandResponse, Shell}
import com.mle.file.{FileUtilities, StorageFile}

import scala.compat.Platform

/**
 * @author Michael
 */
object CertMaker {
  val certOutDir = FileUtilities.userDir / "certout"
  val confFileName = "cert.cnf"
  val confFile = path(confFileName)

  val caCertFileName = "ca-cert.pem"
  val caCert = path("ca-cert.pem")
  val caKeyFileName = "ca-key.pem"
  val caKey = path(caKeyFileName)

  val caKeyCmd = "openssl genrsa 2048"
  val caCertCmd = s"openssl req -new -x509 -nodes -days 3600 -key $caKeyFileName -out $caCertFileName -config $confFileName"

  val server = CertFiles("server")
  val client = CertFiles("client")

  case class CertFiles(name: String) {
    def appendName(suffix: String, sep: String = "-") = s"$name$sep$suffix"

    val certFileName = appendName("cert.pem")
    val cert = path(certFileName)
    val keyFileName = appendName("key.pem")
    val requestFileName = appendName("req.pem")
    val unifiedFileName = appendName("unified.pem")
    val unified = path(unifiedFileName)
    val pkcsFileName = appendName("keystore.p12")
    val pkcsFile = path(pkcsFileName)
    val jksFileName = appendName("keystore.jks")
    val jksFile = path(jksFileName)

    val meta = Seq(
      "Unified Certificate" -> unifiedFileName,
      "Certificate" -> certFileName,
      "Key" -> keyFileName,
      "Certificate Request" -> requestFileName,
      "PKCS12 Keystore" -> pkcsFileName,
      "JKS Keystore" -> jksFileName
    ).map(pair => (appendName(pair._1, " "), pair._2)) map metaify

    def certCmd(confFileName: String) = requestCommand(keyFileName, requestFileName, confFileName)

    def remPassCmd = removePassPhraseCommand(keyFileName)

    def signCmd = signCommand(requestFileName, caCertFileName, caKeyFileName, certFileName)
  }

  val generalMeta: Seq[Meta] = Seq(
    "OpenSSL Conf" -> confFileName,
    "CA Certificate" -> caCertFileName,
    "CA Key" -> caKeyFileName).map(metaify)

  val meta: Seq[Meta] = generalMeta ++ server.meta ++ client.meta

  def metaify(pair: (String, String)): Meta = Meta(pair._1, path(pair._2).toAbsolutePath)

  val order = Seq(caKeyCmd, caCertCmd,
    server.certCmd(confFileName), server.remPassCmd, server.signCmd,
    client.certCmd(confFileName), client.remPassCmd, client.signCmd)

  private def requestCommand(key: String, request: String, config: String) =
    s"openssl req -newkey rsa:2048 -days 3600 -nodes -keyout $key -out $request -config $config"

  private def removePassPhraseCommand(key: String) = s"openssl rsa -in $key -out $key"

  private def signCommand(request: String, caCert: String, caKey: String, certOut: String) =
    s"openssl x509 -req -in $request -days 3600 -CA $caCert -CAkey $caKey -set_serial 01 -out $certOut"

  def pkcsCmd(cert: String, key: String, out: String, storePass: String) =
    s"openssl pkcs12 -export -out $out -inkey $key -in $cert -password pass:$storePass"

  def jksCmd(srcStore: String, destStore: String, storePass: String, destStoreType: String = "JKS", srcStoreType: String = "PKCS12") =
    s"keytool -importkeystore -srckeystore $srcStore -srcstorepass $storePass -srcstoretype $srcStoreType -destkeystore $destStore -deststorepass $storePass -deststoretype $destStoreType -noprompt"

  def run(info: CertRequestInfo): CommandResponse = {
    val shell = new Shell(certOutDir)
    writeConf(info, confFile)
    shell.redirect(caKey)(caKeyCmd) and
      shell.sequence(order.tail) and
      createKeyStores(shell, info, client) and
      createKeyStores(shell, info, server)
  }

  def createKeyStores(shell: Shell, info: CertRequestInfo, files: CertFiles): CommandResponse = {
    merge(files.cert, caCert, files.unified)
    val storePassword = info.storePassword
    Files.deleteIfExists(files.pkcsFile)
    Files.deleteIfExists(files.jksFile)
    val keyStoreCommands = Seq(
      pkcsCmd(files.unifiedFileName, files.keyFileName, files.pkcsFileName, storePassword),
      jksCmd(files.pkcsFileName, files.jksFileName, storePassword))
    shell.sequence(keyStoreCommands, logCommand = false)
  }

  def merge(first: Path, second: Path, dest: Path) = {
    import com.mle.file.FileUtilities.{fileToString, writerTo}
    val firstContents = fileToString(first)
    val secondContents = fileToString(second)
    writerTo(dest)(writer => {
      writer print firstContents
      writer print Platform.EOL
      writer print secondContents
    })
  }

  /**
   *
   * @param info blueprints
   * @return an OpenSSL configuration file as a string
   */
  def confOpenSSL(info: CertRequestInfo): String =
    s"""
      |[ req ]
      |default_bits       = 2048
      |default_keyfile    = privkey.pem
      |distinguished_name	= req_distinguished_name
      |attributes         = req_attributes
      |prompt             = no
      |
      |[ req_distinguished_name ]
      |C				          = ${info.countryCode}
      |ST				          = ${info.state}
      |L				          = ${info.locality}
      |O				          = ${info.organization}
      |CN				          = ${info.commonName}
      |
      |[ req_attributes ]
    """.stripMargin

  def writeConf(info: CertRequestInfo, file: Path) = FileUtilities.writerTo(file)(_.print(confOpenSSL(info)))

  def path(name: String) = certOutDir / name
}

