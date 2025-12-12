
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.MountableFile
import java.util.*

class KratosContainer {

    private val KRATOS_IMAGE = "oryd/kratos:v1.0.0"
    private val CONFIG_DIR = "/etc/config/kratos"

    private val kratos =
        GenericContainer(KRATOS_IMAGE)
            .withExposedPorts(4433, 4434)
            .withCommand("serve -c $CONFIG_DIR/kratos.yaml --dev --watch-courier")
            .waitingFor(Wait.forHttp("/health/ready").forStatusCode(200))
            .withLogConsumer { frame ->     System.err.println("[KRATOS] ${frame.utf8String.trim()}") }
            .withCopyFileToContainer(
                MountableFile.forClasspathResource("kratos.yaml"),
                "$CONFIG_DIR/kratos.yaml"
            )
            .withCopyFileToContainer(
                MountableFile.forClasspathResource("identity.schema.user.json"),
                "$CONFIG_DIR/identities/identity.schema.user.json"
            )

    fun start() {
        kratos.start()

        val publicPort = kratos.getMappedPort(4433)
        val adminPort  = kratos.getMappedPort(4434)

        System.setProperty(
            "managementportal.identityServer.serverUrl",
            "http://localhost:$publicPort"
        )

        System.setProperty(
            "managementportal.identityServer.serverAdminUrl",
            "http://localhost:$adminPort"
        )

        println("Kratos public: http://localhost:$publicPort")
        println("Kratos admin:  http://localhost:$adminPort")
    }
}
