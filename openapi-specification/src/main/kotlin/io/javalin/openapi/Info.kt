package io.javalin.openapi

import java.util.function.Consumer

/** https://github.com/OAI/OpenAPI-Specification/blob/3.1.0/versions/3.1.0.md#infoObject */
class OpenApiInfo {
    /** REQUIRED. The title of the API */
    var title: String? = null
    fun title(title: String) = apply { this.title = title }

    /** A short summary of the API */
    var summary: String? = null
    fun summary(summary: String) = apply { this.summary = summary }

    /** A description of the API. CommonMark's syntax MAY be used for rich text representation */
    var description: String? = null
    fun description(description: String) = apply { this.description = description }

    /** A URL to the Terms of Service for the API. This MUST be in the form of a URL */
    var termsOfService: String? = null
    fun termsOfService(termsOfService: String) = apply { this.termsOfService = termsOfService }

    /** The contact information for the exposed API */
    var contact: OpenApiContact? = null
    @JvmOverloads
    fun contact(name: String?, url: String? = null, email: String? = null) = withContact { it.name(name).url(url).email(email) }
    fun withContact(contact: Consumer<OpenApiContact>) = apply { this.contact = OpenApiContact().apply { contact.accept(this) } }

    /** The license information for the exposed API */
    var license: OpenApiLicense? = null
    @JvmOverloads
    fun license(name: String?, url: String? = null, identifier: String? = null) = withLicense { it.name(name).url(url).identifier(identifier) }
    fun withLicense(license: Consumer<OpenApiLicense>) = apply { this.license = OpenApiLicense().apply { license.accept(this) } }

    /** REQUIRED. The version of the OpenAPI document (which is distinct from the OpenAPI Specification version or the API implementation version). */
    var version: String? = null
    fun version(version: String) = apply { this.version = version }
}

/** https://github.com/OAI/OpenAPI-Specification/blob/3.1.0/versions/3.1.0.md#contactObject */
class OpenApiContact {
    /** The identifying name of the contact person/organization. */
    var name: String? = null
    fun name(name: String?) = apply { this.name = name }

    /** The URL pointing to the contact information. This MUST be in the form of a URL. */
    var url: String? = null
    fun url(url: String?) = apply { this.url = url }

    /** The email address of the contact person/organization. This MUST be in the form of an email address. */
    var email: String? = null
    fun email(email: String?) = apply { this.email = email }
}

/** https://github.com/OAI/OpenAPI-Specification/blob/3.1.0/versions/3.1.0.md#licenseObject */
class OpenApiLicense {
    /** REQUIRED. The license name used for the API */
    var name: String? = null
    fun name(name: String?) = apply { this.name = name }

    /** An SPDX license expression for the API. The identifier field is mutually exclusive of the url field. */
    var identifier: String? = null
    fun identifier(identifier: String?) = apply { this.identifier = identifier }

    /** A URL to the license used for the API. This MUST be in the form of a URL. The url field is mutually exclusive of the identifier field */
    var url: String? = null
    fun url(url: String?) = apply { this.url = url }
}