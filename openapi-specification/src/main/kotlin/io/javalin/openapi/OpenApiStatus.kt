package io.javalin.openapi

object OpenApiStatus {

    const val CONTINUE = "100"
    const val SWITCHING_PROTOCOLS = "101"
    const val PROCESSING = "102"
    const val EARLY_HINTS = "103"
    const val OK = "200"
    const val CREATED = "201"
    const val ACCEPTED = "202"
    const val NON_AUTHORITATIVE_INFORMATION = "203"
    const val NO_CONTENT = "204"
    const val RESET_CONTENT = "205"
    const val PARTIAL_CONTENT = "206"
    const val MULTI_STATUS = "207"
    const val ALREADY_REPORTED = "208"
    const val IM_USED = "226"
    const val MULTIPLE_CHOICES = "300"
    const val MOVED_PERMANENTLY = "301"
    const val FOUND = "302"
    const val SEE_OTHER = "303"
    const val NOT_MODIFIED = "304"
    const val USE_PROXY = "305"
    const val TEMPORARY_REDIRECT = "307"
    const val PERMANENT_REDIRECT = "308"
    const val BAD_REQUEST = "400"
    const val UNAUTHORIZED = "401"
    const val PAYMENT_REQUIRED = "402"
    const val FORBIDDEN = "403"
    const val NOT_FOUND = "404"
    const val METHOD_NOT_ALLOWED = "405"
    const val NOT_ACCEPTABLE = "406"
    const val PROXY_AUTHENTICATION_REQUIRED = "407"
    const val REQUEST_TIMEOUT = "408"
    const val CONFLICT = "409"
    const val GONE = "410"
    const val LENGTH_REQUIRED = "411"
    const val PRECONDITION_FAILED = "412"
    const val CONTENT_TOO_LARGE = "413"
    const val URI_TOO_LONG = "414"
    const val UNSUPPORTED_MEDIA_TYPE = "415"
    const val RANGE_NOT_SATISFIABLE = "416"
    const val EXPECTATION_FAILED = "417"
    const val IM_A_TEAPOT = "418"
    const val ENHANCE_YOUR_CALM = "420"
    const val MISDIRECTED_REQUEST = "421"
    const val UNPROCESSABLE_CONTENT = "422"
    const val LOCKED = "423"
    const val FAILED_DEPENDENCY = "424"
    const val TOO_EARLY = "425"
    const val UPGRADE_REQUIRED = "426"
    const val PRECONDITION_REQUIRED = "428"
    const val TOO_MANY_REQUESTS = "429"
    const val REQUEST_HEADER_FIELDS_TOO_LARGE = "431"
    const val UNAVAILABLE_FOR_LEGAL_REASONS = "451"
    const val CLIENT_CLOSED_REQUEST = "499"
    const val INTERNAL_SERVER_ERROR = "500"
    const val NOT_IMPLEMENTED = "501"
    const val BAD_GATEWAY = "502"
    const val SERVICE_UNAVAILABLE = "503"
    const val GATEWAY_TIMEOUT = "504"
    const val HTTP_VERSION_NOT_SUPPORTED = "505"
    const val INSUFFICIENT_STORAGE = "507"
    const val LOOP_DETECTED = "508"
    const val NETWORK_AUTHENTICATION_REQUIRED = "511"

    fun reasonPhrase(status: String): String? =
        status.toIntOrNull()?.let { REASON_PHRASES[it] ?: "Unknown HTTP code" }

