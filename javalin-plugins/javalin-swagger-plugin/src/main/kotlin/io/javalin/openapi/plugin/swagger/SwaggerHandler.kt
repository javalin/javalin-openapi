package io.javalin.openapi.plugin.swagger

import io.javalin.http.Context
import io.javalin.http.Handler
import org.intellij.lang.annotations.Language

/**
 * Based on https://github.com/tipsy/javalin/blob/master/javalin-openapi/src/main/java/io/javalin/plugin/openapi/ui/SwaggerRenderer.kt by @chsfleury
 */
class SwaggerHandler(
    private val title: String,
    private val documentationPath: String,
    private val versions: Set<String>,
    private val swaggerVersion: String,
    private val validatorUrl: String?,
    private val routingPath: String,
    private val basePath: String?,
    private val tagsSorter: String,
    private val operationsSorter: String,
    private val customStylesheetFiles: List<Pair<String, String>>,
    private val customJavaScriptFiles: List<Pair<String, String>>
) : Handler {

    private val swaggerUiHtml = createSwaggerUiHtml()

    override fun handle(context: Context) {
        context
            .html(swaggerUiHtml)
            .res()
            .characterEncoding = "UTF-8"
    }

    private fun createSwaggerUiHtml(): String {
        val rootPath = (basePath ?: "") + routingPath
        val publicSwaggerAssetsPath = "$rootPath/webjars/swagger-ui/$swaggerVersion".removedDoubledPathOperators()
        val publicDocumentationPath = (rootPath + documentationPath).removedDoubledPathOperators()
        val allDocumentations = versions
            .joinToString(separator = ",\n") { "{ name: '$it', url: '$publicDocumentationPath?v=$it' }" }
        val allCustomStylesheets = customStylesheetFiles
            .joinToString(separator = "\n") { "<link href='${it.first}' rel='stylesheet' media='${it.second}' type='text/css' />" }
        val allCustomJavaScripts = customJavaScriptFiles
            .joinToString(separator = "\n") { "<script src='${it.first}' type='${it.second}' />"}

        @Language("html")
        val html = """
        <!-- HTML for static distribution bundle build -->
        <!DOCTYPE html>
        <html lang="en">
            <head>
                <meta charset="UTF-8">
                <title>$title</title>
                <link rel="stylesheet" type="text/css" href="$publicSwaggerAssetsPath/swagger-ui.css" >
                <link rel="icon" type="image/png" href="$publicSwaggerAssetsPath/favicon-32x32.png" sizes="32x32" />
                $allCustomStylesheets
                <style>
                    html {
                        box-sizing: border-box;
                        overflow: -moz-scrollbars-vertical;
                        overflow-y: scroll;
                    }
                    *, *:before, *:after {
                        box-sizing: inherit;
                    }
                    body {
                        margin:0;
                        background: #fafafa;
                    }
                </style>
            </head>
            <body>
                <div id="swagger-ui"></div>
                <script src="$publicSwaggerAssetsPath/swagger-ui-bundle.js"> </script>
                <script src="$publicSwaggerAssetsPath/swagger-ui-standalone-preset.js"> </script>
                <script>
                window.onload = function() {
                    window.ui = SwaggerUIBundle({
                        urls: [
                            $allDocumentations
                        ],
                        dom_id: "#swagger-ui",
                        deepLinking: true,
                        presets: [
                          SwaggerUIBundle.presets.apis,
                          SwaggerUIStandalonePreset
                        ],
                        plugins: [
                          SwaggerUIBundle.plugins.DownloadUrl
                        ],
                        layout: "StandaloneLayout",
                        tagsSorter: $tagsSorter,
                        operationsSorter: $operationsSorter,
                        validatorUrl: ${if (validatorUrl != null) "\"$validatorUrl\"" else "null"}
                      })
                }
                </script>
                $allCustomJavaScripts
            </body>
        </html>
    """.trimIndent()

        return html
    }

    private val multiplePathOperatorsRegex = Regex("/+")

    private fun String.removedDoubledPathOperators(): String =
        replace(multiplePathOperatorsRegex, "/")

}