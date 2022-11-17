package io.javalin.openapi.plugin.redoc

import io.javalin.http.Context
import io.javalin.http.Handler

/**
 * Based on https://github.com/tipsy/javalin/blob/master/javalin-openapi/src/main/java/io/javalin/plugin/openapi/ui/ReDocRenderer.kt by @chsfleury
 */
class ReDocHandler(
    private val title: String,
    private val documentationPath: String,
    private val version: String,
    private val basePath: String
) : Handler {

    override fun handle(context: Context) {
        context
            .html(createReDocUI())
            .res().characterEncoding = "UTF-8"
    }

    private fun createReDocUI(): String {
        val publicBasePath = "$basePath/webjars/redoc/$version".replace("//", "")

        return """
        |<!DOCTYPE html>
        |<html>
        |  <head>
        |    <title>$title</title>
        |    <!-- Needed for adaptive design -->
        |    <meta charset="utf-8"/>
        |    <meta name="viewport" content="width=device-width, initial-scale=1">
        |    <link href="https://fonts.googleapis.com/css?family=Montserrat:300,400,700|Roboto:300,400,700" rel="stylesheet">
        |    <!-- ReDoc doesn't change outer page styles -->
        |    <style>body{margin:0;padding:0;}</style>
        |  </head>
        |  <body>
        |  <redoc id='redoc'></redoc>
        |  <script src="$publicBasePath/bundles/redoc.standalone.js"></script>
        |  <script>
        |   window.onload = () => {
        |     Redoc.init('$documentationPath', {}, document.getElementById('redoc'))
        |   }
        | </script>
        |  </body>
        |</html>
        |""".trimMargin()
    }

}