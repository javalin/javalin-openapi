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
        REASON_PHRASES[status] ?: status.toIntOrNull()?.let { "Unknown HTTP code" }

    private val REASON_PHRASES: Map<String, String> = mapOf(
        CONTINUE to "Continue",
        SWITCHING_PROTOCOLS to "Switching Protocols",
        PROCESSING to "Processing",
        EARLY_HINTS to "Early Hints",
        OK to "OK",
        CREATED to "Created",
        ACCEPTED to "Accepted",
        NON_AUTHORITATIVE_INFORMATION to "Non-Authoritative Information",
        NO_CONTENT to "No Content",
        RESET_CONTENT to "Reset Content",
        PARTIAL_CONTENT to "Partial Content",
        MULTI_STATUS to "Multi-Status",
        ALREADY_REPORTED to "Already Reported",
        IM_USED to "IM Used",
        MULTIPLE_CHOICES to "Multiple Choices",
        MOVED_PERMANENTLY to "Moved Permanently",
        FOUND to "Found",
        SEE_OTHER to "See Other",
        NOT_MODIFIED to "Not Modified",
        USE_PROXY to "Use Proxy",
        TEMPORARY_REDIRECT to "Temporary Redirect",
        PERMANENT_REDIRECT to "Permanent Redirect",
        BAD_REQUEST to "Bad Request",
        UNAUTHORIZED to "Unauthorized",
        PAYMENT_REQUIRED to "Payment Required",
        FORBIDDEN to "Forbidden",
        NOT_FOUND to "Not Found",
        METHOD_NOT_ALLOWED to "Method Not Allowed",
        NOT_ACCEPTABLE to "Not Acceptable",
        PROXY_AUTHENTICATION_REQUIRED to "Proxy Authentication Required",
        REQUEST_TIMEOUT to "Request Timeout",
        CONFLICT to "Conflict",
        GONE to "Gone",
        LENGTH_REQUIRED to "Length Required",
        PRECONDITION_FAILED to "Precondition Failed",
        CONTENT_TOO_LARGE to "Content Too Large",
        URI_TOO_LONG to "URI Too Long",
        UNSUPPORTED_MEDIA_TYPE to "Unsupported Media Type",
        RANGE_NOT_SATISFIABLE to "Range Not Satisfiable",
        EXPECTATION_FAILED to "Expectation Failed",
        IM_A_TEAPOT to "I'm a teapot",
        ENHANCE_YOUR_CALM to "Enhance your Calm",
        MISDIRECTED_REQUEST to "Misdirected Request",
        UNPROCESSABLE_CONTENT to "Unprocessable Content",
        LOCKED to "Locked",
        FAILED_DEPENDENCY to "Failed Dependency",
        TOO_EARLY to "Too Early",
        UPGRADE_REQUIRED to "Upgrade Required",
        PRECONDITION_REQUIRED to "Precondition Required",
        TOO_MANY_REQUESTS to "Too Many Requests",
        REQUEST_HEADER_FIELDS_TOO_LARGE to "Request Header Fields Too Large",
        UNAVAILABLE_FOR_LEGAL_REASONS to "Unavailable For Legal Reasons",
        CLIENT_CLOSED_REQUEST to "Client Closed Request",
        INTERNAL_SERVER_ERROR to "Internal Server Error",
        NOT_IMPLEMENTED to "Not Implemented",
        BAD_GATEWAY to "Bad Gateway",
        SERVICE_UNAVAILABLE to "Service Unavailable",
        GATEWAY_TIMEOUT to "Gateway Timeout",
        HTTP_VERSION_NOT_SUPPORTED to "HTTP Version Not Supported",
        INSUFFICIENT_STORAGE to "Insufficient Storage",
        LOOP_DETECTED to "Loop Detected",
        NETWORK_AUTHENTICATION_REQUIRED to "Network Authentication Required",
    )

}
