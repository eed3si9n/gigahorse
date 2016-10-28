/**
 * This code is generated using sbt-datatype.
 */

// DO NOT EDIT MANUALLY
package gigahorse
sealed abstract class AuthScheme extends Serializable
object AuthScheme {
  
  
  case object Basic extends AuthScheme
  case object Digest extends AuthScheme
  case object NTLM extends AuthScheme
  case object SPNEGO extends AuthScheme
  case object Kerberos extends AuthScheme
}
