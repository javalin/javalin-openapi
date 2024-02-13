package io.javalin.openapi

import com.fasterxml.jackson.annotation.JsonProperty

/** https://github.com/OAI/OpenAPI-Specification/blob/3.1.0/versions/3.1.0.md#server-object */
class OpenApiServer {

    /** REQUIRED. A URL to the target host. This URL supports Server Variables and MAY be relative, to indicate that the host location is relative to the location where the OpenAPI document is being served. Variable substitutions will be made when a variable is named in {brackets}. */
    var url: String? = null
    fun url(url: String) = apply { this.url = url }

    /** An optional string describing the host designated by the URL. CommonMark syntax MAY be used for rich text representation. */
    var description: String? = null
    fun description(description: String) = apply { this.description = description }

    /** A map between a variable name and its value. The value is used for substitution in the server's URL template. */
    var variables: MutableMap<String, OpenApiServerVariable> = mutableMapOf()

    fun variable(key: String,  description: String, defaultValue: String, vararg values: String) = apply {
        addVariable(key, OpenApiServerVariable().values(*values).default(defaultValue).description(description))
    }

    fun addVariable(key: String, variable: OpenApiServerVariable): OpenApiServer = also {
        variables[key] = variable
    }

    fun addVariable(key: String, defaultValue: String, values: Array<String>, description: String): OpenApiServer = also {
        addVariable(
            key = key,
            variable = OpenApiServerVariable().also {
                it.values = values.toList()
                it.default = defaultValue
                it.description = description
            }
        )
    }

}

/** https://github.com/OAI/OpenAPI-Specification/blob/3.1.0/versions/3.1.0.md#server-variable-object */
class OpenApiServerVariable {
    /** An enumeration of string values to be used if the substitution options are from a limited set. The array MUST NOT be empty. */
    @JsonProperty("enum")
    var values: List<String>? = null
    fun values(vararg values: String) = apply { this.values = values.toList() }

    /** REQUIRED. The default value to use for substitution, which SHALL be sent if an alternate value is not supplied. Note this behavior is different than the Schema Object's treatment of default values, because in those cases parameter values are optional. If the enum is defined, the value MUST exist in the enum's values. */
    var default: String? = null
    fun default(default: String) = apply { this.default = default }

    /** An optional description for the server variable. CommonMark syntax MAY be used for rich text representation. */
    var description: String? = null
    fun description(description: String) = apply { this.description = description }
}
