import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApi
import org.junit.jupiter.api.Test

class OpenApiGeneratorTest {

    @OpenApi(
        path = "/",
        methods = [HttpMethod.GET]
    )
    class MainApi

    private val openApiDocumentation: String = OpenApiGeneratorTest::class.java.getResourceAsStream("/openapi-plugin/openapi.json")!!.readAllBytes().decodeToString()

    @Test
    fun `should generate info`() {
        println(openApiDocumentation)
    }

}