package utils

/**
 * Base exception class for all application exceptions.
 */
class ApplicationException(val title: String, message: String) extends RuntimeException(message)

case class ResourceNotFoundException(message: String) extends ApplicationException("Resource Not Found", message)
case class UnprocessableEntityException(message: String) extends ApplicationException("Unable to Process Request", message)
case class AuthorizationFailedException(message: String) extends ApplicationException("Unable to Process Request", message)
case class ExternalServiceException(message: String) extends ApplicationException("Internal Service Error", message)