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
    private val swaggerVersion: String
) : Handler {

    override fun handle(context: Context) {
        context
            .html(createSwaggerUiHtml())
            .res
            .characterEncoding = "UTF-8"
    }

    private fun createSwaggerUiHtml(): String {
        val publicBasePath = "/webjars/swagger-ui/$swaggerVersion"

        @Language("html")
        val html = """
        <!-- HTML for static distribution bundle build -->
        <!DOCTYPE html>
        <html lang="en">
            <head>
                <meta charset="UTF-8">
                <title>$title</title>
                <link rel="stylesheet" type="text/css" href="$publicBasePath/swagger-ui.css" >
                <link rel="icon" type="image/png" href="$publicBasePath/favicon-32x32.png" sizes="32x32" />
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
                <script src="$publicBasePath/swagger-ui-bundle.js"> </script>
                <script src="$publicBasePath/swagger-ui-standalone-preset.js"> </script>
                <script>
                window.onload = function() {
                    window.ui = SwaggerUIBundle({
                        url: "$documentationPath",
                        dom_id: "#swagger-ui",
                        deepLinking: true,
                        presets: [
                          SwaggerUIBundle.presets.apis,
                          SwaggerUIStandalonePreset
                        ],
                        plugins: [
                          SwaggerUIBundle.plugins.DownloadUrl
                        ],
                        layout: "StandaloneLayout"
                      })
                }
                </script>
            </body>
        </html>
    """.trimIndent()

        return html
    }

}