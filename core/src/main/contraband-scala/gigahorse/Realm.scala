/**
 * This code is generated using [[https://www.scala-sbt.org/contraband/ sbt-contraband]].
 */

// DO NOT EDIT MANUALLY
package gigahorse
/**
 * @param username The user name.
 * @param password The password.
 * @param scheme The scheme for this authentication.
 * @param usePreemptiveAuth Whether preemptive authentication is enabled. (Default: true)
 */
final class Realm private (
  val username: String,
  val password: String,
  val scheme: AuthScheme,
  val usePreemptiveAuth: Boolean,
  val realmNameOpt: Option[String],
  val nonceOpt: Option[String],
  val algorithmOpt: Option[String],
  val responseOpt: Option[String],
  val opaqueOpt: Option[String],
  val qopOpt: Option[String],
  val ncOpt: Option[String],
  val uriOpt: Option[java.net.URI],
  val methodNameOpt: Option[String],
  val charsetOpt: Option[java.nio.charset.Charset],
  val ntlmDomainOpt: Option[String],
  val ntlmHostOpt: Option[String],
  val useAbsoluteURI: Boolean,
  val omitQuery: Boolean) extends Serializable {
  def withRealmName(realmName: String): Realm = copy(realmNameOpt = Some(realmName))
  def withNonce(nonce: String): Realm = copy(nonceOpt = Some(nonce))
  def withAlgorithm(algorithm: String): Realm = copy(algorithmOpt = Some(algorithm))
  def withResponse(response: String): Realm = copy(responseOpt = Some(response))
  def withOpaque(opaque: String): Realm = copy(opaqueOpt = Some(opaque))
  def withQop(qop: String): Realm = copy(qopOpt = Some(qop))
  def withNc(nc: String): Realm = copy(ncOpt = Some(nc))
  def withUri(uri: java.net.URI): Realm = copy(uriOpt = Some(uri))
  def withMethodName(methodName: String): Realm = copy(methodNameOpt = Some(methodName))
  def withCharset(charset: java.nio.charset.Charset): Realm = copy(charsetOpt = Some(charset))
  def withNtlmDomain(ntlmDomain: String): Realm = copy(ntlmDomainOpt = Some(ntlmDomain))
  def withNtlmHost(ntlmHost: String): Realm = copy(ntlmHostOpt = Some(ntlmHost))
  private def this(username: String, password: String) = this(username, password, AuthScheme.Basic, true, None, None, None, None, None, None, None, None, None, None, None, None, false, false)
  private def this(username: String, password: String, scheme: AuthScheme) = this(username, password, scheme, true, None, None, None, None, None, None, None, None, None, None, None, None, false, false)
  
  override def equals(o: Any): Boolean = o match {
    case x: Realm => (this.username == x.username) && (this.password == x.password) && (this.scheme == x.scheme) && (this.usePreemptiveAuth == x.usePreemptiveAuth) && (this.realmNameOpt == x.realmNameOpt) && (this.nonceOpt == x.nonceOpt) && (this.algorithmOpt == x.algorithmOpt) && (this.responseOpt == x.responseOpt) && (this.opaqueOpt == x.opaqueOpt) && (this.qopOpt == x.qopOpt) && (this.ncOpt == x.ncOpt) && (this.uriOpt == x.uriOpt) && (this.methodNameOpt == x.methodNameOpt) && (this.charsetOpt == x.charsetOpt) && (this.ntlmDomainOpt == x.ntlmDomainOpt) && (this.ntlmHostOpt == x.ntlmHostOpt) && (this.useAbsoluteURI == x.useAbsoluteURI) && (this.omitQuery == x.omitQuery)
    case _ => false
  }
  override def hashCode: Int = {
    37 * (37 * (37 * (37 * (37 * (37 * (37 * (37 * (37 * (37 * (37 * (37 * (37 * (37 * (37 * (37 * (37 * (37 * (37 * (17 + "gigahorse.Realm".##) + username.##) + password.##) + scheme.##) + usePreemptiveAuth.##) + realmNameOpt.##) + nonceOpt.##) + algorithmOpt.##) + responseOpt.##) + opaqueOpt.##) + qopOpt.##) + ncOpt.##) + uriOpt.##) + methodNameOpt.##) + charsetOpt.##) + ntlmDomainOpt.##) + ntlmHostOpt.##) + useAbsoluteURI.##) + omitQuery.##)
  }
  override def toString: String = {
    "Realm(" + username + ", " + password + ", " + scheme + ", " + usePreemptiveAuth + ", " + realmNameOpt + ", " + nonceOpt + ", " + algorithmOpt + ", " + responseOpt + ", " + opaqueOpt + ", " + qopOpt + ", " + ncOpt + ", " + uriOpt + ", " + methodNameOpt + ", " + charsetOpt + ", " + ntlmDomainOpt + ", " + ntlmHostOpt + ", " + useAbsoluteURI + ", " + omitQuery + ")"
  }
  private[this] def copy(username: String = username, password: String = password, scheme: AuthScheme = scheme, usePreemptiveAuth: Boolean = usePreemptiveAuth, realmNameOpt: Option[String] = realmNameOpt, nonceOpt: Option[String] = nonceOpt, algorithmOpt: Option[String] = algorithmOpt, responseOpt: Option[String] = responseOpt, opaqueOpt: Option[String] = opaqueOpt, qopOpt: Option[String] = qopOpt, ncOpt: Option[String] = ncOpt, uriOpt: Option[java.net.URI] = uriOpt, methodNameOpt: Option[String] = methodNameOpt, charsetOpt: Option[java.nio.charset.Charset] = charsetOpt, ntlmDomainOpt: Option[String] = ntlmDomainOpt, ntlmHostOpt: Option[String] = ntlmHostOpt, useAbsoluteURI: Boolean = useAbsoluteURI, omitQuery: Boolean = omitQuery): Realm = {
    new Realm(username, password, scheme, usePreemptiveAuth, realmNameOpt, nonceOpt, algorithmOpt, responseOpt, opaqueOpt, qopOpt, ncOpt, uriOpt, methodNameOpt, charsetOpt, ntlmDomainOpt, ntlmHostOpt, useAbsoluteURI, omitQuery)
  }
  def withUsername(username: String): Realm = {
    copy(username = username)
  }
  def withPassword(password: String): Realm = {
    copy(password = password)
  }
  def withScheme(scheme: AuthScheme): Realm = {
    copy(scheme = scheme)
  }
  def withUsePreemptiveAuth(usePreemptiveAuth: Boolean): Realm = {
    copy(usePreemptiveAuth = usePreemptiveAuth)
  }
  def withRealmNameOpt(realmNameOpt: Option[String]): Realm = {
    copy(realmNameOpt = realmNameOpt)
  }
  def withRealmNameOpt(realmNameOpt: String): Realm = {
    copy(realmNameOpt = Option(realmNameOpt))
  }
  def withNonceOpt(nonceOpt: Option[String]): Realm = {
    copy(nonceOpt = nonceOpt)
  }
  def withNonceOpt(nonceOpt: String): Realm = {
    copy(nonceOpt = Option(nonceOpt))
  }
  def withAlgorithmOpt(algorithmOpt: Option[String]): Realm = {
    copy(algorithmOpt = algorithmOpt)
  }
  def withAlgorithmOpt(algorithmOpt: String): Realm = {
    copy(algorithmOpt = Option(algorithmOpt))
  }
  def withResponseOpt(responseOpt: Option[String]): Realm = {
    copy(responseOpt = responseOpt)
  }
  def withResponseOpt(responseOpt: String): Realm = {
    copy(responseOpt = Option(responseOpt))
  }
  def withOpaqueOpt(opaqueOpt: Option[String]): Realm = {
    copy(opaqueOpt = opaqueOpt)
  }
  def withOpaqueOpt(opaqueOpt: String): Realm = {
    copy(opaqueOpt = Option(opaqueOpt))
  }
  def withQopOpt(qopOpt: Option[String]): Realm = {
    copy(qopOpt = qopOpt)
  }
  def withQopOpt(qopOpt: String): Realm = {
    copy(qopOpt = Option(qopOpt))
  }
  def withNcOpt(ncOpt: Option[String]): Realm = {
    copy(ncOpt = ncOpt)
  }
  def withNcOpt(ncOpt: String): Realm = {
    copy(ncOpt = Option(ncOpt))
  }
  def withUriOpt(uriOpt: Option[java.net.URI]): Realm = {
    copy(uriOpt = uriOpt)
  }
  def withUriOpt(uriOpt: java.net.URI): Realm = {
    copy(uriOpt = Option(uriOpt))
  }
  def withMethodNameOpt(methodNameOpt: Option[String]): Realm = {
    copy(methodNameOpt = methodNameOpt)
  }
  def withMethodNameOpt(methodNameOpt: String): Realm = {
    copy(methodNameOpt = Option(methodNameOpt))
  }
  def withCharsetOpt(charsetOpt: Option[java.nio.charset.Charset]): Realm = {
    copy(charsetOpt = charsetOpt)
  }
  def withCharsetOpt(charsetOpt: java.nio.charset.Charset): Realm = {
    copy(charsetOpt = Option(charsetOpt))
  }
  def withNtlmDomainOpt(ntlmDomainOpt: Option[String]): Realm = {
    copy(ntlmDomainOpt = ntlmDomainOpt)
  }
  def withNtlmDomainOpt(ntlmDomainOpt: String): Realm = {
    copy(ntlmDomainOpt = Option(ntlmDomainOpt))
  }
  def withNtlmHostOpt(ntlmHostOpt: Option[String]): Realm = {
    copy(ntlmHostOpt = ntlmHostOpt)
  }
  def withNtlmHostOpt(ntlmHostOpt: String): Realm = {
    copy(ntlmHostOpt = Option(ntlmHostOpt))
  }
  def withUseAbsoluteURI(useAbsoluteURI: Boolean): Realm = {
    copy(useAbsoluteURI = useAbsoluteURI)
  }
  def withOmitQuery(omitQuery: Boolean): Realm = {
    copy(omitQuery = omitQuery)
  }
}
object Realm {
  
  def apply(username: String, password: String): Realm = new Realm(username, password)
  def apply(username: String, password: String, scheme: AuthScheme): Realm = new Realm(username, password, scheme)
  def apply(username: String, password: String, scheme: AuthScheme, usePreemptiveAuth: Boolean, realmNameOpt: Option[String], nonceOpt: Option[String], algorithmOpt: Option[String], responseOpt: Option[String], opaqueOpt: Option[String], qopOpt: Option[String], ncOpt: Option[String], uriOpt: Option[java.net.URI], methodNameOpt: Option[String], charsetOpt: Option[java.nio.charset.Charset], ntlmDomainOpt: Option[String], ntlmHostOpt: Option[String], useAbsoluteURI: Boolean, omitQuery: Boolean): Realm = new Realm(username, password, scheme, usePreemptiveAuth, realmNameOpt, nonceOpt, algorithmOpt, responseOpt, opaqueOpt, qopOpt, ncOpt, uriOpt, methodNameOpt, charsetOpt, ntlmDomainOpt, ntlmHostOpt, useAbsoluteURI, omitQuery)
  def apply(username: String, password: String, scheme: AuthScheme, usePreemptiveAuth: Boolean, realmNameOpt: String, nonceOpt: String, algorithmOpt: String, responseOpt: String, opaqueOpt: String, qopOpt: String, ncOpt: String, uriOpt: java.net.URI, methodNameOpt: String, charsetOpt: java.nio.charset.Charset, ntlmDomainOpt: String, ntlmHostOpt: String, useAbsoluteURI: Boolean, omitQuery: Boolean): Realm = new Realm(username, password, scheme, usePreemptiveAuth, Option(realmNameOpt), Option(nonceOpt), Option(algorithmOpt), Option(responseOpt), Option(opaqueOpt), Option(qopOpt), Option(ncOpt), Option(uriOpt), Option(methodNameOpt), Option(charsetOpt), Option(ntlmDomainOpt), Option(ntlmHostOpt), useAbsoluteURI, omitQuery)
}
