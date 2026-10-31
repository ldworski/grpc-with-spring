package pl.dworski.grpcwithspring

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles('test')
@Import(MongoTestContainerConfiguration)
@AutoConfigureTestGrpcTransport
class ApplicationStartupSpec extends Specification {

    @Autowired
    ConfigurableApplicationContext context

    def "context loads"() {
        expect:
        context.isRunning()
    }

}
