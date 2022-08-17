package io.javalin.openapi.plugin

/** https://github.com/OAI/OpenAPI-Specification/blob/3.1.0/versions/3.1.0.md#infoObject */
class OpenApiInfo {
    /** REQUIRED. The title of the API */
    var title = "OpenApi Title"
    /** A short summary of the API */
    var summary: String? = null
    /** A description of the API. CommonMark's syntax MAY be used for rich text representation */
    var description: String? = null
    /** A URL to the Terms of Service for the API. This MUST be in the form of a URL */
    var termsOfService: String? = null
    /** The contact information for the exposed API */
    var contact: OpenApiContact? = null
    /** The license information for the exposed API */
    var license: OpenApiLicense? = null
    /** REQUIRED. The version of the OpenAPI document (which is distinct from the OpenAPI Specification version or the API implementation version). */
    var version = ""
}

/** https://github.com/OAI/OpenAPI-Specification/blob/3.1.0/versions/3.1.0.md#contactObject */
class OpenApiContact {
    /** The identifying name of the contact person/organization. */
    var name: String? = null
    /** The URL pointing to the contact information. This MUST be in the form of a URL. */
    var url: String? = null
    /** The email address of the contact person/organization. This MUST be in the form of an email address. */
    var email: String? = null
}

/** https://github.com/OAI/OpenAPI-Specification/blob/3.1.0/versions/3.1.0.md#licenseObject */
class OpenApiLicense {
    /** REQUIRED. The license name used for the API */
    var name = ""
    /** An SPDX license expression for the API. The identifier field is mutually exclusive of the url field. */
    var identifier: String? = null
    /** A URL to the license used for the API. This MUST be in the form of a URL. The url field is mutually exclusive of the identifier field */
    var url: String? = null
}