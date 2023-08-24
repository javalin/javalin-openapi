package io.javalin.openapi.experimental

const val OPENAPI_INFO_TITLE = "openapi.info.title"
const val OPENAPI_INFO_VERSION = "openapi.info.version"

data class OpenApiAnnotationProcessorParameters(
    val info: Info
) {

    data class Info(
        val title: String,
        val version: String,
    )

}