    private val REASON_PHRASES: Map<Int, String> = mapOf(
        CONTINUE.toInt() to "Continue",
        SWITCHING_PROTOCOLS.toInt() to "Switching Protocols",
        PROCESSING.toInt() to "Processing",
        EARLY_HINTS.toInt() to "Early Hints",
        OK.toInt() to "OK",
        CREATED.toInt() to "Created",
        ACCEPTED.toInt() to "Accepted",
        NON_AUTHORITATIVE_INFORMATION.toInt() to "Non Authoritative Information",
        NO_CONTENT.toInt() to "No Content",
        RESET_CONTENT.toInt() to "Reset Content",
        PARTIAL_CONTENT.toInt() to "Partial Content",
        MULTI_STATUS.toInt() to "Multi-Status",
        ALREADY_REPORTED.toInt() to "Already Reported",
        IM_USED.toInt() to "IM Used",
        MULTIPLE_CHOICES.toInt() to "Multiple Choices",
        MOVED_PERMANENTLY.toInt() to "Moved Permanently",
        FOUND.toInt() to "Found",
        SEE_OTHER.toInt() to "See Other",
        NOT_MODIFIED.toInt() to "Not Modified",
        USE_PROXY.toInt() to "Use Proxy",
        TEMPORARY_REDIRECT.toInt() to "Temporary Redirect",
        PERMANENT_REDIRECT.toInt() to "Permanent Redirect",
        BAD_REQUEST.toInt() to "Bad Request",
        UNAUTHORIZED.toInt() to "Unauthorized",
        PAYMENT_REQUIRED.toInt() to "Payment Required",
        FORBIDDEN.toInt() to "Forbidden",
        NOT_FOUND.toInt() to "Not Found",
        METHOD_NOT_ALLOWED.toInt() to "Method Not Allowed",
        NOT_ACCEPTABLE.toInt() to "Not Acceptable",
        PROXY_AUTHENTICATION_REQUIRED.toInt() to "Proxy Authentication Required",
        REQUEST_TIMEOUT.toInt() to "Request Timeout",
        CONFLICT.toInt() to "Conflict",
        GONE.toInt() to "Gone",
        LENGTH_REQUIRED.toInt() to "Length Required",
        PRECONDITION_FAILED.toInt() to "Precondition Failed",
        CONTENT_TOO_LARGE.toInt() to "Content Too Large",
        URI_TOO_LONG.toInt() to "URI Too Long",
        UNSUPPORTED_MEDIA_TYPE.toInt() to "Unsupported Media Type",
        RANGE_NOT_SATISFIABLE.toInt() to "Range Not Satisfiable",
        EXPECTATION_FAILED.toInt() to "Expectation Failed",
        IM_A_TEAPOT.toInt() to "I'm a Teapot",
        ENHANCE_YOUR_CALM.toInt() to "Enhance your Calm",
        MISDIRECTED_REQUEST.toInt() to "Misdirected Request",
        UNPROCESSABLE_CONTENT.toInt() to "Unprocessable Content",
        LOCKED.toInt() to "Locked",
        FAILED_DEPENDENCY.toInt() to "Failed Dependency",
        TOO_EARLY.toInt() to "Too Early",
        UPGRADE_REQUIRED.toInt() to "Upgrade Required",
        PRECONDITION_REQUIRED.toInt() to "Precondition Required",
        TOO_MANY_REQUESTS.toInt() to "Too Many Requests",
        REQUEST_HEADER_FIELDS_TOO_LARGE.toInt() to "Request Header Fields Too Large",
        UNAVAILABLE_FOR_LEGAL_REASONS.toInt() to "Unavailable for Legal Reason",
        CLIENT_CLOSED_REQUEST.toInt() to "Client Closed Request",
        INTERNAL_SERVER_ERROR.toInt() to "Server Error",
        NOT_IMPLEMENTED.toInt() to "Not Implemented",
        BAD_GATEWAY.toInt() to "Bad Gateway",
        SERVICE_UNAVAILABLE.toInt() to "Service Unavailable",
        GATEWAY_TIMEOUT.toInt() to "Gateway Timeout",
        HTTP_VERSION_NOT_SUPPORTED.toInt() to "HTTP Version Not Supported",
        INSUFFICIENT_STORAGE.toInt() to "Insufficient Storage",
        LOOP_DETECTED.toInt() to "Loop Detected",
        NETWORK_AUTHENTICATION_REQUIRED.toInt() to "Network Authentication Required",
    )

}
